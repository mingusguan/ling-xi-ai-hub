package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.goal.api.ActionDifficulty;
import com.lingxi.goal.api.ActionExceptionType;
import com.lingxi.goal.api.CheckInResultType;
import com.lingxi.goal.api.EnergyLevel;
import com.lingxi.goal.api.MoodLevel;
import com.lingxi.goal.api.PriorityLevel;
import com.lingxi.goal.api.GoalClarificationStage;
import com.lingxi.goal.api.GoalPrivacyLevel;
import com.lingxi.goal.api.GoalStatus;
import com.lingxi.goal.api.GoalType;
import com.lingxi.goal.api.RecurrenceType;
import com.lingxi.goal.api.StarterFirstAction;
import com.lingxi.goal.api.StarterGoalTemplate;
import com.lingxi.goal.domain.Action;
import com.lingxi.goal.domain.ActionDetail;
import com.lingxi.goal.domain.ActionException;
import com.lingxi.goal.domain.ActionOccurrence;
import com.lingxi.goal.domain.ActionSchedule;
import com.lingxi.goal.domain.ActionStatus;
import com.lingxi.goal.domain.CheckIn;
import com.lingxi.goal.domain.CheckInDetail;
import com.lingxi.goal.domain.FocusSession;
import com.lingxi.goal.domain.FocusSessionStatus;
import com.lingxi.goal.domain.Goal;
import com.lingxi.goal.domain.GoalDefinition;
import com.lingxi.goal.domain.GoalRepository;
import com.lingxi.goal.domain.Milestone;
import com.lingxi.goal.domain.OccurrenceStatus;
import com.lingxi.goal.domain.PlanVersion;
import com.lingxi.goal.domain.PlanVersionStatus;
import com.lingxi.goal.domain.QuickNote;
import com.lingxi.goal.domain.Review;
import com.lingxi.goal.domain.ReviewStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/** 目标执行闭环的 MyBatis-Plus 仓储实现。 */
@Repository
public class MybatisGoalRepository implements GoalRepository {
  private final GoalMapper goalMapper;
  private final PlanVersionMapper planMapper;
  private final MilestoneMapper milestoneMapper;
  private final ActionMapper actionMapper;
  private final ActionExceptionMapper actionExceptionMapper;
  private final FocusSessionMapper focusSessionMapper;
  private final QuickNoteMapper quickNoteMapper;
  private final OccurrenceMapper occurrenceMapper;
  private final CheckInMapper checkInMapper;
  private final ReviewMapper reviewMapper;
  private final StarterGoalTemplateMapper starterTemplateMapper;
  private final ObjectMapper objectMapper;

