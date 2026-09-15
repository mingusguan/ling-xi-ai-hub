package com.lingxi.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingxi.identity.api.AccountStatus;
import com.lingxi.identity.api.AgeBand;
import com.lingxi.identity.api.CommonBlocker;
import com.lingxi.identity.api.CommunicationStyle;
import com.lingxi.identity.api.OnboardingProfile;
import com.lingxi.identity.api.ProactivityLevel;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 新手引导画像的领域不变量（PRD 8.2 ONB-01）。 */
class UserOnboardingProfileTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 15, 10, 0);

  private User adult() {
    return User.register(
        1,
        "public",
        "request",
        "digest",
        new AgeAssessment(AgeBand.ADULT, LocalDate.of(2000, 1, 1)),
        "Asia/Shanghai",
        NOW);
  }

  @Test
  void newAccountHasAnEmptyProfileAndNoCompletionMark() {
    User user = adult();
    assertThat(user.getOnboardingProfile().isEmpty()).isTrue();
    assertThat(user.isOnboardingCompleted()).isFalse();
    assertThat(user.getOnboardingCompletedAt()).isNull();
  }

  @Test
  void emptyProfileStillProvidesEffectiveDefaults() {
    OnboardingProfile empty = OnboardingProfile.empty();
    assertThat(empty.effectiveCommunicationStyle()).isEqualTo(CommunicationStyle.CONCISE);
    assertThat(empty.effectiveProactivityLevel()).isEqualTo(ProactivityLevel.MEDIUM);
  }

  @Test
  void savedProfileKeepsNullsInsteadOfFillingInDefaults() {
    User user = adult();
    OnboardingProfile profile =
        new OnboardingProfile(
            "小灵",
            LocalTime.of(23, 0),
            LocalTime.of(7, 0),
            600,
            LocalTime.of(9, 0),
            LocalTime.of(21, 0),
            null,
            null,
            CommunicationStyle.COACHING,
            null,
            Set.of(CommonBlocker.TIME, CommonBlocker.ENERGY));

    user.updateOnboardingProfile(profile, 0, NOW);

    assertThat(user.getOnboardingProfile().nickname()).isEqualTo("小灵");
    assertThat(user.getOnboardingProfile().weeklyAvailableMinutes()).isEqualTo(600);
    // 用户没选主动程度，就必须保持 null，不能被兜底值写进来。
    assertThat(user.getOnboardingProfile().proactivityLevel()).isNull();
    assertThat(user.getOnboardingProfile().effectiveProactivityLevel())
        .isEqualTo(ProactivityLevel.MEDIUM);
    assertThat(user.getVersion()).isEqualTo(1);
  }

  @Test
  void skippingOnboardingMarksItCompleteWhileLeavingTheProfileEmpty() {
    User user = adult();
    user.updateOnboardingProfile(OnboardingProfile.empty(), user.getVersion(), NOW);
    user.completeOnboarding(user.getVersion(), NOW);

    assertThat(user.isOnboardingCompleted()).isTrue();
    assertThat(user.getOnboardingProfile().isEmpty()).isTrue();
  }

  @Test
  void completingOnboardingTwiceKeepsTheFirstTimestamp() {
    User user = adult();
    user.completeOnboarding(0, NOW);
    LocalDateTime first = user.getOnboardingCompletedAt();
    user.completeOnboarding(user.getVersion(), NOW.plusDays(1));

    assertThat(user.getOnboardingCompletedAt()).isEqualTo(first);
  }

  @Test
  void reopeningOnboardingKeepsTheFilledProfile() {
    User user = adult();
    user.updateOnboardingProfile(
        new OnboardingProfile("小灵", null, null, null, null, null, null, null, null, null, Set.of()), 0, NOW);
    user.completeOnboarding(user.getVersion(), NOW);
    assertThat(user.isOnboardingCompleted()).isTrue();

    user.reopenOnboarding(user.getVersion(), NOW.plusHours(1));

    assertThat(user.isOnboardingCompleted()).isFalse();
    assertThat(user.getOnboardingProfile().nickname()).isEqualTo("小灵");
  }

  @Test
  void staleVersionIsRejected() {
    User user = adult();
    user.updateOnboardingProfile(OnboardingProfile.empty(), 0, NOW);

    assertThatThrownBy(() -> user.updateOnboardingProfile(OnboardingProfile.empty(), 0, NOW))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "IDENTITY_VERSION_CONFLICT");
  }

  @Test
  void closedAccountCannotEditTheProfile() {
    User user = adult();
    user.beginClosing(NOW);
    user.completeClosing(NOW);

    assertThatThrownBy(
            () -> user.updateOnboardingProfile(OnboardingProfile.empty(), user.getVersion(), NOW))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "IDENTITY_ACCOUNT_CLOSED");
  }

  @Test
  void accountStatusTransitionDoesNotWipeTheProfile() {
    User user = User.register(
        2,
        "teen",
        "request-teen",
        "digest-teen",
        new AgeAssessment(AgeBand.TEEN, LocalDate.of(2030, 8, 5)),
        "Asia/Shanghai",
        NOW);
    user.updateOnboardingProfile(
        new OnboardingProfile("阿宝", null, null, null, null, null, null, null, null, null, Set.of()), 0, NOW);

    user.activateTeenAccount(NOW.plusMinutes(1));

    assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE_TEEN);
    assertThat(user.getOnboardingProfile().nickname()).isEqualTo("阿宝");
  }
}
