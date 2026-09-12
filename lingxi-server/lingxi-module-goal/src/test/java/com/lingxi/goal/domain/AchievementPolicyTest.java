package com.lingxi.goal.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.goal.api.AchievementType;
import com.lingxi.goal.api.CheckInResultType;
import com.lingxi.goal.api.GoalStatus;
import com.lingxi.goal.api.RecurrenceType;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AchievementPolicyTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 12, 10, 0);
  private static final long USER_ID = 9L;

  @Test
  void completedGoalEarnsOneAchievement() {
    Goal completed = goal(GoalStatus.COMPLETED, 100);

    Optional<Achievement> achievement = AchievementPolicy.goalCompleted(101, completed, NOW);

    assertThat(achievement).isPresent();
    assertThat(achievement.get().type()).isEqualTo(AchievementType.GOAL_COMPLETED);
    assertThat(achievement.get().referenceKey()).isEqualTo("goal-completed:5");
    assertThat(achievement.get().description()).contains("每日阅读");
  }

  @Test
  void activeGoalEarnsNothing() {
    assertThat(AchievementPolicy.goalCompleted(101, goal(GoalStatus.ACTIVE, 80), NOW)).isEmpty();
  }

  @Test
  void milestoneEarnedOnlyWhenEveryOccurrenceCompleted() {
    Milestone milestone = new Milestone(31, 3, 1, "第一周适应", "完成三次行动", NOW);
    List<Action> actions = List.of(action(41, 31));
    MilestoneProgress completed =
        new MilestoneProgress(
            USER_ID, milestone, actions, List.of(occurrence(51, 41, OccurrenceStatus.COMPLETED)));

    Optional<Achievement> achievement =
        AchievementPolicy.milestoneCompleted(102, completed, NOW);

    assertThat(achievement).isPresent();
    assertThat(achievement.get().type()).isEqualTo(AchievementType.MILESTONE_COMPLETED);
    assertThat(achievement.get().referenceKey()).isEqualTo("milestone-completed:31");
    assertThat(achievement.get().goalId()).isEqualTo(5L);
  }

  @Test
  void milestoneNotEarnedWhileAnyOccurrenceUnfinished() {
    Milestone milestone = new Milestone(31, 3, 1, "第一周适应", "完成三次行动", NOW);
    List<Action> actions = List.of(action(41, 31));
    MilestoneProgress partial =
        new MilestoneProgress(
            USER_ID,
            milestone,
            actions,
            List.of(
                occurrence(51, 41, OccurrenceStatus.COMPLETED),
                occurrence(52, 41, OccurrenceStatus.PARTIAL)));

    assertThat(AchievementPolicy.milestoneCompleted(102, partial, NOW)).isEmpty();
  }

  @Test
  void milestoneWithoutGeneratedOccurrenceEarnsNothing() {
    Milestone milestone = new Milestone(31, 3, 1, "第一周适应", "完成三次行动", NOW);
    MilestoneProgress empty =
        new MilestoneProgress(USER_ID, milestone, List.of(action(41, 31)), List.of());

    assertThat(AchievementPolicy.milestoneCompleted(102, empty, NOW)).isEmpty();
  }

  @Test
  void streakEarnedOnFirstDayReachingThreshold() {
    LocalDate endDate = LocalDate.of(2026, 9, 12);
    CheckInStreak streak = new CheckInStreak(endDate, streakDates(endDate, 7), 7);

    Optional<Achievement> achievement =
        AchievementPolicy.checkInStreak(103, USER_ID, streak, NOW);

    assertThat(achievement).isPresent();
    assertThat(achievement.get().type()).isEqualTo(AchievementType.CHECK_IN_STREAK);
    assertThat(achievement.get().referenceKey())
        .isEqualTo("check-in-streak:9:7:2026-09-12");
    assertThat(achievement.get().title()).isEqualTo("连续打卡 7 天");
  }

  @Test
  void streakNotEarnedWhenWindowHasGap() {
    LocalDate endDate = LocalDate.of(2026, 9, 12);
    Set<LocalDate> dates = new HashSet<>(streakDates(endDate, 7));
    dates.remove(endDate.minusDays(2));

    assertThat(AchievementPolicy.checkInStreak(103, USER_ID, new CheckInStreak(endDate, dates, 7), NOW))
        .isEmpty();
  }

  @Test
  void streakNotRepeatedWhileContinuousRecordContinues() {
    LocalDate endDate = LocalDate.of(2026, 9, 12);
    Set<LocalDate> dates = streakDates(endDate, 8);

    assertThat(AchievementPolicy.checkInStreak(103, USER_ID, new CheckInStreak(endDate, dates, 7), NOW))
        .isEmpty();
  }

  @Test
  void streakEarnedAgainAfterInterruption() {
    LocalDate endDate = LocalDate.of(2026, 9, 20);
    Set<LocalDate> dates = streakDates(endDate, 7);

    Optional<Achievement> achievement =
        AchievementPolicy.checkInStreak(103, USER_ID, new CheckInStreak(endDate, dates, 7), NOW);

    assertThat(achievement).isPresent();
    assertThat(achievement.get().referenceKey())
        .isEqualTo("check-in-streak:9:7:2026-09-20");
  }

  private static Set<LocalDate> streakDates(LocalDate endDate, int days) {
    Set<LocalDate> dates = new HashSet<>();
    for (int offset = 0; offset < days; offset++) {
      dates.add(endDate.minusDays(offset));
    }
    return dates;
  }

  private static Goal goal(GoalStatus status, int progress) {
    return Goal.rehydrate(
        5,
        "G-5",
        USER_ID,
        "goal-key",
        "digest",
        "每日阅读",
        "连续阅读 30 天",
        status,
        3L,
        progress,
        4,
        NOW,
        NOW);
  }

  private static Action action(long id, long milestoneId) {
    return new Action(
        id,
        5,
        3,
        milestoneId,
        "client-" + id,
        "阅读 20 分钟",
        RecurrenceType.DAILY,
        Set.of(),
        LocalDate.of(2026, 9, 1),
        null,
        LocalTime.of(21, 0),
        "Asia/Shanghai",
        ActionStatus.ACTIVE,
        0,
        NOW,
        NOW);
  }

  private static ActionOccurrence occurrence(long id, long actionId, OccurrenceStatus status) {
    ActionOccurrence occurrence =
        ActionOccurrence.schedule(
            id,
            actionId,
            Instant.parse("2026-09-05T13:00:00Z"),
            LocalDate.of(2026, 9, 5),
            "Asia/Shanghai",
            NOW);
    if (status == OccurrenceStatus.COMPLETED) {
      occurrence.checkIn(CheckInResultType.COMPLETED, NOW);
    }
    if (status == OccurrenceStatus.PARTIAL) {
      occurrence.checkIn(CheckInResultType.PARTIAL, NOW);
    }
    return occurrence;
  }
}
