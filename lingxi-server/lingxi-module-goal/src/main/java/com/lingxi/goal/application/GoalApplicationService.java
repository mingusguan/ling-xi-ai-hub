package com.lingxi.goal.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.goal.api.*;
import com.lingxi.goal.domain.*;
import com.lingxi.identity.api.AccessProfile;
import com.lingxi.identity.api.IdentityFacade;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.DomainEventPublisher;
import com.lingxi.kernel.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 目标、计划、行动、打卡和复盘应用服务。 */
@Service
public class GoalApplicationService implements GoalFacade {
  private final GoalRepository repository;
  private final IdentityFacade identityFacade;
  private final IdGenerator idGenerator;
  private final DomainEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public GoalApplicationService(
      GoalRepository repository,
      IdentityFacade identityFacade,
      IdGenerator idGenerator,
      DomainEventPublisher eventPublisher,
      ObjectMapper objectMapper) {
    this(repository, identityFacade, idGenerator, eventPublisher, objectMapper, Clock.systemUTC());
  }

  GoalApplicationService(
      GoalRepository repository,
      IdentityFacade identityFacade,
      IdGenerator idGenerator,
      DomainEventPublisher eventPublisher,
      ObjectMapper objectMapper,
      Clock clock) {
    this.repository = repository;
    this.identityFacade = identityFacade;
    this.idGenerator = idGenerator;
    this.eventPublisher = eventPublisher;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  @Transactional
  public GoalResult createGoal(CreateGoalCommand command) {
    validate(command);
    requireCoreAccess(command.userId());
    String requestDigest = digest(command.title().trim() + "|" + command.successCriteria().trim());
    Goal existed = repository.findByRequestKey(command.requestKey()).orElse(null);
    if (existed != null) {
      if (!existed.getRequestDigest().equals(requestDigest)) {
        throw new BusinessException("GOAL_IDEMPOTENCY_CONFLICT", "同一请求不能创建不同目标");
      }
      existed.assertOwnedBy(command.userId());
      return result(existed);
    }
    Instant instant = clock.instant();
    LocalDateTime now = utc(instant);
    Goal goal =
        Goal.create(
            idGenerator.nextId(),
            idGenerator.nextPublicId(),
            command.userId(),
            command.requestKey(),
            requestDigest,
            command.title(),
            command.successCriteria(),
            now);
    repository.insert(goal);
    eventPublisher.publish(
        new GoalCreatedEvent(
            idGenerator.nextEventId(), goal.getId(), goal.getUserId(), goal.getVersion(), instant));
    return result(goal);
  }

  @Override
  @Transactional
  public PlanDraftResult savePlanDraft(SavePlanDraftCommand command) {
    if (command == null
        || blank(command.requestKey())
        || command.userId() <= 0
        || command.goalId() <= 0
        || blank(command.planSnapshotJson())
        || blank(command.source())) {
      throw new BusinessException("GOAL_INVALID_PLAN_DRAFT", "计划草案参数不完整");
    }
    requireCoreAccess(command.userId());
    Goal goal = ownedGoal(command.userId(), command.goalId());
    String planDigest =
        digest(
            command.planSnapshotJson().trim()
                + "|"
                + Objects.toString(command.adjustmentReason(), "")
                + "|"
                + json(command.milestones())
                + "|"
                + json(command.actions()));
    PlanVersion existed = repository.findPlanByRequestKey(command.requestKey()).orElse(null);
    if (existed != null) {
      if (existed.goalId() != goal.getId() || !existed.requestDigest().equals(planDigest)) {
        throw new BusinessException("GOAL_IDEMPOTENCY_CONFLICT", "同一请求不能保存不同计划草案");
      }
      return draftResult(existed);
    }
    LocalDateTime now = utc(clock.instant());
    long planId = idGenerator.nextId();
    PlanVersion plan =
        PlanVersion.draft(
            planId,
            goal.getId(),
            repository.nextPlanVersionNo(goal.getId()),
            command.requestKey(),
            planDigest,
            command.planSnapshotJson(),
            command.adjustmentReason(),
            command.source(),
            now);
    List<Milestone> milestones = createMilestones(planId, command.milestones(), now);
    List<Action> actions =
        createActions(goal.getId(), planId, milestones, command.actions(), ActionStatus.DRAFT, now);
    repository.insertPlanBundle(plan, milestones, actions);
    return draftResult(plan);
  }

  @Override
  @Transactional
  public GoalResult confirmPlanDraft(ConfirmPlanDraftCommand command) {
    if (command == null
        || blank(command.requestKey())
        || command.userId() <= 0
        || command.planVersionId() <= 0
        || command.expectedGoalVersion() < 0) {
      throw new BusinessException("GOAL_INVALID_PLAN_CONFIRMATION", "计划确认参数不完整");
    }
    requireCoreAccess(command.userId());
    PlanVersion proposal =
        repository
            .findPlanById(command.planVersionId())
            .orElseThrow(() -> new BusinessException("GOAL_PLAN_NOT_FOUND", "计划草案不存在"));
    Goal goal = ownedGoal(command.userId(), proposal.goalId());
    if (proposal.status() == PlanVersionStatus.ACTIVE
        && Objects.equals(goal.getCurrentPlanVersionId(), proposal.id())) {
      return result(goal);
    }
    LocalDateTime now = utc(clock.instant());
    PlanVersion active = proposal.activate(now);
    long previousVersion = goal.getVersion();
    goal.activatePlan(active.id(), command.expectedGoalVersion(), now);
    if (!repository.activatePlanVersion(active, proposal.status())) {
      throw new BusinessException("GOAL_PLAN_CONFLICT", "计划草案状态已变化");
    }
    repository.activatePlanActions(active.id());
    repository.supersedeOtherPlans(goal.getId(), active.id());
    if (!repository.updateActivatedPlan(goal, previousVersion)) {
      throw new BusinessException("GOAL_VERSION_CONFLICT", "目标已被其他终端更新");
    }
    eventPublisher.publish(
        new PlanActivatedEvent(
            idGenerator.nextEventId(),
            goal.getUserId(),
            goal.getId(),
            active.id(),
            goal.getVersion(),
            clock.instant()));
    return result(goal);
  }

  @Override
  @Transactional
  public GoalResult confirmPlan(ConfirmPlanCommand command) {
    validate(command);
    requireCoreAccess(command.userId());
    Goal goal = ownedGoal(command.userId(), command.goalId());
    String planDigest =
        digest(
            command.planSnapshotJson().trim()
                + "|"
                + Objects.toString(command.adjustmentReason(), "")
                + "|"
                + json(command.milestones())
                + "|"
                + json(command.actions()));
    PlanVersion existed = repository.findPlanByRequestKey(command.requestKey()).orElse(null);
    if (existed != null) {
      if (existed.goalId() != goal.getId() || !existed.requestDigest().equals(planDigest)) {
        throw new BusinessException("GOAL_IDEMPOTENCY_CONFLICT", "同一请求不能确认不同计划");
      }
      return result(goal);
    }

    Instant instant = clock.instant();
    LocalDateTime now = utc(instant);
    long planId = idGenerator.nextId();
    PlanVersion plan =
        new PlanVersion(
            planId,
            goal.getId(),
            repository.nextPlanVersionNo(goal.getId()),
            command.requestKey(),
            planDigest,
            command.planSnapshotJson(),
            command.adjustmentReason(),
            now,
            now);
    List<Milestone> milestones = createMilestones(planId, command.milestones(), now);
    List<Action> actions =
        createActions(
            goal.getId(), planId, milestones, command.actions(), ActionStatus.ACTIVE, now);
    long previousVersion = goal.getVersion();
    goal.activatePlan(plan.id(), command.expectedGoalVersion(), now);

    // 计划版本、里程碑、行动和目标当前版本在同一本地事务内原子切换。
    repository.insertPlanBundle(plan, milestones, actions);
    repository.supersedeOtherPlans(goal.getId(), plan.id());
    if (!repository.updateActivatedPlan(goal, previousVersion)) {
      throw new BusinessException("GOAL_VERSION_CONFLICT", "目标已被其他终端更新");
    }
    eventPublisher.publish(
        new PlanActivatedEvent(
            idGenerator.nextEventId(), goal.getUserId(), goal.getId(), plan.id(), goal.getVersion(), instant));
    return result(goal);
  }

  @Override
  @Transactional(readOnly = true)
  public GoalResult getGoal(long userId, long goalId) {
    return result(ownedGoal(userId, goalId));
  }

  @Override
  @Transactional
  public int generateOccurrences(long actionId, LocalDate fromDate, LocalDate toDate) {
    if (fromDate == null
        || toDate == null
        || toDate.isBefore(fromDate)
        || toDate.isAfter(fromDate.plusDays(62))) {
      throw new BusinessException("GOAL_INVALID_OCCURRENCE_WINDOW", "行动实例生成窗口不合法");
    }
    Action action =
        repository
            .findAction(actionId)
            .orElseThrow(() -> new BusinessException("GOAL_ACTION_NOT_FOUND", "行动不存在"));
    if (action.status() != ActionStatus.ACTIVE) {
      return 0;
    }
    LocalDateTime now = utc(clock.instant());
    List<ActionOccurrence> occurrences = new ArrayList<>();
    for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
      if (!action.occursOn(date)) {
        continue;
      }
      // atZone 对 DST 缺口自动前移，对重叠时刻固定选择较早偏移；唯一键保证不重不漏。
      Instant scheduled =
          date.atTime(action.localTime()).atZone(ZoneId.of(action.timezone())).toInstant();
      occurrences.add(
          ActionOccurrence.schedule(
              idGenerator.nextId(), action.id(), scheduled, date, action.timezone(), now));
    }
    return repository.insertOccurrences(occurrences);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OccurrenceResult> listOccurrences(long userId, LocalDate fromDate, LocalDate toDate) {
    requireCoreAccess(userId);
    List<Goal> goals = repository.findByUserId(userId);
    List<Action> actions =
        repository.findActionsByGoalIds(goals.stream().map(Goal::getId).toList());
    Map<Long, Action> actionsById =
        actions.stream().collect(Collectors.toMap(Action::id, Function.identity()));
    return repository
        .findOccurrences(new ArrayList<>(actionsById.keySet()), fromDate, toDate)
        .stream()
        .map(
            o -> {
              Action action = actionsById.get(o.getActionId());
              return new OccurrenceResult(
                  o.getId(),
                  o.getActionId(),
                  action.title(),
                  o.getScheduledAt(),
                  o.getLocalDate(),
                  o.getTimezone(),
                  o.getStatus().name());
            })
        .toList();
  }

  @Override
  @Transactional
  public CheckInResult checkIn(CheckInCommand command) {
    validate(command);
    requireCoreAccess(command.userId());
    ActionOccurrence occurrence =
        repository
            .findOccurrence(command.occurrenceId())
            .orElseThrow(() -> new BusinessException("GOAL_OCCURRENCE_NOT_FOUND", "行动实例不存在"));
    Action action =
        repository
            .findAction(occurrence.getActionId())
            .orElseThrow(() -> new BusinessException("GOAL_ACTION_NOT_FOUND", "行动不存在"));
    Goal goal = ownedGoal(command.userId(), action.goalId());
    String requestDigest =
        digest(
            command.result()
                + "|"
                + Objects.toString(command.note(), "")
                + "|"
                + Objects.toString(command.evidenceReference(), "")
                + "|"
                + command.correction());
    CheckIn repeated =
        repository.findCheckInByRequest(occurrence.getId(), command.requestKey()).orElse(null);
    if (repeated != null) {
      if (!repeated.getRequestDigest().equals(requestDigest)) {
        throw new BusinessException("GOAL_IDEMPOTENCY_CONFLICT", "同一打卡请求不能提交不同内容");
      }
      return checkInResult(repeated, occurrence, goal.getProgress());
    }
    CheckIn effective = repository.findEffectiveCheckIn(occurrence.getId()).orElse(null);
    if (effective != null && !command.correction()) {
      throw new BusinessException("GOAL_CHECK_IN_EXISTS", "行动已经打卡，修正时必须显式声明");
    }
    if (effective != null) {
      repository.supersedeCheckIn(effective.getId());
      effective.supersede();
    }

    Instant instant = clock.instant();
    LocalDateTime now = utc(instant);
    CheckIn checkIn =
        CheckIn.record(
            idGenerator.nextId(),
            occurrence.getId(),
            command.userId(),
            command.requestKey(),
            requestDigest,
            command.result(),
            command.note(),
            command.evidenceReference(),
            now);
    String previousStatus = occurrence.getStatus().name();
    occurrence.checkIn(command.result(), now);
    if (!repository.updateOccurrence(occurrence, previousStatus)) {
      throw new BusinessException("GOAL_OCCURRENCE_CONFLICT", "行动实例已被其他终端更新");
    }
    repository.insertCheckIn(checkIn);
    long previousGoalVersion = goal.getVersion();
    goal.updateProgress(repository.calculateProgress(goal.getId(), now), now);
    if (!repository.updateProgress(goal, previousGoalVersion)) {
      throw new BusinessException("GOAL_VERSION_CONFLICT", "目标进度已变化");
    }
    eventPublisher.publish(
        new ActionCheckedInEvent(
            idGenerator.nextEventId(),
            goal.getUserId(),
            goal.getId(),
            occurrence.getId(),
            command.result(),
            goal.getVersion(),
            instant));
    return checkInResult(checkIn, occurrence, goal.getProgress());
  }

  @Override
  @Transactional
  public ReviewResult completeReview(CompleteReviewCommand command) {
    if (command == null
        || command.userId() <= 0
        || command.reviewId() <= 0
        || isBlank(command.requestKey())
        || isBlank(command.conclusionJson())) {
      throw new BusinessException("GOAL_INVALID_REVIEW", "复盘参数不完整");
    }
    requireCoreAccess(command.userId());
    Review review =
        repository
            .findReview(command.reviewId())
            .orElseThrow(() -> new BusinessException("GOAL_REVIEW_NOT_FOUND", "复盘不存在"));
    Goal goal = ownedGoal(command.userId(), review.getGoalId());
    String digest = digest(command.conclusionJson());
    long previous = review.getVersion();
    review.complete(command.requestKey(), digest, command.conclusionJson(), utc(clock.instant()));
    if (review.getVersion() != previous && !repository.updateReview(review, previous)) {
      throw new BusinessException("GOAL_REVIEW_CONFLICT", "复盘已被其他终端更新");
    }
    eventPublisher.publish(
        new ReviewCompletedEvent(
            idGenerator.nextEventId(),
            goal.getUserId(),
            goal.getId(),
            review.getId(),
            goal.getVersion(),
            clock.instant()));
    return reviewResult(review);
  }

  /** 为全部生效行动滚动生成未来十四天实例。 */
  @Transactional
  public int generateRollingOccurrences() {
    LocalDate from = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    LocalDate to = from.plusDays(13);
    LocalDateTime now = utc(clock.instant());
    List<Long> goalIds = repository.findActiveGoals().stream().map(Goal::getId).toList();
    List<ActionOccurrence> occurrences = new ArrayList<>();
    for (Action action : repository.findActionsByGoalIds(goalIds)) {
      for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
        if (!action.occursOn(date)) {
          continue;
        }
        Instant scheduled =
            date.atTime(action.localTime()).atZone(ZoneId.of(action.timezone())).toInstant();
        occurrences.add(
            ActionOccurrence.schedule(
                idGenerator.nextId(), action.id(), scheduled, date, action.timezone(), now));
      }
    }
    return repository.insertOccurrences(occurrences);
  }

