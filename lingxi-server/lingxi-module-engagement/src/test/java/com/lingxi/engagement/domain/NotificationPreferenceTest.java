package com.lingxi.engagement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingxi.engagement.api.NotificationChannel;
import java.time.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NotificationPreferenceTest {
  @Test
  void teenQuietHoursAreMandatoryAndSafetyCanBypass() {
    var p =
        NotificationPreference.create(
            1,
            "REMINDER",
            Set.of(NotificationChannel.PUSH),
            null,
            null,
            "Asia/Shanghai",
            true,
            LocalDateTime.now());
    assertThat(p.getQuietStart()).isEqualTo(LocalTime.of(22, 0));
    assertThat(p.getQuietEnd()).isEqualTo(LocalTime.of(7, 0));
    Instant at23 =
        LocalDateTime.of(2026, 8, 5, 23, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant();
    assertThat(p.allows(NotificationChannel.PUSH, at23, false)).isFalse();
    assertThat(p.allows(NotificationChannel.PUSH, at23, true)).isTrue();
  }
}
