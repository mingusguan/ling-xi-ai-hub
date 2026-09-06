package com.lingxi.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingxi.identity.api.AccountStatus;
import com.lingxi.identity.api.AgeBand;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void teenNeedsGuardianBeforeCoreFeaturesBecomeAvailable() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    User user =
        User.register(
            1,
            "public",
            "request",
            "digest",
            new AgeAssessment(AgeBand.TEEN, LocalDate.of(2030, 8, 5)),
            "Asia/Shanghai",
            now);
    assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING_GUARDIAN);
    assertThat(user.canUseCoreFeatures()).isFalse();

    user.activateTeenAccount(now.plusMinutes(1));

    assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE_TEEN);
    assertThat(user.canUseCoreFeatures()).isTrue();
    assertThat(user.getAuthorizationVersion()).isEqualTo(2);
  }

  @Test
  void adultIsActiveImmediately() {
    User user =
        User.register(
            1,
            "public",
            "request",
            "digest",
            new AgeAssessment(AgeBand.ADULT, LocalDate.of(2020, 1, 1)),
            "Asia/Shanghai",
            LocalDateTime.of(2026, 8, 5, 10, 0));
    assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE_ADULT);
    assertThat(user.canUseCoreFeatures()).isTrue();
  }

  @Test
  void cancellingClosureRestoresTheExactPreviousAccountState() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    User user =
        User.register(
            2,
            "teen",
            "request-2",
            "digest-2",
            new AgeAssessment(AgeBand.TEEN, LocalDate.of(2030, 8, 5)),
            "Asia/Shanghai",
            now);
    user.beginClosing(now.plusMinutes(1));
    user.cancelClosing(now.plusMinutes(2));

    assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING_GUARDIAN);
    assertThat(user.getClosingPreviousStatus()).isNull();
  }
}
