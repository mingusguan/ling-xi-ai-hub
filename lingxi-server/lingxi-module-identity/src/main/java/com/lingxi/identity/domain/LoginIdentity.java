package com.lingxi.identity.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/** 经过外部渠道验证后的不可枚举登录身份。 */
public record LoginIdentity(
    long id,
    long userId,
    String channel,
    String subjectHash,
    LocalDateTime verifiedAt,
    LocalDateTime createdAt) {
  public LoginIdentity {
    if (id <= 0 || userId <= 0) {
      throw new IllegalArgumentException("id and userId must be positive");
    }
    Objects.requireNonNull(channel);
    Objects.requireNonNull(subjectHash);
    Objects.requireNonNull(verifiedAt);
    Objects.requireNonNull(createdAt);
  }
}