  /** 为全部活跃目标幂等创建当前自然周复盘。 */
  @Transactional
  public int scheduleCurrentWeekReviews() {
    LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    WeekFields week = WeekFields.ISO;
    String period =
        today.getYear() + "-W" + String.format("%02d", today.get(week.weekOfWeekBasedYear()));
    LocalDateTime now = utc(clock.instant());
    List<Review> pendingReviews = new ArrayList<>();
    for (Goal goal : repository.findActiveGoals()) {
      if (repository.findReviewByGoalAndPeriod(goal.getId(), period).isPresent()) {
        continue;
      }
      pendingReviews.add(
          Review.schedule(
              idGenerator.nextId(),
              goal.getId(),
              period,
              "{\"goalVersion\":" + goal.getVersion() + ",\"progress\":" + goal.getProgress() + "}",
              now));
    }
    // 多实例并发由 goal_id+period_key 唯一键和 INSERT IGNORE 兜底。
    return repository.insertReviews(pendingReviews);
  }

  private List<Milestone> createMilestones(
      long planId, List<PlanMilestoneDraft> drafts, LocalDateTime now) {
    Set<Integer> sequences = new HashSet<>();
    List<Milestone> result = new ArrayList<>();
    for (PlanMilestoneDraft draft : drafts) {
      if (!sequences.add(draft.sequenceNo())) {
        throw new BusinessException("GOAL_DUPLICATE_MILESTONE", "里程碑顺序重复");
      }
      result.add(
          new Milestone(
              idGenerator.nextId(),
              planId,
              draft.sequenceNo(),
              draft.title(),
              draft.successCriteria(),
              now));
    }
    return result;
  }

