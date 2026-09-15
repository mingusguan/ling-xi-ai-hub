package com.lingxi.platform.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

/**
 * 日志脱敏转换器：在日志落盘前抹掉凭据与联系方式。
 *
 * <p>为什么需要它：访问令牌、刷新令牌、管理员口令、短信验证码和手机号都可能出现在
 * 日志里（例如请求追踪、调试打印、异常消息），而日志被采集、转发、留存的时间远长于
 * 数据库；一旦泄漏，令牌在有效期内可直接冒用身份，手机号属于个人信息。
 * 这些值在业务代码里难以逐处避免，因此在输出层统一兜底。
 *
 * <p>只做「明显形态」的替换，不追求语义识别：命中越多误杀风险越大，把正常日志打成
 * 一片星号反而会让人失去排障能力。因此这里只覆盖四类高价值目标：
 * <ul>
 *   <li>{@code Authorization: Bearer xxx} 与 JSON 里的 accessToken/refreshToken
 *   <li>含 password/secret 的键值对
 *   <li>11 位手机号（保留前 3 后 4，便于按用户排查）
 *   <li>短信验证码键值对
 * </ul>
 */
public class SensitiveDataMaskingConverter extends ClassicConverter {
  private static final String MASK = "***";

  /** Bearer 令牌：保留前缀便于确认「确实带了令牌」。 */
  private static final Pattern BEARER =
      Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]{8,}");

  /** JSON 或键值对里的令牌字段。 */
  private static final Pattern TOKEN_FIELD =
      Pattern.compile(
          "(?i)(\"?(?:accessToken|refreshToken|access_token|refresh_token|signedAssertion|signed_assertion)\"?\\s*[:=]\\s*\"?)([^\",}\\s]{8,})");

  /** 口令与密钥字段。 */
  private static final Pattern SECRET_FIELD =
      Pattern.compile(
          "(?i)(\"?(?:password|passwd|newPassword|assertion-secret|privacy-export-key|secret|token)\"?\\s*[:=]\\s*\"?)([^\",}\\s]{4,})");

  /** 短信验证码字段。 */
  private static final Pattern OTP_FIELD =
      Pattern.compile("(?i)(\"?(?:code|otp|smsCode|verificationCode)\"?\\s*[:=]\\s*\"?)(\\d{4,8})");

  /** 中国大陆手机号：保留前 3 后 4，既能定位用户又不构成完整个人信息。 */
  private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1[3-9]\\d)(\\d{4})(\\d{4})(?!\\d)");

  @Override
  public String convert(ILoggingEvent event) {
    return mask(event.getFormattedMessage());
  }

  /** 供单元测试直接调用，避免为了验证规则去搭一套 logback 上下文。 */
  public static String mask(String message) {
    if (message == null || message.isEmpty()) {
      return message;
    }
    String masked = BEARER.matcher(message).replaceAll("$1" + MASK);
    masked = TOKEN_FIELD.matcher(masked).replaceAll("$1" + MASK);
    masked = SECRET_FIELD.matcher(masked).replaceAll("$1" + MASK);
    masked = OTP_FIELD.matcher(masked).replaceAll("$1" + MASK);
    masked = PHONE.matcher(masked).replaceAll("$1****$3");
    return masked;
  }
}
