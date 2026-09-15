package com.lingxi.goal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingxi.goal.api.GoalStatus;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class GoalTest {

  @Test
  void shouldActivatePlanAndAdvanceVersion() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    Goal goal =
        Goal.create(
            1,
            "public",
            10,
            "request",
            "digest",
            GoalDefinition.of("跑半马", "两小时内完赛"),
            now);
    goal.activatePlan(20, 0, now.plusMinutes(1));
    assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
    assertThat(goal.getCurrentPlanVersionId()).isEqualTo(20);
    assertThat(goal.getVersion()).isEqualTo(1);
  }

  @Test
  void shouldRejectStaleClientVersion() {
    Goal goal =
        Goal.create(
            1,
            "public",
            10,
            "request",
            "digest",
            GoalDefinition.of("跑半马", "完赛"),
            LocalDateTime.now());
    assertThatThrownBy(() -> goal.activatePlan(20, 1, LocalDateTime.now()))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_VERSION_CONFLICT");
  }

  @Test
  void pauseExcludesGoalFromActiveQuotaAndResumeRestoresIt() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    Goal goal =
        Goal.create(
            1,
            "public",
            10,
            "request",
            "digest",
            GoalDefinition.of("跑半马", "完赛"),
            now);
    goal.activatePlan(20, 0, now);
    assertThat(goal.countsAsActive()).isTrue();

    goal.pause(LocalDate.of(2026, 9, 1), goal.getVersion(), now.plusMinutes(1));
    assertThat(goal.getStatus()).isEqualTo(GoalStatus.PAUSED);
    assertThat(goal.getPauseResumeAt()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(goal.countsAsActive()).isFalse();

    goal.resume(goal.getVersion(), now.plusMinutes(2));
    assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
    assertThat(goal.getPauseResumeAt()).isNull();
    assertThat(goal.countsAsActive()).isTrue();
  }

  @Test
  void abandonRequiresReasonAndClearsPausePlan() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    Goal goal =
        Goal.create(
            1,
            "public",
            10,
            "request",
            "digest",
            GoalDefinition.of("跑半马", "完赛"),
            now);
    goal.activatePlan(20, 0, now);
    assertThatThrownBy(() -> goal.abandon("  ", goal.getVersion(), now))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_ABANDON_REASON_REQUIRED");

    goal.abandon("时间安排冲突，改到明年", goal.getVersion(), now.plusMinutes(1));
    assertThat(goal.getStatus()).isEqualTo(GoalStatus.ABANDONED);
    assertThat(goal.getAbandonReason()).isEqualTo("时间安排冲突，改到明年");
    assertThat(goal.countsAsActive()).isFalse();
  }

  @Test
  void activeGoalCannotBeArchivedDirectly() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    Goal goal =
        Goal.create(
            1,
            "public",
            10,
            "request",
            "digest",
            GoalDefinition.of("跑半马", "完赛"),
            now);
    goal.activatePlan(20, 0, now);
    assertThatThrownBy(() -> goal.archive(goal.getVersion(), now))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_ARCHIVE_NOT_ALLOWED");
  }
}