  private List<Action> createActions(
      long goalId,
      long planId,
      List<Milestone> milestones,
      List<PlanActionDraft> drafts,
      ActionStatus initialStatus,
      LocalDateTime now) {
    Map<Integer, Long> milestoneIds =
        milestones.stream().collect(Collectors.toMap(Milestone::sequenceNo, Milestone::id));
    Set<String> keys = new HashSet<>();
    List<Action> result = new ArrayList<>();
    for (PlanActionDraft draft : drafts) {
      if (isBlank(draft.clientKey()) || !keys.add(draft.clientKey())) {
        throw new BusinessException("GOAL_DUPLICATE_ACTION", "行动客户端标识为空或重复");
      }
      Long milestoneId =
          draft.milestoneSequence() == null ? null : milestoneIds.get(draft.milestoneSequence());
      if (draft.milestoneSequence() != null && milestoneId == null) {
        throw new BusinessException("GOAL_MILESTONE_NOT_FOUND", "行动引用的里程碑不存在");
      }
      result.add(
          new Action(
              idGenerator.nextId(),
              goalId,
              planId,
              milestoneId,
              draft.clientKey(),
              draft.title(),
              draft.recurrenceType(),
              draft.weekdays(),
              draft.startDate(),
              draft.endDate(),
              draft.localTime(),
              draft.timezone(),
              initialStatus,
              0,
              now,
              now));
    }
    return result;
  }

