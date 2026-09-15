package com.lingxi.platform.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 日志脱敏规则。
 *
 * <p>这些断言的价值在于「脱敏不能太激进」：把所有内容都打成星号会让日志失去排障能力，
 * 因此每条规则都同时验证「敏感值被抹掉」和「周围上下文保留」。
 */
class SensitiveDataMaskingConverterTest {

  @Test
  void bearerTokenIsMaskedButThePrefixIsKept() {
    String masked = SensitiveDataMaskingConverter.mask("Authorization: Bearer abc123DEF456ghi789");
    assertThat(masked).doesNotContain("abc123DEF456ghi789");
    assertThat(masked).contains("Bearer");
  }

  @Test
  void accessAndRefreshTokenFieldsAreMasked() {
    String masked =
        SensitiveDataMaskingConverter.mask(
            "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9.payload.sig\",\"refreshToken\":\"rt-9876543210\"}");
    assertThat(masked).doesNotContain("eyJhbGciOiJIUzI1NiJ9.payload.sig");
    assertThat(masked).doesNotContain("rt-9876543210");
    assertThat(masked).contains("accessToken");
    assertThat(masked).contains("refreshToken");
  }

  @Test
  void passwordAndSecretFieldsAreMasked() {
    String masked =
        SensitiveDataMaskingConverter.mask("password=Sup3rSecret! assertion-secret: topsecret-value");
    assertThat(masked).doesNotContain("Sup3rSecret!");
    assertThat(masked).doesNotContain("topsecret-value");
    assertThat(masked).contains("password");
  }

  @Test
  void smsVerificationCodeIsMasked() {
    String masked = SensitiveDataMaskingConverter.mask("{\"phone\":\"13800001234\",\"code\":\"654321\"}");
    assertThat(masked).doesNotContain("654321");
    assertThat(masked).contains("\"code\"");
  }

  @Test
  void phoneNumberKeepsEnoughToIdentifyTheUser() {
    String masked = SensitiveDataMaskingConverter.mask("phone 13912345678 login ok");
    assertThat(masked).doesNotContain("13912345678");
    // 保留前 3 后 4：既不是完整个人信息，又能按用户定位问题。
    assertThat(masked).contains("139");
    assertThat(masked).contains("5678");
  }

  @Test
  void ordinaryLogLinesAreLeftAlone() {
    String line = "goal 2099382027332988929 occurrence status=SCHEDULED retried 2 times";
    assertThat(SensitiveDataMaskingConverter.mask(line)).isEqualTo(line);
  }

  @Test
  void shortNumberLikeValuesAreNotTreatedAsPhoneNumbers() {
    // 只要不是 11 位手机号形态，数字 ID 与计数器都不该被改写，否则日志难以对照。
    String line = "runId=1234567890123 latencyMillis=42";
    assertThat(SensitiveDataMaskingConverter.mask(line)).isEqualTo(line);
  }
}
