package com.lingxi.identity.application;

import com.lingxi.identity.api.AccessProfile;
import com.lingxi.identity.api.AuthenticatedSession;
import com.lingxi.identity.api.AuthenticationFacade;
import com.lingxi.identity.api.LoginWithAssertionCommand;
import com.lingxi.identity.api.RefreshSessionCommand;
import com.lingxi.identity.api.SessionTokens;
import com.lingxi.identity.api.VerifiedIdentityAssertion;
import com.lingxi.identity.domain.DeviceSession;
import com.lingxi.identity.domain.IdentitySecurityRepository;
import com.lingxi.identity.domain.LoginIdentity;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Opaque Token 登录、轮换、重放防护和授权版本校验。 */
@Service
public class AuthenticationApplicationService implements AuthenticationFacade {
  private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
  private static final Duration REFRESH_TTL = Duration.ofDays(30);

  private final IdentitySecurityRepository repository;
  private final IdentityApplicationService identityService;
  private final IdentityAssertionVerifier assertionVerifier;
  private final SecureTokenGenerator tokenGenerator;
  private final SessionReplayRevoker replayRevoker;
  private final IdGenerator idGenerator;
  private final Clock clock;
  private final Duration recentAuthenticationWindow;
  private final LoginRiskRecorder riskRecorder;

  public AuthenticationApplicationService(
      IdentitySecurityRepository repository,
      IdentityApplicationService identityService,
      IdentityAssertionVerifier assertionVerifier,
      SecureTokenGenerator tokenGenerator,
      SessionReplayRevoker replayRevoker,
      IdGenerator idGenerator,
      LoginRiskRecorder riskRecorder,
      @Value("${lingxi.identity.recent-authentication-window:PT5M}")
          Duration recentAuthenticationWindow) {
    this(
        repository,
        identityService,
        assertionVerifier,
        tokenGenerator,
        replayRevoker,
        idGenerator,
        Clock.systemUTC(),
        recentAuthenticationWindow,
        riskRecorder);
  }

  AuthenticationApplicationService(
      IdentitySecurityRepository repository,
      IdentityApplicationService identityService,
      IdentityAssertionVerifier assertionVerifier,
      SecureTokenGenerator tokenGenerator,
      SessionReplayRevoker replayRevoker,
      IdGenerator idGenerator,
      Clock clock,
      Duration recentAuthenticationWindow) {
    this(repository,identityService,assertionVerifier,tokenGenerator,replayRevoker,idGenerator,
        clock,recentAuthenticationWindow,null);
  }

  AuthenticationApplicationService(
      IdentitySecurityRepository repository,
      IdentityApplicationService identityService,
      IdentityAssertionVerifier assertionVerifier,
      SecureTokenGenerator tokenGenerator,
      SessionReplayRevoker replayRevoker,
      IdGenerator idGenerator,
      Clock clock,
      Duration recentAuthenticationWindow,
      LoginRiskRecorder riskRecorder) {
    this.repository = repository;
    this.identityService = identityService;
    this.assertionVerifier = assertionVerifier;
    this.tokenGenerator = tokenGenerator;
    this.replayRevoker = replayRevoker;
    this.idGenerator = idGenerator;
    this.clock = clock;
    this.recentAuthenticationWindow = recentAuthenticationWindow;
    this.riskRecorder = riskRecorder;
  }

  @Override
  @Transactional
  public SessionTokens login(LoginWithAssertionCommand command) {
    if (command == null || isBlank(command.deviceId()) || isBlank(command.signedAssertion())) {
      throw new BusinessException("AUTH_INVALID_LOGIN", "登录参数不完整");
    }
    VerifiedIdentityAssertion assertion =
        assertionVerifier.verify(command.signedAssertion(), false);
    LoginIdentity loginIdentity =
        repository
            .findLoginIdentity(assertion.channel(), assertion.subjectHash())
            .orElseThrow(() -> new BusinessException("AUTH_IDENTITY_NOT_FOUND", "登录身份不存在"));
    AccessProfile profile = identityService.getAccessProfile(loginIdentity.userId());
    if (profile.status() == com.lingxi.identity.api.AccountStatus.CLOSED) {
      throw new BusinessException("AUTH_ACCOUNT_CLOSED", "账号已关闭");
    }
    Instant now = clock.instant();
    String accessToken = tokenGenerator.nextToken();
    String refreshToken = tokenGenerator.nextToken();
    String familyId = tokenGenerator.nextToken();
    DeviceSession session =
        DeviceSession.create(
            idGenerator.nextId(),
            familyId,
            profile.userId(),
            command.deviceId(),
            hash(accessToken),
            hash(refreshToken),
            profile.authorizationVersion(),
            utc(now.plus(ACCESS_TTL)),
            utc(now.plus(REFRESH_TTL)),
            utc(now));
    repository.ensureDevice(idGenerator.nextId(), profile.userId(), command.deviceId(), utc(now));
    repository.insertSession(session);
    return tokens(accessToken, refreshToken, session, profile);
  }

