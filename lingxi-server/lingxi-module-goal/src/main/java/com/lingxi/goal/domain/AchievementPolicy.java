package com.lingxi.goal.domain;

import com.lingxi.goal.api.AchievementType;
import com.lingxi.goal.api.GoalStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 成就授予规则。
 *
 * <p>只依据已落库的确定性领域事实判定，不依赖随机、排名或外部模型输出；同一输入重复执行得到相同结果，
 * 重复授予由引用键唯一约束兜底。
 */
public final class AchievementPolicy {
  private static final String GOAL_REFERENCE_PREFIX = "goal-completed:";
  private static final String MILESTONE_REFERENCE_PREFIX = "milestone-completed:";
  private static final String STREAK_REFERENCE_PREFIX = "check-in-streak:";

  private AchievementPolicy() {}

  /** 目标完成成就：目标达到成功标准并进入 COMPLETED 时授予。 */
  public static Optional<Achievement> goalCompleted(
      long achievementId, Goal goal, LocalDateTime now) {
    if (goal.getStatus() != GoalStatus.COMPLETED) {
      return Optional.empty();
    }
    return Optional.of(
        new Achievement(
            achievementId,
            goal.getUserId(),
            goal.getId(),
            AchievementType.GOAL_COMPLETED,
            "目标达成",
            "达成目标「" + goal.getTitle() + "」",
            GOAL_REFERENCE_PREFIX + goal.getId(),
            now,
            now));
  }

  /**
   * 里程碑完成成就：里程碑下未取消行动的全部已生成实例都已完成时授予。
   *
   * <p>只要存在未完成、部分完成、跳过或错过的实例，里程碑即视为未完成，避免提前发放成就。
   */
  public static Optional<Achievement> milestoneCompleted(
      long achievementId, MilestoneProgress progress, LocalDateTime now) {
    if (progress.actions().isEmpty() || progress.occurrences().isEmpty()) {
      return Optional.empty();
    }
    boolean allCompleted =
        progress.occurrences().stream()
            .allMatch(occurrence -> occurrence.getStatus() == OccurrenceStatus.COMPLETED);
    if (!allCompleted) {
      return Optional.empty();
    }
    Milestone milestone = progress.milestone();
    return Optional.of(
        new Achievement(
            achievementId,
            progress.userId(),
            milestoneGoalId(progress),
            AchievementType.MILESTONE_COMPLETED,
            "里程碑完成",
            "完成里程碑「" + milestone.title() + "」",
            MILESTONE_REFERENCE_PREFIX + milestone.id(),
            now,
            now));
  }

  /**
   * 连续打卡成就：最近阈值天数内每个自然日都完成过打卡，且恰好在本日首次达到阈值。
   *
   * <p>窗口外前一天同样已完成说明本次不是首次达标，不再重复授予；中断后重新连续达标会得到新的引用键。
   */
  public static Optional<Achievement> checkInStreak(
      long achievementId, long userId, CheckInStreak streak, LocalDateTime now) {
    LocalDate endDate = streak.checkInDate();
    LocalDate startDate = endDate.minusDays(streak.thresholdDays() - 1L);
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      if (!streak.completedDates().contains(date)) {
        return Optional.empty();
      }
    }
    if (streak.completedDates().contains(startDate.minusDays(1))) {
      return Optional.empty();
    }
    int thresholdDays = streak.thresholdDays();
    return Optional.of(
        new Achievement(
            achievementId,
            userId,
            null,
            AchievementType.CHECK_IN_STREAK,
            "连续打卡 " + thresholdDays + " 天",
            startDate + " 至 " + endDate + " 每天完成行动打卡",
            STREAK_REFERENCE_PREFIX + userId + ":" + thresholdDays + ":" + endDate,
            now,
            now));
  }

  private static long milestoneGoalId(MilestoneProgress progress) {
    List<Long> goalIds = progress.actions().stream().map(Action::goalId).distinct().toList();
    if (goalIds.size() != 1) {
      throw new IllegalStateException("里程碑下的行动必须属于同一目标");
    }
    return goalIds.get(0);
  }
}
