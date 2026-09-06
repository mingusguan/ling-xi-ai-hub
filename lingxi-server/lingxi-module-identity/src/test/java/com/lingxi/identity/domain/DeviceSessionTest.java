package com.lingxi.identity.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.kernel.BusinessException;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DeviceSessionTest {
  @Test
  void refreshDoesNotExtendRecentAuthenticationWindow() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    DeviceSession session =
        DeviceSession.create(
            1, "family", 2, "device", "a1", "r1", 3, now.plusMinutes(15), now.plusDays(30), now);

    assertThat(session.isRecentlyAuthenticated(now.plusMinutes(5), Duration.ofMinutes(5))).isTrue();
    session.rotate("r1", "a2", "r2", 3, now.plusMinutes(20), now.plusDays(30), now.plusMinutes(6));
    assertThat(session.isRecentlyAuthenticated(now.plusMinutes(6), Duration.ofMinutes(5)))
        .isFalse();
  }

  @Test
  void shouldRotateRefreshTokenAndRejectOldAuthorizationVersion() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    DeviceSession session =
        DeviceSession.create(
            1, "family", 2, "device", "a1", "r1", 3, now.plusMinutes(15), now.plusDays(30), now);
    session.rotate("r1", "a2", "r2", 4, now.plusMinutes(16), now.plusDays(30), now.plusMinutes(1));
    assertThat(session.getPreviousRefreshTokenHash()).isEqualTo("r1");
    assertThat(session.getRefreshTokenHash()).isEqualTo("r2");
    assertThatThrownBy(() -> session.ensureAccessAllowed(3, now.plusMinutes(2)))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("AUTH_SESSION_STALE");
  }

  @Test
  void shouldRevokeFamilyWhenRefreshTokenIsReplayed() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    DeviceSession session =
        DeviceSession.create(
            1, "family", 2, "device", "a1", "r1", 3, now.plusMinutes(15), now.plusDays(30), now);
    assertThatThrownBy(
            () -> session.rotate("old", "a2", "r2", 3, now.plusMinutes(15), now.plusDays(30), now))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("AUTH_REFRESH_REPLAY");
    assertThat(session.getRevokedAt()).isEqualTo(now);
  }
}
