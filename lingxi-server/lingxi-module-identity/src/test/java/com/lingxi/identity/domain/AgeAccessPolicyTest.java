package com.lingxi.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingxi.identity.api.AgeBand;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AgeAccessPolicyTest {
  private final AgeAccessPolicy policy = new AgeAccessPolicy();
  private final LocalDate today = LocalDate.of(2026, 8, 5);

  @Test
  void shouldRejectUnderFourteen() {
    assertThat(policy.assess(LocalDate.of(2012, 8, 6), today).ageBand())
        .isEqualTo(AgeBand.UNDER_14);
  }

  @Test
  void shouldTreatFourteenAsTeen() {
    AgeAssessment assessment = policy.assess(LocalDate.of(2012, 8, 5), today);
    assertThat(assessment.ageBand()).isEqualTo(AgeBand.TEEN);
    assertThat(assessment.adultTransitionDate()).isEqualTo(LocalDate.of(2030, 8, 5));
  }

  @Test
  void shouldTreatEighteenAsAdult() {
    assertThat(policy.assess(LocalDate.of(2008, 8, 5), today).ageBand()).isEqualTo(AgeBand.ADULT);
  }
}
