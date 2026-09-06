package com.lingxi.goal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingxi.goal.api.GoalStatus;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class GoalTest {

  @Test
  void shouldActivatePlanAndAdvanceVersion() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    Goal goal = Goal.create(1, "public", 10, "request", "digest", "跑半马", "两小时内完赛", now);
    goal.activatePlan(20, 0, now.plusMinutes(1));
    assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
    assertThat(goal.getCurrentPlanVersionId()).isEqualTo(20);
    assertThat(goal.getVersion()).isEqualTo(1);
  }

  @Test
  void shouldRejectStaleClientVersion() {
    Goal goal = Goal.create(1, "public", 10, "request", "digest", "跑半马", "完赛", LocalDateTime.now());
    assertThatThrownBy(() -> goal.activatePlan(20, 1, LocalDateTime.now()))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_VERSION_CONFLICT");
  }
}
