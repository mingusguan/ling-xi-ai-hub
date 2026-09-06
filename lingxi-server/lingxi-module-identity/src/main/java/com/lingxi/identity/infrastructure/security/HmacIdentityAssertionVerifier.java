package com.lingxi.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.VerifiedIdentityAssertion;
import com.lingxi.identity.application.IdentityAssertionVerifier;
import com.lingxi.kernel.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 验证身份供应商桥接层签发的 HMAC 断言。 */
@Component
public class HmacIdentityAssertionVerifier implements IdentityAssertionVerifier {
  private static final Duration MAX_ASSERTION_AGE = Duration.ofMinutes(5);

  private final ObjectMapper objectMapper;
  private final byte[] secret;
  private final Clock clock;

  public HmacIdentityAssertionVerifier(
      ObjectMapper objectMapper, @Value("${lingxi.identity.assertion-secret}") String secret) {
    this(objectMapper, secret, Clock.systemUTC());
  }

  HmacIdentityAssertionVerifier(ObjectMapper objectMapper, String secret, Clock clock) {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("IDENTITY_ASSERTION_SECRET 至少需要 32 字节");
    }
    this.objectMapper = objectMapper;
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.clock = clock;
  }

  @Override
  public VerifiedIdentityAssertion verify(String signedAssertion, boolean birthDateRequired) {
    try {
      String[] parts = signedAssertion == null ? new String[0] : signedAssertion.split("\\.");
      if (parts.length != 2) {
        throw invalid();
      }
      byte[] expected = hmac(parts[0]);
      byte[] actual = Base64.getUrlDecoder().decode(parts[1]);
      if (!MessageDigest.isEqual(expected, actual)) {
        throw invalid();
      }
      byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
      VerifiedIdentityAssertion assertion =
          objectMapper.readValue(payload, VerifiedIdentityAssertion.class);
      validate(assertion, birthDateRequired);
      return assertion;
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw invalid();
    }
  }

  private void validate(VerifiedIdentityAssertion assertion, boolean birthDateRequired) {
    Instant now = clock.instant();
    if (assertion == null
        || isBlank(assertion.channel())
        || isBlank(assertion.subjectHash())
        || isBlank(assertion.verificationMethod())
        || isBlank(assertion.evidenceReference())
        || assertion.issuedAt() == null
        || assertion.issuedAt().isAfter(now.plusSeconds(30))
        || assertion.issuedAt().isBefore(now.minus(MAX_ASSERTION_AGE))
        || (birthDateRequired && assertion.verifiedBirthDate() == null)) {
      throw invalid();
    }
  }

  private byte[] hmac(String encodedPayload) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    return mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
  }

  private BusinessException invalid() {
    return new BusinessException("AUTH_INVALID_ASSERTION", "身份验证凭证无效或已过期");
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