  private Goal ownedGoal(long userId, long goalId) {
    Goal goal =
        repository
            .findById(goalId)
            .orElseThrow(() -> new BusinessException("GOAL_NOT_FOUND", "目标不存在"));
    goal.assertOwnedBy(userId);
    return goal;
  }

  private void requireCoreAccess(long userId) {
    AccessProfile p = identityFacade.getAccessProfile(userId);
    if (!p.coreFeaturesAllowed()) {
      throw new BusinessException("GOAL_ACCESS_DENIED", "当前账号尚未获得目标功能权限");
    }
  }

  private void validate(CreateGoalCommand c) {
    if (c == null
        || isBlank(c.requestKey())
        || isBlank(c.title())
        || isBlank(c.successCriteria())
        || c.userId() <= 0) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "创建目标参数不完整");
    }
  }

  private void validate(ConfirmPlanCommand c) {
    if (c == null
        || isBlank(c.requestKey())
        || c.userId() <= 0
        || c.goalId() <= 0
        || c.expectedGoalVersion() < 0
        || isBlank(c.planSnapshotJson())) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "确认计划参数不完整");
    }
  }

  private void validate(CheckInCommand c) {
    if (c == null
        || isBlank(c.requestKey())
        || c.userId() <= 0
        || c.occurrenceId() <= 0
        || c.result() == null) {
      throw new BusinessException("GOAL_INVALID_CHECK_IN", "打卡参数不完整");
    }
  }

  private String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private PlanDraftResult draftResult(PlanVersion p) {
    return new PlanDraftResult(p.id(), p.goalId(), p.versionNo(), p.status().name(), p.source());
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private GoalResult result(Goal g) {
    return new GoalResult(
        g.getId(),
        g.getPublicId(),
        g.getUserId(),
        g.getTitle(),
        g.getSuccessCriteria(),
        g.getStatus(),
        g.getCurrentPlanVersionId(),
        g.getProgress(),
        g.getVersion());
  }

  private CheckInResult checkInResult(CheckIn c, ActionOccurrence o, int progress) {
    return new CheckInResult(
        c.getId(),
        o.getId(),
        c.getResult(),
        o.getStatus().name(),
        progress,
        c.getRecordedAt().toInstant(ZoneOffset.UTC));
  }

  private ReviewResult reviewResult(Review r) {
    return new ReviewResult(
        r.getId(),
        r.getGoalId(),
        r.getPeriodKey(),
        r.getStatus().name(),
        r.getConclusionJson(),
        r.getCompletedAt() == null ? null : r.getCompletedAt().toInstant(ZoneOffset.UTC));
  }

  private LocalDateTime utc(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
