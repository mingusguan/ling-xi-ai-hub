package com.lingxi.goal.application;

import com.lingxi.goal.api.*;
import com.lingxi.goal.domain.*;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.DomainEventPublisher;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.kernel.PageResult;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成就用例：在目标执行事务内授予确定性成就，并提供本人成就查询。
 *
 * <p>授予逻辑与调用方共用同一个本地事务，保证打卡事实与成就事实同时生效；重复授予由引用键唯一约束兜底。
 */
@Service
public class AchievementApplicationService implements AchievementFacade {
  /** 成就查询允许的最大页大小。 */
  private static final int MAX_PAGE_SIZE = 200;

  private final GoalRepository goalRepository;
  private final AchievementRepository achievementRepository;
  private final IdGenerator idGenerator;
  private final DomainEventPublisher eventPublisher;
  private final int checkInStreakDays;

  public AchievementApplicationService(
      GoalRepository goalRepository,
      AchievementRepository achievementRepository,
      IdGenerator idGenerator,
      DomainEventPublisher eventPublisher,
      @Value("${lingxi.goal.achievement.check-in-streak-days:7}") int checkInStreakDays) {
    if (checkInStreakDays < 2) {
      throw new IllegalStateException("连续打卡成就阈值必须大于 1 天");
    }
    this.goalRepository = goalRepository;
    this.achievementRepository = achievementRepository;
    this.idGenerator = idGenerator;
    this.eventPublisher = eventPublisher;
    this.checkInStreakDays = checkInStreakDays;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<AchievementResult> listAchievements(
      long userId, Long goalId, int page, int pageSize) {
    if (userId <= 0 || page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw new BusinessException("GOAL_INVALID_QUERY", "成就查询参数不合法");
    }
    long total = achievementRepository.countByUser(userId, goalId);
    List<AchievementResult> items =
        achievementRepository.findByUser(userId, goalId, page, pageSize).stream()
            .map(this::view)
            .toList();
    return new PageResult<>(items, total, page, pageSize);
  }

  /**
   * 打卡事务内评估并授予成就，返回本次新获得的成就。
   *
   * <p>未知目标状态的重复打卡请求不会再次授予，历史成就由成就查询接口返回。
   */
  @Transactional
  public List<AchievementResult> grantForCheckIn(
      CheckInAchievementContext context, LocalDateTime now) {
    List<Achievement> granted = new ArrayList<>();
    if (context.statusBefore() != GoalStatus.COMPLETED) {
      AchievementPolicy.goalCompleted(idGenerator.nextId(), context.goal(), now)
          .ifPresent(achievement -> grant(achievement, context.goal().getVersion(), granted));
    }
    milestoneAchievement(context, now)
        .ifPresent(achievement -> grant(achievement, context.goal().getVersion(), granted));
    streakAchievement(context, now)
        .ifPresent(achievement -> grant(achievement, context.goal().getVersion(), granted));
    return granted.stream().map(this::view).toList();
  }

  /** 里程碑成就：只在打卡行动属于里程碑时判定该里程碑是否全部完成。 */
  private Optional<Achievement> milestoneAchievement(
      CheckInAchievementContext context, LocalDateTime now) {
    Long milestoneId = context.action().milestoneId();
    if (milestoneId == null) {
      return Optional.empty();
    }
    Milestone milestone = goalRepository.findMilestone(milestoneId).orElse(null);
    if (milestone == null) {
      return Optional.empty();
    }
    List<Action> actions =
        goalRepository.findActionsByMilestone(milestoneId).stream()
            .filter(action -> action.status() != ActionStatus.CANCELLED)
            .toList();
    if (actions.isEmpty()) {
      return Optional.empty();
    }
    List<Long> actionIds = actions.stream().map(Action::id).toList();
    MilestoneProgress progress =
        new MilestoneProgress(
            context.goal().getUserId(),
            milestone,
            actions,
            goalRepository.findOccurrencesByActionIds(actionIds));
    return AchievementPolicy.milestoneCompleted(idGenerator.nextId(), progress, now);
  }

  /** 连续打卡成就：按本次打卡的本地日期和最近阈值窗口内的完成情况判定。 */
  private Optional<Achievement> streakAchievement(
      CheckInAchievementContext context, LocalDateTime now) {
    if (context.checkInResult() != CheckInResultType.COMPLETED) {
      return Optional.empty();
    }
    long userId = context.goal().getUserId();
    LocalDate checkInDate = context.occurrence().getLocalDate();
    LocalDate windowStart = checkInDate.minusDays(checkInStreakDays - 1L);
    // 多取一天用于识别“首次达到阈值”，避免连续多日重复授予同一个连续纪录。
    Set<LocalDate> completedDates =
        new HashSet<>(
            goalRepository.findCompletedCheckInDates(
                userId, windowStart.minusDays(1), checkInDate));
    CheckInStreak streak = new CheckInStreak(checkInDate, completedDates, checkInStreakDays);
    return AchievementPolicy.checkInStreak(idGenerator.nextId(), userId, streak, now);
  }

  private void grant(Achievement achievement, long goalVersion, List<Achievement> granted) {
    if (!achievementRepository.insertIfAbsent(achievement)) {
      return;
    }
    granted.add(achievement);
    eventPublisher.publish(
        new AchievementEarnedEvent(
            idGenerator.nextEventId(),
            achievement.id(),
            achievement.userId(),
            achievement.goalId(),
            achievement.type(),
            achievement.goalId() == null ? 0 : goalVersion,
            achievement.achievedAt().toInstant(ZoneOffset.UTC)));
  }

  private AchievementResult view(Achievement achievement) {
    return new AchievementResult(
        achievement.id(),
        achievement.goalId(),
        achievement.type(),
        achievement.title(),
        achievement.description(),
        achievement.achievedAt().toInstant(ZoneOffset.UTC));
  }
}
