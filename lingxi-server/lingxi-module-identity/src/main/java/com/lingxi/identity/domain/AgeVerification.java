package com.lingxi.identity.domain;

import com.lingxi.identity.api.AgeBand;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** 年龄验证结果。证据只保存安全引用，不保存证件原文。 */
public record AgeVerification(
    long id,
    long userId,
    String method,
    String evidenceReference,
    LocalDate verifiedBirthDate,
    AgeBand result,
    LocalDateTime verifiedAt,
    LocalDateTime createdAt) {

  public AgeVerification {
    if (id <= 0 || userId <= 0) {
      throw new IllegalArgumentException("id and userId must be positive");
    }
    Objects.requireNonNull(method, "method");
    Objects.requireNonNull(evidenceReference, "evidenceReference");
    Objects.requireNonNull(verifiedBirthDate, "verifiedBirthDate");
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(verifiedAt, "verifiedAt");
    Objects.requireNonNull(createdAt, "createdAt");
  }
}
