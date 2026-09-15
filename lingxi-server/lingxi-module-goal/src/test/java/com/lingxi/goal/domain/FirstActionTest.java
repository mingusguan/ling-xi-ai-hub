package com.lingxi.goal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingxi.goal.api.ActionDifficulty;
import com.lingxi.goal.api.PriorityLevel;
import com.lingxi.goal.api.RecurrenceType;
import com.lingxi.kernel.BusinessException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 新手引导首行动的领域约束（PRD 8.2 ONB-02）。
 *
 * <p>PRD 要求「创建成功后直接生成一个 5—30 分钟的首行动，降低启动门槛」。
 * 这条承诺只有在服务端强制时才成立，因此这里固定住三条约束：必须是一次性行动、
 * 必须给出预计时长、时长必须落在 5—30 分钟内。
 */
class FirstActionTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 15, 10, 0);

  private ActionDetail first(int minutes) {
    return new ActionDetail(
        "起步动作",
        PriorityLevel.NORMAL,
        ActionDifficulty.EASY,
        "做完并确认",
        minutes,
        null,
        null,
        true);
  }

  private ActionSchedule onceSchedule() {
    return new ActionSchedule(
        RecurrenceType.ONCE,
        Set.of(),
        null,
        LocalDate.of(2026, 9, 15),
        null,
        LocalTime.of(20, 0),
        null,
        "Asia/Shanghai");
  }

  private Action action(ActionDetail detail, ActionSchedule schedule) {
    return new Action(
        1L,
        2L,
        3L,
        null,
        "first-action",
        "读 10 分钟",
        schedule,
        detail,
        ActionStatus.ACTIVE,
        0L,
        NOW,
        NOW);
  }

  @Test
  void firstActionAcceptsTheBoundariesOfTheFiveToThirtyMinuteWindow() {
    assertThat(first(5).first()).isTrue();
    assertThat(first(30).first()).isTrue();
  }

  @Test
  void firstActionBelowFiveMinutesIsRejected() {
    assertThatThrownBy(() -> first(4))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "GOAL_INVALID_FIRST_ACTION");
  }

  @Test
  void firstActionAboveThirtyMinutesIsRejected() {
    assertThatThrownBy(() -> first(31))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "GOAL_INVALID_FIRST_ACTION");
  }

  @Test
  void firstActionWithoutAnEstimateIsRejected() {
    // 缺时长时「5—30 分钟能做完」这条承诺无法核对，因此按错误处理而不是按默许处理。
    assertThatThrownBy(() -> first(0).asFirst(true))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "GOAL_INVALID_ESTIMATED_MINUTES");
    assertThatThrownBy(
            () ->
                new ActionDetail(
                    null, null, null, null, null, null, null, true))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "GOAL_INVALID_FIRST_ACTION");
  }

  @Test
  void ordinaryActionsKeepHavingNoDurationRequirement() {
    // 普通行动不受首行动约束：预计时长仍然完全可选。
    ActionDetail ordinary = ActionDetail.defaults();
    assertThat(ordinary.first()).isFalse();
    assertThat(ordinary.estimatedMinutes()).isNull();
  }

  @Test
  void firstActionCannotBeEditedIntoARecurringAction() {
    ActionDetail detail = first(10);
    Action action = action(detail, onceSchedule());

    ActionSchedule daily =
        new ActionSchedule(
            RecurrenceType.DAILY,
            Set.of(),
            null,
            LocalDate.of(2026, 9, 15),
            null,
            LocalTime.of(20, 0),
            null,
            "Asia/Shanghai");

    assertThatThrownBy(() -> action.edited("读 10 分钟", daily, detail, NOW))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "GOAL_INVALID_FIRST_ACTION");
  }

  @Test
  void firstActionCanStillBeEditedWhileStayingOneOff() {
    ActionDetail detail = first(10);
    Action action = action(detail, onceSchedule());

    Action edited = action.edited("读 15 分钟", onceSchedule(), first(15), NOW);

    assertThat(edited.detail().first()).isTrue();
    assertThat(edited.detail().estimatedMinutes()).isEqualTo(15);
    assertThat(edited.title()).isEqualTo("读 15 分钟");
  }

  @Test
  void movingAFirstActionKeepsItsFirstFlag() {
    Action action = action(first(10), onceSchedule());

    Action moved = action.movedTo(LocalDate.of(2026, 9, 16), NOW);

    assertThat(moved.detail().first()).isTrue();
    assertThat(moved.startDate()).isEqualTo(LocalDate.of(2026, 9, 16));
  }

  @Test
  void weeklyFirstActionIsRejectedByTheScheduleRule() {
    // 首行动必须一次性：每周重复的行动不属于「降低启动门槛」的首行动。
    ActionSchedule weeklyWithoutWeekdays =
        new ActionSchedule(
            RecurrenceType.WEEKLY,
            Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            null,
            LocalDate.of(2026, 9, 15),
            null,
            LocalTime.of(20, 0),
            null,
            "Asia/Shanghai");
    Action action = action(first(10), onceSchedule());

    assertThatThrownBy(() -> action.edited("读 10 分钟", weeklyWithoutWeekdays, first(10), NOW))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "GOAL_INVALID_FIRST_ACTION");
  }
}