  public MybatisGoalRepository(
      GoalMapper goalMapper,
      PlanVersionMapper planMapper,
      MilestoneMapper milestoneMapper,
      ActionMapper actionMapper,
      ActionExceptionMapper actionExceptionMapper,
      FocusSessionMapper focusSessionMapper,
      QuickNoteMapper quickNoteMapper,
      OccurrenceMapper occurrenceMapper,
      CheckInMapper checkInMapper,
      ReviewMapper reviewMapper,
      StarterGoalTemplateMapper starterTemplateMapper,
      ObjectMapper objectMapper) {
    this.goalMapper = goalMapper;
    this.planMapper = planMapper;
    this.milestoneMapper = milestoneMapper;
    this.actionMapper = actionMapper;
    this.actionExceptionMapper = actionExceptionMapper;
    this.focusSessionMapper = focusSessionMapper;
    this.quickNoteMapper = quickNoteMapper;
    this.occurrenceMapper = occurrenceMapper;
    this.checkInMapper = checkInMapper;
    this.reviewMapper = reviewMapper;
    this.starterTemplateMapper = starterTemplateMapper;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<Goal> findById(long id) {
    return Optional.ofNullable(goalMapper.selectById(id)).map(this::toDomain);
  }

  @Override
  public Optional<Goal> findByRequestKey(String key) {
    return Optional.ofNullable(
            goalMapper.selectOne(
                Wrappers.<GoalEntity>lambdaQuery()
                    .eq(GoalEntity::getRequestKey, key)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public List<Goal> findByUserId(long userId) {
    return goalMapper
        .selectList(
            Wrappers.<GoalEntity>lambdaQuery()
                .eq(GoalEntity::getUserId, userId)
                .orderByDesc(GoalEntity::getId))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Goal> findActiveGoals() {
    return goalMapper
        .selectList(
            Wrappers.<GoalEntity>lambdaQuery()
                .eq(GoalEntity::getStatus, GoalStatus.ACTIVE.name())
                .orderByAsc(GoalEntity::getId))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public Optional<PlanVersion> findPlanByRequestKey(String key) {
    return Optional.ofNullable(
            planMapper.selectOne(
                Wrappers.<PlanVersionEntity>lambdaQuery()
                    .eq(PlanVersionEntity::getRequestKey, key)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public Optional<PlanVersion> findPlanById(long id) {
    return Optional.ofNullable(planMapper.selectById(id)).map(this::toDomain);
  }

  @Override
  public int nextPlanVersionNo(long goalId) {
    return Math.toIntExact(
        planMapper.selectCount(
                Wrappers.<PlanVersionEntity>lambdaQuery().eq(PlanVersionEntity::getGoalId, goalId))
            + 1);
  }

  @Override
  public void insert(Goal goal) {
    goalMapper.insert(toEntity(goal));
  }

  @Override
  public void insertPlanBundle(PlanVersion plan, List<Milestone> milestones, List<Action> actions) {
    planMapper.insert(toEntity(plan));
    if (!milestones.isEmpty()) {
      milestoneMapper.insertBatch(milestones.stream().map(this::toEntity).toList());
    }
    if (!actions.isEmpty()) {
      actionMapper.insertBatch(actions.stream().map(this::toEntity).toList());
    }
  }

  @Override
  public void supersedeOtherPlans(long goalId, long activePlanId) {
    planMapper.update(
        null,
        Wrappers.<PlanVersionEntity>lambdaUpdate()
            .eq(PlanVersionEntity::getGoalId, goalId)
            .ne(PlanVersionEntity::getId, activePlanId)
            .eq(PlanVersionEntity::getStatus, PlanVersionStatus.ACTIVE.name())
            .set(PlanVersionEntity::getStatus, PlanVersionStatus.SUPERSEDED.name()));
    actionMapper.update(
        null,
        Wrappers.<ActionEntity>lambdaUpdate()
            .eq(ActionEntity::getGoalId, goalId)
            .ne(ActionEntity::getPlanVersionId, activePlanId)
            .eq(ActionEntity::getStatus, ActionStatus.ACTIVE.name())
            .set(ActionEntity::getStatus, ActionStatus.CANCELLED.name())
            .setSql("version = version + 1"));
  }

  @Override
  public boolean activatePlanVersion(PlanVersion plan, PlanVersionStatus expectedStatus) {
    return planMapper.update(
            null,
            Wrappers.<PlanVersionEntity>lambdaUpdate()
                .eq(PlanVersionEntity::getId, plan.id())
                .eq(PlanVersionEntity::getStatus, expectedStatus.name())
                .set(PlanVersionEntity::getStatus, plan.status().name())
                .set(PlanVersionEntity::getActivatedAt, plan.activatedAt()))
        == 1;
  }

  @Override
  public void activatePlanActions(long planVersionId) {
    actionMapper.update(
        null,
        Wrappers.<ActionEntity>lambdaUpdate()
            .eq(ActionEntity::getPlanVersionId, planVersionId)
            .eq(ActionEntity::getStatus, ActionStatus.DRAFT.name())
            .set(ActionEntity::getStatus, ActionStatus.ACTIVE.name())
            .setSql("version = version + 1"));
  }

  @Override
  public boolean updateActivatedPlan(Goal goal, long previous) {
    return goalMapper.update(
            null,
            Wrappers.<GoalEntity>lambdaUpdate()
                .eq(GoalEntity::getId, goal.getId())
                .eq(GoalEntity::getVersion, previous)
                .set(GoalEntity::getCurrentPlanVersionId, goal.getCurrentPlanVersionId())
                .set(GoalEntity::getStatus, goal.getStatus().name())
                .set(GoalEntity::getVersion, goal.getVersion())
                .set(GoalEntity::getUpdatedAt, goal.getUpdatedAt()))
        == 1;
  }

  @Override
  public boolean updateDefinitionAndState(Goal goal, long previous) {
    return goalMapper.update(
            null,
            Wrappers.<GoalEntity>lambdaUpdate()
                .eq(GoalEntity::getId, goal.getId())
                .eq(GoalEntity::getVersion, previous)
                .set(GoalEntity::getTitle, goal.getDefinition().title())
                .set(GoalEntity::getDescription, goal.getDefinition().description())
                .set(GoalEntity::getGoalType, goal.getDefinition().goalType().name())
                .set(GoalEntity::getSuccessCriteria, goal.getDefinition().successCriteria())
                .set(GoalEntity::getStartDate, goal.getDefinition().startDate())
                .set(GoalEntity::getTargetEndDate, goal.getDefinition().targetEndDate())
                .set(GoalEntity::getPriority, goal.getDefinition().priority().name())
                .set(
                    GoalEntity::getWeeklyAvailableMinutes,
                    goal.getDefinition().weeklyAvailableMinutes())
                .set(GoalEntity::getResourceConstraints, goal.getDefinition().resourceConstraints())
                .set(GoalEntity::getVerifiableOutcomes, goal.getDefinition().verifiableOutcomes())
                .set(GoalEntity::getPrivacyLevel, goal.getDefinition().privacyLevel().name())
                .set(GoalEntity::getStatus, goal.getStatus().name())
                .set(GoalEntity::getPauseResumeAt, goal.getPauseResumeAt())
                .set(GoalEntity::getAbandonReason, goal.getAbandonReason())
                .set(
                    GoalEntity::getClarificationStage,
                    goal.getClarificationStage() == null
                        ? null
                        : goal.getClarificationStage().name())
                .set(GoalEntity::getVersion, goal.getVersion())
                .set(GoalEntity::getUpdatedAt, goal.getUpdatedAt()))
        == 1;
  }

  @Override
  public int countActiveGoals(long userId, long excludeGoalId) {
    // 计数口径与 ActiveGoalQuota.countsAsActive 一致：只有进行中与等待确认计划的目标计入上限。
    return Math.toIntExact(
        goalMapper.selectCount(
            Wrappers.<GoalEntity>lambdaQuery()
                .eq(GoalEntity::getUserId, userId)
                .in(
                    GoalEntity::getStatus,
                    List.of(GoalStatus.ACTIVE.name(), GoalStatus.PENDING_CONFIRMATION.name()))
                .ne(excludeGoalId > 0, GoalEntity::getId, excludeGoalId)));
  }

  @Override
  public Optional<Action> findAction(long id) {
    return Optional.ofNullable(actionMapper.selectById(id)).map(this::toDomain);
  }

  @Override
  public void insertAction(Action action) {
    actionMapper.insert(toEntity(action));
  }

  @Override
  public boolean updateAction(Action action, long expected) {
    ActionEntity e = toEntity(action);
    // 传入 null 实体并显式 set，避免 @Version 与 wrapper 的 eq(version) 叠加成双重版本判定。
    return actionMapper.update(
            null,
            Wrappers.<ActionEntity>lambdaUpdate()
                .eq(ActionEntity::getId, action.id())
                .eq(ActionEntity::getVersion, expected)
                .set(ActionEntity::getTitle, e.getTitle())
                .set(ActionEntity::getDescription, e.getDescription())
                .set(ActionEntity::getPriority, e.getPriority())
                .set(ActionEntity::getDifficulty, e.getDifficulty())
                .set(ActionEntity::getCompletionCriteria, e.getCompletionCriteria())
                .set(ActionEntity::getEstimatedMinutes, e.getEstimatedMinutes())
                .set(ActionEntity::getRecurrenceType, e.getRecurrenceType())
                .set(ActionEntity::getWeekdaysJson, e.getWeekdaysJson())
                .set(ActionEntity::getIntervalDays, e.getIntervalDays())
                .set(ActionEntity::getStartDate, e.getStartDate())
                .set(ActionEntity::getEndDate, e.getEndDate())
                .set(ActionEntity::getLocalTime, e.getLocalTime())
                .set(ActionEntity::getEndLocalTime, e.getEndLocalTime())
                .set(ActionEntity::getPrerequisiteActionId, e.getPrerequisiteActionId())
                .set(ActionEntity::getReminderPolicy, e.getReminderPolicy())
                .set(ActionEntity::getTimezone, e.getTimezone())
                .set(ActionEntity::getStatus, e.getStatus())
                .set(ActionEntity::getVersion, e.getVersion())
                .set(ActionEntity::getUpdatedAt, e.getUpdatedAt()))
        == 1;
  }

  @Override
  public List<ActionException> findActionExceptions(List<Long> actionIds) {
    if (actionIds.isEmpty()) {
      return List.of();
    }
    return actionExceptionMapper
        .selectList(
            Wrappers.<ActionExceptionEntity>lambdaQuery()
                .in(ActionExceptionEntity::getActionId, actionIds)
                .orderByAsc(ActionExceptionEntity::getLocalDate))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public void upsertActionException(ActionException exception) {
    // 同一行动同一天只能有一条例外；重复调整同一天时按新决定覆盖，避免留下两条互相矛盾的记录。
    actionExceptionMapper.delete(
        Wrappers.<ActionExceptionEntity>lambdaQuery()
            .eq(ActionExceptionEntity::getActionId, exception.actionId())
            .eq(ActionExceptionEntity::getLocalDate, exception.localDate()));
    actionExceptionMapper.insert(toEntity(exception));
  }

  @Override
  public boolean deleteActionException(long actionId, LocalDate localDate) {
    return actionExceptionMapper.delete(
            Wrappers.<ActionExceptionEntity>lambdaQuery()
                .eq(ActionExceptionEntity::getActionId, actionId)
                .eq(ActionExceptionEntity::getLocalDate, localDate))
        == 1;
  }

  @Override
  public List<Action> findActionsByGoalIds(List<Long> goalIds) {
    if (goalIds.isEmpty()) {
      return List.of();
    }
    return actionMapper
        .selectList(
            Wrappers.<ActionEntity>lambdaQuery()
                .in(ActionEntity::getGoalId, goalIds)
                .eq(ActionEntity::getStatus, ActionStatus.ACTIVE.name()))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public Optional<ActionOccurrence> findOccurrence(long id) {
    return Optional.ofNullable(occurrenceMapper.selectById(id)).map(this::toDomain);
  }

  @Override
  public List<ActionOccurrence> findOccurrences(
      List<Long> actionIds, LocalDate from, LocalDate to) {
    if (actionIds.isEmpty()) {
      return List.of();
    }
    return occurrenceMapper
        .selectList(
            Wrappers.<OccurrenceEntity>lambdaQuery()
                .in(OccurrenceEntity::getActionId, actionIds)
                .between(OccurrenceEntity::getLocalDate, from, to)
                .orderByAsc(OccurrenceEntity::getScheduledAt))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public int insertOccurrences(List<ActionOccurrence> occurrences) {
    return occurrences.isEmpty()
        ? 0
        : occurrenceMapper.upsertBatch(occurrences.stream().map(this::toEntity).toList());
  }

  @Override
  public boolean updateOccurrence(ActionOccurrence occurrence, String expectedStatus) {
    return occurrenceMapper.update(
            null,
            Wrappers.<OccurrenceEntity>lambdaUpdate()
                .eq(OccurrenceEntity::getId, occurrence.getId())
                .eq(OccurrenceEntity::getStatus, expectedStatus)
                .set(OccurrenceEntity::getStatus, occurrence.getStatus().name())
                .set(OccurrenceEntity::getUpdatedAt, occurrence.getUpdatedAt()))
        == 1;
  }

  @Override
  public int deleteFutureScheduledOccurrences(long actionId, LocalDateTime after) {
    // 只删尚未执行的实例；已打卡或已跳过的实例属于历史事实，一律保留。
    // 边界截断到毫秒：DATETIME(3) 只存毫秒，超过毫秒精度的边界值会被数据库四舍五入。
    return occurrenceMapper.delete(
        Wrappers.<OccurrenceEntity>lambdaQuery()
            .eq(OccurrenceEntity::getActionId, actionId)
            .eq(OccurrenceEntity::getStatus, OccurrenceStatus.SCHEDULED.name())
            .gt(OccurrenceEntity::getScheduledAt, after.truncatedTo(ChronoUnit.MILLIS)));
  }

  @Override
  public boolean deleteScheduledOccurrenceAt(long actionId, LocalDateTime scheduledAt) {
    return occurrenceMapper.delete(
            Wrappers.<OccurrenceEntity>lambdaQuery()
                .eq(OccurrenceEntity::getActionId, actionId)
                .eq(OccurrenceEntity::getStatus, OccurrenceStatus.SCHEDULED.name())
                .eq(OccurrenceEntity::getScheduledAt, scheduledAt.truncatedTo(ChronoUnit.MILLIS)))
        == 1;
  }

  @Override
  public Optional<CheckIn> findCheckInByRequest(long occurrenceId, String key) {
    return Optional.ofNullable(
            checkInMapper.selectOne(
                Wrappers.<CheckInEntity>lambdaQuery()
                    .eq(CheckInEntity::getOccurrenceId, occurrenceId)
                    .eq(CheckInEntity::getRequestKey, key)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public Optional<CheckIn> findEffectiveCheckIn(long occurrenceId) {
    return Optional.ofNullable(
            checkInMapper.selectOne(
                Wrappers.<CheckInEntity>lambdaQuery()
                    .eq(CheckInEntity::getOccurrenceId, occurrenceId)
                    .eq(CheckInEntity::getEffectiveKey, 1)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public void supersedeCheckIn(long id) {
    checkInMapper.update(
        null,
        Wrappers.<CheckInEntity>lambdaUpdate()
            .eq(CheckInEntity::getId, id)
            .set(CheckInEntity::getEffective, false)
            .set(CheckInEntity::getEffectiveKey, null));
  }

  @Override
  public void insertCheckIn(CheckIn checkIn) {
    checkInMapper.insert(toEntity(checkIn));
  }

  @Override
  public int calculateProgress(long goalId, LocalDateTime asOf) {
    List<Action> actions = findActionsByGoalIds(List.of(goalId));
    if (actions.isEmpty()) {
      return 0;
    }
    List<Long> ids = actions.stream().map(Action::id).toList();
    List<OccurrenceEntity> occurrences =
        occurrenceMapper.selectList(
            Wrappers.<OccurrenceEntity>lambdaQuery()
                .in(OccurrenceEntity::getActionId, ids)
                .le(OccurrenceEntity::getScheduledAt, asOf));
    if (occurrences.isEmpty()) {
      return 0;
    }
    int points =
        occurrences.stream()
            .mapToInt(
                e ->
                    switch (OccurrenceStatus.valueOf(e.getStatus())) {
                      case COMPLETED -> 100;
                      case PARTIAL -> 50;
                      default -> 0;
                    })
            .sum();
    return points / occurrences.size();
  }

  @Override
  public boolean updateProgress(Goal goal, long previous) {
    return goalMapper.update(
            null,
            Wrappers.<GoalEntity>lambdaUpdate()
                .eq(GoalEntity::getId, goal.getId())
                .eq(GoalEntity::getVersion, previous)
                .set(GoalEntity::getProgress, goal.getProgress())
                .set(GoalEntity::getStatus, goal.getStatus().name())
                .set(GoalEntity::getVersion, goal.getVersion())
                .set(GoalEntity::getUpdatedAt, goal.getUpdatedAt()))
        == 1;
  }

  @Override
  public Optional<Review> findReview(long id) {
    return Optional.ofNullable(reviewMapper.selectById(id)).map(this::toDomain);
  }

  @Override
  public Optional<Review> findReviewByGoalAndPeriod(long goalId, String periodKey) {
    return Optional.ofNullable(
            reviewMapper.selectOne(
                Wrappers.<ReviewEntity>lambdaQuery()
                    .eq(ReviewEntity::getGoalId, goalId)
                    .eq(ReviewEntity::getPeriodKey, periodKey)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public int insertReviews(List<Review> reviews) {
    return reviews.isEmpty()
        ? 0
        : reviewMapper.insertIgnoreBatch(reviews.stream().map(this::toEntity).toList());
  }

  @Override
  public long countReviews(long userId, Long goalId) {
    List<Long> goalIds = ownedGoalIds(userId, goalId);
    if (goalIds.isEmpty()) {
      return 0;
    }
    return reviewMapper.selectCount(
        Wrappers.<ReviewEntity>lambdaQuery().in(ReviewEntity::getGoalId, goalIds));
  }

  @Override
  public List<Review> findReviewsByUser(long userId, Long goalId, int page, int pageSize) {
    List<Long> goalIds = ownedGoalIds(userId, goalId);
    if (goalIds.isEmpty()) {
      return List.of();
    }
    Page<ReviewEntity> result =
        reviewMapper.selectPage(
            Page.of(page, pageSize),
            Wrappers.<ReviewEntity>lambdaQuery()
                .in(ReviewEntity::getGoalId, goalIds)
                .orderByDesc(ReviewEntity::getPeriodKey)
                .orderByDesc(ReviewEntity::getId));
    return result.getRecords().stream().map(this::toDomain).toList();
  }

  /** 用户目标标识；传入 goalId 时先校验归属，避免跨用户读取复盘。 */
  private List<Long> ownedGoalIds(long userId, Long goalId) {
    if (goalId != null) {
      boolean owned =
          goalMapper.selectCount(
                  Wrappers.<GoalEntity>lambdaQuery()
                      .eq(GoalEntity::getId, goalId)
                      .eq(GoalEntity::getUserId, userId))
              > 0;
      return owned ? List.of(goalId) : List.of();
    }
    return goalMapper
        .selectList(
            Wrappers.<GoalEntity>lambdaQuery()
                .select(GoalEntity::getId)
                .eq(GoalEntity::getUserId, userId))
        .stream()
        .map(GoalEntity::getId)
        .toList();
  }

  @Override
  public boolean updateReview(Review r, long previous) {
    return reviewMapper.update(
            null,
            Wrappers.<ReviewEntity>lambdaUpdate()
                .eq(ReviewEntity::getId, r.getId())
                .eq(ReviewEntity::getVersion, previous)
                .set(ReviewEntity::getStatus, r.getStatus().name())
                .set(ReviewEntity::getConclusionJson, r.getConclusionJson())
                .set(ReviewEntity::getCompletionRequestKey, r.getCompletionRequestKey())
                .set(ReviewEntity::getCompletionRequestDigest, r.getCompletionRequestDigest())
                .set(ReviewEntity::getVersion, r.getVersion())
                .set(ReviewEntity::getCompletedAt, r.getCompletedAt()))
        == 1;
  }

  @Override
  public Optional<Milestone> findMilestone(long milestoneId) {
    return Optional.ofNullable(milestoneMapper.selectById(milestoneId)).map(this::toDomain);
  }

  @Override
  public Optional<Milestone> findMilestoneByPlanAndSequence(long planVersionId, int sequenceNo) {
    return Optional.ofNullable(
        milestoneMapper.selectOne(
            Wrappers.<MilestoneEntity>lambdaQuery()
                .eq(MilestoneEntity::getPlanVersionId, planVersionId)
                .eq(MilestoneEntity::getSequenceNo, sequenceNo)
                .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public List<Action> findActionsByMilestone(long milestoneId) {
    return actionMapper
        .selectList(
            Wrappers.<ActionEntity>lambdaQuery()
                .eq(ActionEntity::getMilestoneId, milestoneId)
                .orderByAsc(ActionEntity::getId))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<ActionOccurrence> findOccurrencesByActionIds(List<Long> actionIds) {
    if (actionIds.isEmpty()) {
      return List.of();
    }
    return occurrenceMapper
        .selectList(
            Wrappers.<OccurrenceEntity>lambdaQuery()
                .in(OccurrenceEntity::getActionId, actionIds)
                .orderByAsc(OccurrenceEntity::getScheduledAt))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<LocalDate> findCompletedCheckInDates(long userId, LocalDate fromDate, LocalDate toDate) {
    List<Long> goalIds =
        goalMapper
            .selectList(
                Wrappers.<GoalEntity>lambdaQuery()
                    .select(GoalEntity::getId)
                    .eq(GoalEntity::getUserId, userId))
            .stream()
            .map(GoalEntity::getId)
            .toList();
    if (goalIds.isEmpty()) {
      return List.of();
    }
    List<Long> actionIds =
        actionMapper
            .selectList(
                Wrappers.<ActionEntity>lambdaQuery()
                    .select(ActionEntity::getId)
                    .in(ActionEntity::getGoalId, goalIds)
                    .eq(ActionEntity::getStatus, ActionStatus.ACTIVE.name()))
            .stream()
            .map(ActionEntity::getId)
            .toList();
    if (actionIds.isEmpty()) {
      return List.of();
    }
    List<OccurrenceEntity> occurrences =
        occurrenceMapper.selectList(
            Wrappers.<OccurrenceEntity>lambdaQuery()
                .select(OccurrenceEntity::getId, OccurrenceEntity::getLocalDate)
                .in(OccurrenceEntity::getActionId, actionIds)
                .between(OccurrenceEntity::getLocalDate, fromDate, toDate));
    if (occurrences.isEmpty()) {
      return List.of();
    }
    List<Long> occurrenceIds = occurrences.stream().map(OccurrenceEntity::getId).toList();
    Set<Long> completedOccurrenceIds =
        checkInMapper
            .selectList(
                Wrappers.<CheckInEntity>lambdaQuery()
                    .select(CheckInEntity::getOccurrenceId)
                    .in(CheckInEntity::getOccurrenceId, occurrenceIds)
                    .eq(CheckInEntity::getEffectiveKey, 1)
                    .eq(CheckInEntity::getResult, CheckInResultType.COMPLETED.name()))
            .stream()
            .map(CheckInEntity::getOccurrenceId)
            .collect(Collectors.toSet());
    return occurrences.stream()
        .filter(occurrence -> completedOccurrenceIds.contains(occurrence.getId()))
        .map(OccurrenceEntity::getLocalDate)
        .distinct()
        .sorted()
        .toList();
  }

  @Override
  public int logicallyDeleteUserData(long userId) {
    int affectedRows = 0;
    List<Long> goalIds =
        goalMapper
            .selectList(
                Wrappers.<GoalEntity>lambdaQuery()
                    .select(GoalEntity::getId)
                    .eq(GoalEntity::getUserId, userId))
            .stream()
            .map(GoalEntity::getId)
            .toList();
    if (goalIds.isEmpty()) {
      return 0;
    }
    List<Long> actionIds =
        actionMapper
            .selectList(
                Wrappers.<ActionEntity>lambdaQuery()
                    .select(ActionEntity::getId)
                    .in(ActionEntity::getGoalId, goalIds))
            .stream()
            .map(ActionEntity::getId)
            .toList();
    if (!actionIds.isEmpty()) {
      List<Long> occurrenceIds =
          occurrenceMapper
              .selectList(
                  Wrappers.<OccurrenceEntity>lambdaQuery()
                      .select(OccurrenceEntity::getId)
                      .in(OccurrenceEntity::getActionId, actionIds))
              .stream()
              .map(OccurrenceEntity::getId)
              .toList();
      if (!occurrenceIds.isEmpty()) {
        affectedRows += checkInMapper.delete(
            Wrappers.<CheckInEntity>lambdaQuery()
                .in(CheckInEntity::getOccurrenceId, occurrenceIds));
      }
      affectedRows += occurrenceMapper.delete(
          Wrappers.<OccurrenceEntity>lambdaQuery().in(OccurrenceEntity::getActionId, actionIds));
    }
    affectedRows +=
        actionMapper.delete(
            Wrappers.<ActionEntity>lambdaQuery().in(ActionEntity::getGoalId, goalIds));
    List<Long> planIds =
        planMapper
            .selectList(
                Wrappers.<PlanVersionEntity>lambdaQuery()
                    .select(PlanVersionEntity::getId)
                    .in(PlanVersionEntity::getGoalId, goalIds))
            .stream()
            .map(PlanVersionEntity::getId)
            .toList();
    if (!planIds.isEmpty()) {
      affectedRows += milestoneMapper.delete(
          Wrappers.<MilestoneEntity>lambdaQuery().in(MilestoneEntity::getPlanVersionId, planIds));
    }
    affectedRows +=
        reviewMapper.delete(
            Wrappers.<ReviewEntity>lambdaQuery().in(ReviewEntity::getGoalId, goalIds));
    affectedRows += planMapper.delete(
        Wrappers.<PlanVersionEntity>lambdaQuery().in(PlanVersionEntity::getGoalId, goalIds));
    affectedRows +=
        goalMapper.delete(Wrappers.<GoalEntity>lambdaQuery().in(GoalEntity::getId, goalIds));
    return affectedRows;
  }

  @Override
  public Optional<FocusSession> findActiveFocusSession(long userId) {
    return Optional.ofNullable(
            focusSessionMapper.selectOne(
                Wrappers.<FocusSessionEntity>lambdaQuery()
                    .eq(FocusSessionEntity::getUserId, userId)
                    .eq(FocusSessionEntity::getActiveKey, 1)
                    .last("LIMIT 1")))
        .map(this::toDomain);
  }

  @Override
  public Optional<FocusSession> findFocusSession(long sessionId) {
    return Optional.ofNullable(focusSessionMapper.selectById(sessionId)).map(this::toDomain);
  }

  @Override
  public void insertFocusSession(FocusSession session) {
    focusSessionMapper.insert(toEntity(session));
  }

  @Override
  public boolean updateFocusSession(FocusSession session, long expectedVersion) {
    FocusSessionEntity e = toEntity(session);
    // 与服务端其它乐观更新一致：传 null 实体并显式 set，避免 @Version 与 wrapper 叠加成双重版本判定。
    return focusSessionMapper.update(
            null,
            Wrappers.<FocusSessionEntity>lambdaUpdate()
                .eq(FocusSessionEntity::getId, session.getId())
                .eq(FocusSessionEntity::getVersion, expectedVersion)
                .set(FocusSessionEntity::getStatus, e.getStatus())
                .set(FocusSessionEntity::getAccumulatedSeconds, e.getAccumulatedSeconds())
                .set(FocusSessionEntity::getLastResumedAt, e.getLastResumedAt())
                .set(FocusSessionEntity::getEndedAt, e.getEndedAt())
                .set(FocusSessionEntity::getNote, e.getNote())
                .set(FocusSessionEntity::getActiveKey, e.getActiveKey())
                .set(FocusSessionEntity::getVersion, e.getVersion())
                .set(FocusSessionEntity::getUpdatedAt, e.getUpdatedAt()))
        == 1;
  }

  @Override
  public void insertQuickNote(QuickNote note) {
    quickNoteMapper.insert(toEntity(note));
  }

  @Override
  public List<QuickNote> findQuickNotes(long userId, int limit) {
    return quickNoteMapper
        .selectList(
            Wrappers.<QuickNoteEntity>lambdaQuery()
                .eq(QuickNoteEntity::getUserId, userId)
                .orderByDesc(QuickNoteEntity::getCreatedAt)
                .orderByDesc(QuickNoteEntity::getId)
                .last("LIMIT " + Math.max(1, limit)))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<FocusSession> findFocusSessionsSince(long userId, java.time.Instant since) {
    return focusSessionMapper
        .selectList(
            Wrappers.<FocusSessionEntity>lambdaQuery()
                .eq(FocusSessionEntity::getUserId, userId)
                .ge(FocusSessionEntity::getStartedAt, LocalDateTime.ofInstant(since, ZoneOffset.UTC))
                .orderByAsc(FocusSessionEntity::getStartedAt))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public boolean deleteQuickNote(long noteId) {
    // @TableLogic 会把 delete 改写成标记 deleted=1，历史数据不会被物理清除。
    return quickNoteMapper.deleteById(noteId) == 1;
  }

  @Override
  public List<StarterGoalTemplate> findStarterTemplates() {
    return starterTemplateMapper
        .selectList(
            Wrappers.<StarterGoalTemplateEntity>lambdaQuery()
                .eq(StarterGoalTemplateEntity::getEnabled, true)
                .orderByAsc(StarterGoalTemplateEntity::getDisplayOrder))
        .stream()
        .map(this::toStarterTemplate)
        .toList();
  }

  @Override
  public Optional<StarterGoalTemplate> findStarterTemplate(String templateKey) {
    if (templateKey == null || templateKey.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(
            starterTemplateMapper.selectOne(
                Wrappers.<StarterGoalTemplateEntity>lambdaQuery()
                    .eq(StarterGoalTemplateEntity::getTemplateKey, templateKey)
                    .eq(StarterGoalTemplateEntity::getEnabled, true)
                    .last("LIMIT 1")))
        .map(this::toStarterTemplate);
  }

  /**
   * 由持久化对象还原入门模板。
   *
   * <p>首行动候选是迁移种入的 JSON，解析失败说明模板数据写坏了：这里直接抛错而不是
   * 返回空列表，否则用户会看到一个「没有首行动」的模板却查不出原因。
   */
  private StarterGoalTemplate toStarterTemplate(StarterGoalTemplateEntity e) {
    try {
      List<StarterFirstAction> firstActions =
          objectMapper.readValue(
              e.getFirstActionsJson(), new TypeReference<List<StarterFirstAction>>() {});
      List<String> tags =
          e.getTagsJson() == null || e.getTagsJson().isBlank()
              ? List.of()
              : objectMapper.readValue(e.getTagsJson(), new TypeReference<List<String>>() {});
      return new StarterGoalTemplate(
          e.getTemplateKey(),
          e.getName(),
          e.getSummary(),
          GoalType.valueOf(e.getGoalType()),
          e.getDefaultSuccessCriteria(),
          tags,
          firstActions);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("入门模板数据损坏：" + e.getTemplateKey(), ex);
    }
  }

  private FocusSession toDomain(FocusSessionEntity e) {
    return FocusSession.rehydrate(
        e.getId(),
        e.getUserId(),
        e.getOccurrenceId(),
        e.getActionId(),
        e.getPlannedMinutes(),
        FocusSessionStatus.valueOf(e.getStatus()),
        e.getAccumulatedSeconds() == null ? 0 : e.getAccumulatedSeconds(),
        e.getLastResumedAt() == null ? null : e.getLastResumedAt().toInstant(ZoneOffset.UTC),
        e.getStartedAt().toInstant(ZoneOffset.UTC),
        e.getEndedAt() == null ? null : e.getEndedAt().toInstant(ZoneOffset.UTC),
        e.getNote(),
        e.getVersion() == null ? 0 : e.getVersion(),
        e.getCreatedAt() == null ? null : e.getCreatedAt().toInstant(ZoneOffset.UTC),
        e.getUpdatedAt() == null ? null : e.getUpdatedAt().toInstant(ZoneOffset.UTC));
  }

  private FocusSessionEntity toEntity(FocusSession s) {
    FocusSessionEntity e = new FocusSessionEntity();
    e.setId(s.getId());
    e.setUserId(s.getUserId());
    e.setOccurrenceId(s.getOccurrenceId());
    e.setActionId(s.getActionId());
    e.setStatus(s.getStatus().name());
    e.setPlannedMinutes(s.getPlannedMinutes());
    e.setAccumulatedSeconds(s.getAccumulatedSeconds());
    e.setLastResumedAt(
        s.getLastResumedAt() == null
            ? null
            : LocalDateTime.ofInstant(s.getLastResumedAt(), ZoneOffset.UTC));
    e.setStartedAt(LocalDateTime.ofInstant(s.getStartedAt(), ZoneOffset.UTC));
    e.setEndedAt(
        s.getEndedAt() == null ? null : LocalDateTime.ofInstant(s.getEndedAt(), ZoneOffset.UTC));
    e.setNote(s.getNote());
    e.setActiveKey(s.activeKey());
    e.setVersion(s.getVersion());
    // created_at / updated_at 在表上是 NOT NULL 且无默认值：聚合必须自带这两个时间戳，
    // 否则 MyBatis-Plus 生成的 INSERT 会漏列，写入直接被数据库拒绝。
    e.setCreatedAt(s.getCreatedAt() == null ? null : LocalDateTime.ofInstant(s.getCreatedAt(), ZoneOffset.UTC));
    e.setUpdatedAt(s.getUpdatedAt() == null ? null : LocalDateTime.ofInstant(s.getUpdatedAt(), ZoneOffset.UTC));
    return e;
  }

  private QuickNote toDomain(QuickNoteEntity e) {
    return new QuickNote(
        e.getId(),
        e.getUserId(),
        e.getContent(),
        e.getMoodLevel() == null ? null : MoodLevel.valueOf(e.getMoodLevel()),
        e.getLocalDate(),
        e.getCreatedAt());
  }

  private QuickNoteEntity toEntity(QuickNote n) {
    QuickNoteEntity e = new QuickNoteEntity();
    e.setId(n.id());
    e.setUserId(n.userId());
    e.setContent(n.content());
    e.setMoodLevel(n.moodLevel() == null ? null : n.moodLevel().name());
    e.setLocalDate(n.localDate());
    e.setCreatedAt(n.createdAt());
    e.setUpdatedAt(n.createdAt());
    return e;
  }

  private Goal toDomain(GoalEntity e) {
    return Goal.rehydrate(
        e.getId(),
        e.getPublicId(),
        e.getUserId(),
        e.getRequestKey(),
        e.getRequestDigest(),
        toDefinition(e),
        GoalStatus.valueOf(e.getStatus()),
        e.getCurrentPlanVersionId(),
        e.getProgress(),
        e.getPauseResumeAt(),
        e.getAbandonReason(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getClarificationStage() == null
            ? null
            : GoalClarificationStage.valueOf(e.getClarificationStage()));
  }

  /** 由持久化对象还原目标定义值对象；目标类型与优先级列在迁移中已补默认值，不会为空。 */
  private GoalDefinition toDefinition(GoalEntity e) {
    return new GoalDefinition(
        e.getTitle(),
        e.getDescription(),
        e.getSuccessCriteria(),
        GoalType.valueOf(e.getGoalType()),
        e.getStartDate(),
        e.getTargetEndDate(),
        PriorityLevel.valueOf(e.getPriority()),
        e.getWeeklyAvailableMinutes(),
        e.getResourceConstraints(),
        e.getVerifiableOutcomes(),
        GoalPrivacyLevel.valueOf(e.getPrivacyLevel()));
  }

  private PlanVersion toDomain(PlanVersionEntity e) {
    return new PlanVersion(
        e.getId(),
        e.getGoalId(),
        e.getVersionNo(),
        e.getRequestKey(),
        e.getRequestDigest(),
        e.getSnapshotJson(),
        e.getAdjustmentReason(),
        PlanVersionStatus.valueOf(e.getStatus()),
        e.getSource(),
        e.getActivatedAt(),
        e.getCreatedAt());
  }

  private Action toDomain(ActionEntity e) {
    return new Action(
        e.getId(),
        e.getGoalId(),
        e.getPlanVersionId(),
        e.getMilestoneId(),
        e.getClientKey(),
        e.getTitle(),
        new ActionSchedule(
            RecurrenceType.valueOf(e.getRecurrenceType()),
            parseWeekdays(e.getWeekdaysJson()),
            e.getIntervalDays(),
            e.getStartDate(),
            e.getEndDate(),
            e.getLocalTime(),
            e.getEndLocalTime(),
            e.getTimezone()),
        new ActionDetail(
            e.getDescription(),
            PriorityLevel.valueOf(e.getPriority()),
            ActionDifficulty.valueOf(e.getDifficulty()),
            e.getCompletionCriteria(),
            e.getEstimatedMinutes(),
            e.getPrerequisiteActionId(),
            e.getReminderPolicy(),
            Boolean.TRUE.equals(e.getIsFirst())),
        ActionStatus.valueOf(e.getStatus()),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private Milestone toDomain(MilestoneEntity e) {
    return new Milestone(
        e.getId(),
        e.getPlanVersionId(),
        e.getSequenceNo(),
        e.getTitle(),
        e.getSuccessCriteria(),
        e.getCreatedAt());
  }

  private ActionOccurrence toDomain(OccurrenceEntity e) {
    return ActionOccurrence.rehydrate(
        e.getId(),
        e.getActionId(),
        e.getScheduledAt().toInstant(ZoneOffset.UTC),
        e.getLocalDate(),
        e.getTimezone(),
        OccurrenceStatus.valueOf(e.getStatus()),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private CheckIn toDomain(CheckInEntity e) {
    return CheckIn.rehydrate(
        e.getId(),
        e.getOccurrenceId(),
        e.getUserId(),
        e.getRequestKey(),
        e.getRequestDigest(),
        CheckInResultType.valueOf(e.getResult()),
        new CheckInDetail(
            e.getNote(),
            e.getEvidenceReference(),
            e.getActualMinutes(),
            e.getPerceivedDifficulty() == null ? null : ActionDifficulty.valueOf(e.getPerceivedDifficulty()),
            e.getEnergyLevel() == null ? null : EnergyLevel.valueOf(e.getEnergyLevel()),
            e.getMoodLevel() == null ? null : MoodLevel.valueOf(e.getMoodLevel()),
            e.getFailureReason()),
        Boolean.TRUE.equals(e.getEffective()),
        e.getRecordedAt(),
        e.getCreatedAt());
  }

  private Review toDomain(ReviewEntity e) {
    return Review.rehydrate(
        e.getId(),
        e.getGoalId(),
        e.getPeriodKey(),
        e.getInputSnapshotJson(),
        ReviewStatus.valueOf(e.getStatus()),
        e.getConclusionJson(),
        e.getCompletionRequestKey(),
        e.getCompletionRequestDigest(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getCompletedAt());
  }

  private GoalEntity toEntity(Goal g) {
    GoalEntity e = new GoalEntity();
    e.setId(g.getId());
    e.setPublicId(g.getPublicId());
    e.setUserId(g.getUserId());
    e.setRequestKey(g.getRequestKey());
    e.setRequestDigest(g.getRequestDigest());
    e.setTitle(g.getDefinition().title());
    e.setDescription(g.getDefinition().description());
    e.setGoalType(g.getDefinition().goalType().name());
    e.setSuccessCriteria(g.getDefinition().successCriteria());
    e.setStartDate(g.getDefinition().startDate());
    e.setTargetEndDate(g.getDefinition().targetEndDate());
    e.setPriority(g.getDefinition().priority().name());
    e.setWeeklyAvailableMinutes(g.getDefinition().weeklyAvailableMinutes());
    e.setResourceConstraints(g.getDefinition().resourceConstraints());
    e.setVerifiableOutcomes(g.getDefinition().verifiableOutcomes());
    e.setPrivacyLevel(g.getDefinition().privacyLevel().name());
    e.setPauseResumeAt(g.getPauseResumeAt());
    e.setAbandonReason(g.getAbandonReason());
    e.setClarificationStage(
        g.getClarificationStage() == null ? null : g.getClarificationStage().name());
    e.setStatus(g.getStatus().name());
    e.setCurrentPlanVersionId(g.getCurrentPlanVersionId());
    e.setProgress(g.getProgress());
    e.setVersion(g.getVersion());
    e.setCreatedAt(g.getCreatedAt());
    e.setUpdatedAt(g.getUpdatedAt());
    return e;
  }

  private PlanVersionEntity toEntity(PlanVersion p) {
    PlanVersionEntity e = new PlanVersionEntity();
    e.setId(p.id());
    e.setGoalId(p.goalId());
    e.setVersionNo(p.versionNo());
    e.setRequestKey(p.requestKey());
    e.setRequestDigest(p.requestDigest());
    e.setSnapshotJson(p.snapshotJson());
    e.setAdjustmentReason(p.adjustmentReason());
    e.setStatus(p.status().name());
    e.setSource(p.source());
    e.setActivatedAt(p.activatedAt());
    e.setCreatedAt(p.createdAt());
    return e;
  }

  private MilestoneEntity toEntity(Milestone m) {
    MilestoneEntity e = new MilestoneEntity();
    e.setId(m.id());
    e.setPlanVersionId(m.planVersionId());
    e.setSequenceNo(m.sequenceNo());
    e.setTitle(m.title());
    e.setSuccessCriteria(m.successCriteria());
    e.setCreatedAt(m.createdAt());
    return e;
  }

  private ActionEntity toEntity(Action a) {
    ActionEntity e = new ActionEntity();
    e.setId(a.id());
    e.setGoalId(a.goalId());
    e.setPlanVersionId(a.planVersionId());
    e.setMilestoneId(a.milestoneId());
    e.setClientKey(a.clientKey());
    e.setTitle(a.title());
    e.setDescription(a.detail().description());
    e.setPriority(a.detail().priority().name());
    e.setDifficulty(a.detail().difficulty().name());
    e.setCompletionCriteria(a.detail().completionCriteria());
    e.setEstimatedMinutes(a.detail().estimatedMinutes());
    e.setRecurrenceType(a.recurrenceType().name());
    e.setWeekdaysJson(writeWeekdays(a.weekdays()));
    e.setIntervalDays(a.intervalDays());
    e.setStartDate(a.startDate());
    e.setEndDate(a.endDate());
    e.setLocalTime(a.localTime());
    e.setEndLocalTime(a.endLocalTime());
    e.setPrerequisiteActionId(a.detail().prerequisiteActionId());
    e.setReminderPolicy(a.detail().reminderPolicy());
    e.setTimezone(a.timezone());
    e.setStatus(a.status().name());
    e.setIsFirst(a.detail().first());
    e.setVersion(a.version());
    e.setCreatedAt(a.createdAt());
    e.setUpdatedAt(a.updatedAt());
    return e;
  }

  private ActionException toDomain(ActionExceptionEntity e) {
    return new ActionException(
        e.getId(),
        e.getActionId(),
        e.getLocalDate(),
        ActionExceptionType.valueOf(e.getExceptionType()),
        e.getRescheduledDate(),
        e.getReason(),
        e.getCreatedAt());
  }

  private ActionExceptionEntity toEntity(ActionException x) {
    ActionExceptionEntity e = new ActionExceptionEntity();
    e.setId(x.id());
    e.setActionId(x.actionId());
    e.setLocalDate(x.localDate());
    e.setExceptionType(x.type().name());
    e.setRescheduledDate(x.rescheduledDate());
    e.setReason(x.reason());
    e.setCreatedAt(x.createdAt());
    return e;
  }

  private OccurrenceEntity toEntity(ActionOccurrence o) {
    OccurrenceEntity e = new OccurrenceEntity();
    e.setId(o.getId());
    e.setActionId(o.getActionId());
    e.setScheduledAt(java.time.LocalDateTime.ofInstant(o.getScheduledAt(), ZoneOffset.UTC));
    e.setLocalDate(o.getLocalDate());
    e.setTimezone(o.getTimezone());
    e.setStatus(o.getStatus().name());
    e.setCreatedAt(o.getCreatedAt());
    e.setUpdatedAt(o.getUpdatedAt());
    return e;
  }

  private CheckInEntity toEntity(CheckIn c) {
    CheckInEntity e = new CheckInEntity();
    e.setId(c.getId());
    e.setOccurrenceId(c.getOccurrenceId());
    e.setUserId(c.getUserId());
    e.setRequestKey(c.getRequestKey());
    e.setRequestDigest(c.getRequestDigest());
    e.setResult(c.getResult().name());
    CheckInDetail detail = c.getDetail();
    e.setNote(detail.note());
    e.setEvidenceReference(detail.evidenceReference());
    e.setActualMinutes(detail.actualMinutes());
    e.setPerceivedDifficulty(
        detail.perceivedDifficulty() == null ? null : detail.perceivedDifficulty().name());
    e.setEnergyLevel(detail.energyLevel() == null ? null : detail.energyLevel().name());
    e.setMoodLevel(detail.moodLevel() == null ? null : detail.moodLevel().name());
    e.setFailureReason(detail.failureReason());
    e.setEffective(c.isEffective());
    e.setEffectiveKey(c.isEffective() ? 1 : null);
    e.setRecordedAt(c.getRecordedAt());
    e.setCreatedAt(c.getCreatedAt());
    return e;
  }

  private ReviewEntity toEntity(Review r) {
    ReviewEntity e = new ReviewEntity();
    e.setId(r.getId());
    e.setGoalId(r.getGoalId());
    e.setPeriodKey(r.getPeriodKey());
    e.setInputSnapshotJson(r.getInputSnapshotJson());
    e.setStatus(r.getStatus().name());
    e.setConclusionJson(r.getConclusionJson());
    e.setCompletionRequestKey(r.getCompletionRequestKey());
    e.setCompletionRequestDigest(r.getCompletionRequestDigest());
    e.setVersion(r.getVersion());
    e.setCreatedAt(r.getCreatedAt());
    e.setCompletedAt(r.getCompletedAt());
    return e;
  }

  private String writeWeekdays(Set<DayOfWeek> values) {    try {
      return objectMapper.writeValueAsString(values);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private Set<DayOfWeek> parseWeekdays(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<Set<DayOfWeek>>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
