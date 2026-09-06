package com.lingxi.relationship.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.kernel.BusinessException;
import com.lingxi.relationship.api.PartnerPermission;
import java.time.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PartnerGrantAndShareTest {
  @Test
  void grantRevocationIsImmediateAndOwnerIsNotPartner() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 6, 0);
    var g =
        PartnerGrant.create(
            1,
            2,
            10,
            3,
            Set.of(PartnerPermission.VIEW_PROGRESS),
            Instant.parse("2026-08-06T00:00:00Z"),
            now);
    assertThat(
            g.permits(11, PartnerPermission.VIEW_PROGRESS, Instant.parse("2026-08-05T00:00:00Z")))
        .isTrue();
    assertThat(
            g.permits(10, PartnerPermission.VIEW_PROGRESS, Instant.parse("2026-08-05T00:00:00Z")))
        .isFalse();
    g.revoke(10, 0, now.plusMinutes(1));
    assertThat(
            g.permits(11, PartnerPermission.VIEW_PROGRESS, Instant.parse("2026-08-05T00:01:00Z")))
        .isFalse();
  }

  @Test
  void revokedShareCannotBeAccessed() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 6, 0);
    var s =
        ShareLink.create(
            1,
            "k",
            "d",
            2,
            "GOAL",
            "3",
            Set.of("title"),
            "{\"title\":\"x\"}",
            "hash",
            null,
            Instant.parse("2026-08-06T00:00:00Z"),
            1,
            now);
    s.revoke(2, 0, now.plusMinutes(1));
    assertThatThrownBy(() -> s.assertAccessible(Instant.parse("2026-08-05T00:02:00Z")))
        .isInstanceOf(BusinessException.class);
  }
}
