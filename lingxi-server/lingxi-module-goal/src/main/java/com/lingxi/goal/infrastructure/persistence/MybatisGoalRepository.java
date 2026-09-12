package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.goal.api.CheckInResultType;
import com.lingxi.goal.api.GoalStatus;
import com.lingxi.goal.api.RecurrenceType;
import com.lingxi.goal.domain.Action;
import com.lingxi.goal.domain.ActionOccurrence;
import com.lingxi.goal.domain.ActionStatus;
import com.lingxi.goal.domain.CheckIn;
import com.lingxi.goal.domain.Goal;
import com.lingxi.goal.domain.GoalRepository;
import com.lingxi.goal.domain.Milestone;
import com.lingxi.goal.domain.OccurrenceStatus;
import com.lingxi.goal.domain.PlanVersion;
import com.lingxi.goal.domain.PlanVersionStatus;
import com.lingxi.goal.domain.Review;
import com.lingxi.goal.domain.ReviewStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
  private final OccurrenceMapper occurrenceMapper;
  private final CheckInMapper checkInMapper;
  private final ReviewMapper reviewMapper;
  private final ObjectMapper objectMapper;

  public MybatisGoalRepository(
      GoalMapper goalMapper,
      PlanVersionMapper planMapper,
      MilestoneMapper milestoneMapper,
      ActionMapper actionMapper,
      OccurrenceMapper occurrenceMapper,
      CheckInMapper checkInMapper,
      ReviewMapper reviewMapper,
      ObjectMapper objectMapper) {
    this.goalMapper = goalMapper;
    this.planMapper = planMapper;
    this.milestoneMapper = milestoneMapper;
    this.actionMapper = actionMapper;
    this.occurrenceMapper = occurrenceMapper;
    this.checkInMapper = checkInMapper;
    this.reviewMapper = reviewMapper;
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
  public Optional<Action> findAction(long id) {
    return Optional.ofNullable(actionMapper.selectById(id)).map(this::toDomain);
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
        : occurrenceMapper.insertIgnoreBatch(occurrences.stream().map(this::toEntity).toList());
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

  private Goal toDomain(GoalEntity e) {
    return Goal.rehydrate(
        e.getId(),
        e.getPublicId(),
        e.getUserId(),
        e.getRequestKey(),
        e.getRequestDigest(),
        e.getTitle(),
        e.getSuccessCriteria(),
        GoalStatus.valueOf(e.getStatus()),
        e.getCurrentPlanVersionId(),
        e.getProgress(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
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
        RecurrenceType.valueOf(e.getRecurrenceType()),
        parseWeekdays(e.getWeekdaysJson()),
        e.getStartDate(),
        e.getEndDate(),
        e.getLocalTime(),
        e.getTimezone(),
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
        e.getNote(),
        e.getEvidenceReference(),
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
    e.setTitle(g.getTitle());
    e.setSuccessCriteria(g.getSuccessCriteria());
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
    e.setRecurrenceType(a.recurrenceType().name());
    e.setWeekdaysJson(writeWeekdays(a.weekdays()));
    e.setStartDate(a.startDate());
    e.setEndDate(a.endDate());
    e.setLocalTime(a.localTime());
    e.setTimezone(a.timezone());
    e.setStatus(a.status().name());
    e.setVersion(a.version());
    e.setCreatedAt(a.createdAt());
    e.setUpdatedAt(a.updatedAt());
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
    e.setNote(c.getNote());
    e.setEvidenceReference(c.getEvidenceReference());
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

  private String writeWeekdays(Set<DayOfWeek> values) {
    try {
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
