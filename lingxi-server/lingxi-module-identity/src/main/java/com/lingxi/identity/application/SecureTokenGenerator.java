package com.lingxi.identity.application;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/** 生成不可枚举的 Opaque Token。 */
@Component
public class SecureTokenGenerator {
  private final SecureRandom random = new SecureRandom();

  public String nextToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
