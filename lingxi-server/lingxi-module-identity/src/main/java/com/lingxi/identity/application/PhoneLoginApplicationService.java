package com.lingxi.identity.application;

import com.lingxi.identity.api.PhoneLoginCommand;
import com.lingxi.identity.api.PhoneLoginFacade;
import com.lingxi.identity.api.RegisterVerifiedUserCommand;
import com.lingxi.identity.api.RegisteredUserResult;
import com.lingxi.identity.api.SessionTokens;
import com.lingxi.identity.domain.IdentitySecurityRepository;
import com.lingxi.identity.domain.LoginIdentity;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 手机号登录用例。
 *
 * <p>手机号以不可逆摘要作为登录身份（channel=PHONE），数据库不保存明文手机号；首次使用该手机号登录时
 * 必须提供出生日期，服务端按 14+ 准入与青少年规则建立账号，之后登录只校验手机号与设备标识。
 *
 * <p>当前不校验短信验证码：任何知道手机号的人都能登录该账号，属于**临时的过渡实现**。生产对外开放前必须
 * 接入短信验证码或受信身份桥接层；可通过 `lingxi.identity.phone-login-enabled=false` 立即关闭该入口。
 */
@Service
public class PhoneLoginApplicationService implements PhoneLoginFacade {
  /** 登录身份渠道标识。 */
  private static final String CHANNEL = "PHONE";
  /** 中国大陆手机号。 */
  private static final Pattern CN_MOBILE = Pattern.compile("^1[3-9]\\d{9}$");

  private final IdentitySecurityRepository securityRepository;
  private final IdentityApplicationService identityService;
  private final AuthenticationApplicationService authenticationService;
  private final IdGenerator idGenerator;
  private final boolean enabled;
  private final Clock clock;

  @Autowired
  public PhoneLoginApplicationService(
      IdentitySecurityRepository securityRepository,
      IdentityApplicationService identityService,
      AuthenticationApplicationService authenticationService,
      IdGenerator idGenerator,
      @Value("${lingxi.identity.phone-login-enabled:true}") boolean enabled) {
    this(
        securityRepository,
        identityService,
        authenticationService,
        idGenerator,
        enabled,
        Clock.systemUTC());
  }

  PhoneLoginApplicationService(
      IdentitySecurityRepository securityRepository,
      IdentityApplicationService identityService,
      AuthenticationApplicationService authenticationService,
      IdGenerator idGenerator,
      boolean enabled,
      Clock clock) {
    this.securityRepository = securityRepository;
    this.identityService = identityService;
    this.authenticationService = authenticationService;
    this.idGenerator = idGenerator;
    this.enabled = enabled;
    this.clock = clock;
  }

  @Override
  @Transactional
  public SessionTokens loginWithPhone(PhoneLoginCommand command) {
    if (!enabled) {
      throw new BusinessException("AUTH_PHONE_LOGIN_DISABLED", "手机号登录入口已关闭");
    }
    if (command == null || isBlank(command.deviceId())) {
      throw new BusinessException("AUTH_INVALID_LOGIN", "登录参数不完整");
    }
    String phoneNumber = normalizePhoneNumber(command);
    String subjectHash = subjectHash(phoneNumber);
    LoginIdentity existing =
        securityRepository.findLoginIdentity(CHANNEL, subjectHash).orElse(null);
    long userId =
        existing != null
            ? existing.userId()
            : registerByPhone(phoneNumber, subjectHash, command);
    return authenticationService.issueSession(userId, command.deviceId().trim());
  }

  /** 首次使用该手机号登录：按出生日期建立账号并绑定手机号登录身份。 */
  private long registerByPhone(
      String phoneNumber, String subjectHash, PhoneLoginCommand command) {
    if (command.birthDate() == null) {
      throw new BusinessException(
          "AUTH_PHONE_REGISTRATION_REQUIRED", "首次使用手机号登录需要提供出生日期");
    }
    // 请求键使用手机号摘要，保证同一手机号并发首登不会建立两个账号。
    RegisteredUserResult user =
        identityService.registerVerifiedUser(
            new RegisterVerifiedUserCommand(
                "phone-registration:" + subjectHash,
                command.birthDate(),
                "PHONE_NUMBER",
                "phone:" + maskedSuffix(phoneNumber),
                isBlank(command.timezone()) ? "Asia/Shanghai" : command.timezone().trim()));
    LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    securityRepository.insertLoginIdentity(
        new LoginIdentity(
            idGenerator.nextId(), user.userId(), CHANNEL, subjectHash, now, now));
    return user.userId();
  }

  /** 归一化手机号：去掉分隔符与 +86 前缀后校验中国大陆号段。 */
  private String normalizePhoneNumber(PhoneLoginCommand command) {
    if (isBlank(command.phoneNumber())) {
      throw new BusinessException("AUTH_INVALID_PHONE", "手机号不能为空");
    }
    String normalized =
        command.phoneNumber().replaceAll("[\\s\\-()]", "").replaceFirst("^\\+?86", "");
    if (!CN_MOBILE.matcher(normalized).matches()) {
      throw new BusinessException("AUTH_INVALID_PHONE", "手机号格式不正确");
    }
    return normalized;
  }

  /** 登录身份的不可枚举摘要；数据库不保存明文手机号。 */
  private String subjectHash(String phoneNumber) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest((CHANNEL + "|" + phoneNumber).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("JVM 不支持 SHA-256", exception);
    }
  }

  /** 仅在年龄证据引用中保留后四位，避免审计视图泄露完整号码。 */
  private String maskedSuffix(String phoneNumber) {
    return "****" + phoneNumber.substring(phoneNumber.length() - 4);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