  @Override
  @Transactional
  public SessionTokens refresh(RefreshSessionCommand command) {
    if (command == null
        || isBlank(command.sessionFamilyId())
        || isBlank(command.refreshToken())
        || isBlank(command.deviceId())) {
      throw new BusinessException("AUTH_INVALID_REFRESH", "刷新参数不完整");
    }
    DeviceSession session =
        repository
            .findSessionByFamilyId(command.sessionFamilyId())
            .orElseThrow(() -> new BusinessException("AUTH_SESSION_NOT_FOUND", "会话不存在"));
    if (!session.getDeviceId().equals(command.deviceId())) {
      recordRisk(session.getUserId(),command.deviceId(),"MEDIUM","DEVICE_MISMATCH","刷新会话的设备标识不一致");
      throw new BusinessException("AUTH_DEVICE_MISMATCH", "会话设备不匹配");
    }
    String presentedHash = hash(command.refreshToken());
    LocalDateTime now = utc(clock.instant());
    if (!session.getRefreshTokenHash().equals(presentedHash)) {
      replayRevoker.revoke(session.getFamilyId(), now);
      recordRisk(session.getUserId(),command.deviceId(),"HIGH","REFRESH_REPLAY","检测到刷新令牌重放并已撤销会话族");
      throw new BusinessException("AUTH_REFRESH_REPLAY", "检测到刷新令牌重放，会话族已撤销");
    }

    AccessProfile profile = identityService.getAccessProfile(session.getUserId());
    String accessToken = tokenGenerator.nextToken();
    String refreshToken = tokenGenerator.nextToken();
    long previousVersion = session.getVersion();
    session.rotate(
        presentedHash,
        hash(accessToken),
        hash(refreshToken),
        profile.authorizationVersion(),
        now.plus(ACCESS_TTL),
        now.plus(REFRESH_TTL),
        now);
    if (!repository.updateSession(session, previousVersion)) {
      throw new BusinessException("AUTH_SESSION_CONFLICT", "会话已在其他终端刷新");
    }
    return tokens(accessToken, refreshToken, session, profile);
  }

  @Override
  @Transactional(readOnly = true)
  public AuthenticatedSession authenticate(String accessToken) {
    if (isBlank(accessToken)) {
      throw new BusinessException("AUTH_UNAUTHENTICATED", "请先登录");
    }
    DeviceSession session =
        repository
            .findSessionByAccessHash(hash(accessToken))
            .orElseThrow(() -> new BusinessException("AUTH_UNAUTHENTICATED", "登录凭证无效"));
    AccessProfile profile = identityService.getAccessProfile(session.getUserId());
    session.ensureAccessAllowed(profile.authorizationVersion(), utc(clock.instant()));
    return new AuthenticatedSession(
        session.getUserId(),
        session.getDeviceId(),
        session.getAuthorizationVersion(),
        session.isRecentlyAuthenticated(utc(clock.instant()), recentAuthenticationWindow));
  }

  @Override
  @Transactional
  public void revokeAllSessions(long userId, String reason) {
    repository.revokeAllSessions(userId, reason, utc(clock.instant()));
  }

  private void recordRisk(long userId,String deviceId,String level,String type,String summary){
    if(riskRecorder!=null)riskRecorder.record(userId,deviceId,level,type,summary);
  }

  private SessionTokens tokens(
      String access, String refresh, DeviceSession session, AccessProfile profile) {
    return new SessionTokens(
        access,
        refresh,
        session.getFamilyId(),
        session.getAccessExpiresAt().toInstant(ZoneOffset.UTC),
        session.getRefreshExpiresAt().toInstant(ZoneOffset.UTC),
        profile.ageBand(),
        profile.status(),
        profile.authorizationVersion());
  }

  private String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("JVM 不支持 SHA-256", exception);
    }
  }

  private LocalDateTime utc(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
