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
import com.lingxi.kernel.PageResult;
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
import org.springframework.beans.factory.annotation.Autowired;

/** 目标、计划、行动、打卡和复盘应用服务。 */
@Service
public class GoalApplicationService implements GoalFacade {
  /** 列表类查询允许的最大页大小。 */
  private static final int MAX_PAGE_SIZE = 200;
  /** 行动实例滚动生成窗口长度（含当天）。 */
  private static final int ROLLING_WINDOW_DAYS = 14;

  private final GoalRepository repository;
  private final IdentityFacade identityFacade;
  private final IdGenerator idGenerator;
  private final DomainEventPublisher eventPublisher;
  private final AchievementApplicationService achievementService;
  private final ObjectMapper objectMapper;
  private final ActiveGoalQuotaPort quotaPort;
  private final Clock clock;

  @Autowired
  public GoalApplicationService(
      GoalRepository repository,
      IdentityFacade identityFacade,
      IdGenerator idGenerator,
      DomainEventPublisher eventPublisher,
      AchievementApplicationService achievementService,
      ObjectMapper objectMapper,
      ActiveGoalQuotaPort quotaPort) {
    this(
        repository,
        identityFacade,
        idGenerator,
        eventPublisher,
        achievementService,
        objectMapper,
        quotaPort,
        Clock.systemUTC());
  }

  GoalApplicationService(
      GoalRepository repository,
      IdentityFacade identityFacade,
      IdGenerator idGenerator,
      DomainEventPublisher eventPublisher,
      AchievementApplicationService achievementService,
      ObjectMapper objectMapper,
      ActiveGoalQuotaPort quotaPort,
      Clock clock) {
    this.repository = repository;
    this.identityFacade = identityFacade;
    this.idGenerator = idGenerator;
    this.eventPublisher = eventPublisher;
    this.achievementService = achievementService;
    this.objectMapper = objectMapper;
    this.quotaPort = quotaPort;
    this.clock = clock;
  }

  @Override
  @Transactional
  public GoalResult createGoal(CreateGoalCommand command) {
    validate(command);
    requireCoreAccess(command.userId());
    GoalDefinition definition = toDefinition(command.definition());
    String requestDigest =
        digest(
            definition.title()
                + "|"
                + definition.successCriteria()
                + "|"
                + definition.goalType()
                + "|"
                + Objects.toString(definition.startDate(), "")
                + "|"
                + Objects.toString(definition.targetEndDate(), ""));
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
            definition,
            now);
    repository.insert(goal);
    eventPublisher.publish(
        new GoalCreatedEvent(
            idGenerator.nextEventId(), goal.getId(), goal.getUserId(), goal.getVersion(), instant));
    return result(goal);
  }

  @Override
  @Transactional
  public GoalResult updateDefinition(UpdateGoalDefinitionCommand command) {
    if (command == null
        || isBlank(command.requestKey())
        || command.userId() <= 0
        || command.goalId() <= 0
        || command.definition() == null) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "更新目标定义参数不完整");
    }
    requireCoreAccess(command.userId());
    Goal goal = ownedGoal(command.userId(), command.goalId());
    long previousVersion = goal.getVersion();
    goal.updateDefinition(toDefinition(command.definition()), command.expectedVersion(), utc(clock.instant()));
    if (!repository.updateDefinitionAndState(goal, previousVersion)) {
      throw new BusinessException("GOAL_VERSION_CONFLICT", "目标已被其他终端更新");
    }
    return result(goal);
  }

  @Override
  @Transactional
  public GoalResult transition(TransitionGoalCommand command) {
    if (command == null
        || isBlank(command.requestKey())
        || command.userId() <= 0
        || command.goalId() <= 0
        || command.transition() == null) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "目标状态流转参数不完整");
    }
    requireCoreAccess(command.userId());
    Goal goal = ownedGoal(command.userId(), command.goalId());
    GoalStatus target = targetStatus(command.transition());
    // 状态幂等：目标已处于目标状态时直接返回当前结果，重复提交不会报错，也不需要额外的幂等键存储。
    if (goal.getStatus() == target) {
      return result(goal);
    }
    Instant instant = clock.instant();
    LocalDateTime now = utc(instant);
    GoalStatus from = goal.getStatus();
    long previousVersion = goal.getVersion();
    switch (command.transition()) {
      case PAUSE -> {
        assertResumeDateNotPast(command.expectedResumeDate(), now);
        goal.pause(command.expectedResumeDate(), command.expectedVersion(), now);
      }
      case RESUME -> goal.resume(command.expectedVersion(), now);
      case ABANDON -> goal.abandon(command.abandonReason(), command.expectedVersion(), now);
      case ARCHIVE -> goal.archive(command.expectedVersion(), now);
    }
    if (!repository.updateDefinitionAndState(goal, previousVersion)) {
      throw new BusinessException("GOAL_VERSION_CONFLICT", "目标已被其他终端更新");
    }
    eventPublisher.publish(
        new GoalStatusChangedEvent(
            idGenerator.nextEventId(),
            goal.getId(),
            goal.getUserId(),
            from,
            goal.getStatus(),
            goal.getVersion(),
            instant));
    return result(goal);
  }

  @Override
  public GoalQuotaResult quota(long userId) {
    requireCoreAccess(userId);
    return new GoalQuotaResult(
        quotaPort.allowanceFor(userId),
        repository.countActiveGoals(userId, 0));
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
    assertQuotaAllowsActivation(goal);
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
    // 计划生效即生成实例，用户无需等待下一次滚动调度。
    generateActivatedPlanOccurrences(goal, active.id(), now);
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
    assertQuotaAllowsActivation(goal);
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
    // 计划生效即生成实例，用户无需等待下一次滚动调度。
    generateActivatedPlanOccurrences(goal, plan.id(), now);
    return result(goal);
  }

  @Override
  @Transactional(readOnly = true)
  public GoalResult getGoal(long userId, long goalId) {
    return result(ownedGoal(userId, goalId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<GoalResult> listGoals(long userId) {
    requireCoreAccess(userId);
    return repository.findByUserId(userId).stream().map(this::result).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<StarterGoalTemplate> listStarterTemplates(long userId) {
    requireCoreAccess(userId);
    return repository.findStarterTemplates();
  }

  @Override
  @Transactional(readOnly = true)
  public StarterGoalTemplate getStarterTemplate(long userId, String templateKey) {
    requireCoreAccess(userId);
    return repository
        .findStarterTemplate(templateKey)
        .orElseThrow(
            () -> new BusinessException("GOAL_TEMPLATE_NOT_FOUND", "入门模板不存在或已下架"));
  }

  /**
   * 推进首目标引导的澄清阶段。
   *
   * <p>只写阶段字段，不动目标状态机：PRD 要求用户随时可切换为手动创建，
   * 如果引导没走完就把目标锁住，用户会被卡在一个不是自己选的流程里。
   */
  @Override
  @Transactional
  public GoalResult advanceClarification(AdvanceClarificationCommand command) {
    if (command == null
        || isBlank(command.requestKey())
        || command.userId() <= 0
        || command.stage() == null) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "引导阶段参数不完整");
    }
    requireCoreAccess(command.userId());
    Goal goal = ownedGoal(command.userId(), command.goalId());
    LocalDateTime now = utc(clock.instant());
    long previous = goal.getVersion();
    goal.advanceClarification(command.stage(), command.expectedVersion(), now);
    if (goal.getVersion() != previous && !repository.updateDefinitionAndState(goal, previous)) {
      throw new BusinessException("GOAL_VERSION_CONFLICT", "目标已被其他终端更新");
    }
    return result(goal);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<OccurrenceResult> findOccurrence(long userId, long occurrenceId) {
    if (userId <= 0 || occurrenceId <= 0) {
      return Optional.empty();
    }
    ActionOccurrence occurrence = repository.findOccurrence(occurrenceId).orElse(null);
    if (occurrence == null) {
      return Optional.empty();
    }
    Action action = repository.findAction(occurrence.getActionId()).orElse(null);
    if (action == null) {
      return Optional.empty();
    }
    Goal goal = repository.findById(action.goalId()).orElse(null);
    if (goal == null || goal.getUserId() != userId) {
      return Optional.empty();
    }
    return Optional.of(occurrenceResult(occurrence, action));
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<ReviewResult> listReviews(long userId, Long goalId, int page, int pageSize) {
    requireCoreAccess(userId);
    if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw new BusinessException("GOAL_INVALID_QUERY", "复盘查询参数不合法");
    }
    long total = repository.countReviews(userId, goalId);
    List<ReviewResult> items =
        repository.findReviewsByUser(userId, goalId, page, pageSize).stream()
            .map(this::reviewResult)
            .toList();
    return new PageResult<>(items, total, page, pageSize);
  }

  @Override
  @Transactional(readOnly = true)
  public ReviewResult getReview(long userId, long reviewId) {
    requireCoreAccess(userId);
    Review review =
        repository
            .findReview(reviewId)
            .orElseThrow(() -> new BusinessException("GOAL_REVIEW_NOT_FOUND", "复盘不存在"));
    ownedGoal(userId, review.getGoalId());
    return reviewResult(review);
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
    Goal goal =
        repository
            .findById(action.goalId())
            .orElseThrow(() -> new BusinessException("GOAL_NOT_FOUND", "目标不存在"));
    return insertNewOccurrences(
        occurrences, Map.of(action.id(), action), Map.of(goal.getId(), goal));
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
  @Transactional(readOnly = true)
  public List<ActionResult> listActions(long userId, long goalId) {
    requireCoreAccess(userId);
    Goal goal = ownedGoal(userId, goalId);
    return repository.findActionsByGoalIds(List.of(goal.getId())).stream()
        .map(this::actionResult)
        .toList();
  }

  @Override
  @Transactional
  public ActionResult addAction(AddActionCommand command) {
    if (command == null || isBlank(command.requestKey()) || command.userId() <= 0 || command.input() == null) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "新增行动参数不完整");
    }
    requireCoreAccess(command.userId());
    Goal goal = ownedGoal(command.userId(), command.goalId());
    if (goal.getCurrentPlanVersionId() == null) {
      throw new BusinessException("GOAL_PLAN_NOT_ACTIVE", "目标还没有生效计划，请先确认计划再新增行动");
    }
    LocalDateTime now = utc(clock.instant());
    Long milestoneId = null;
    if (command.milestoneSequence() != null) {
      Milestone milestone =
          repository
              .findMilestoneByPlanAndSequence(goal.getCurrentPlanVersionId(), command.milestoneSequence())
              .orElseThrow(() -> new BusinessException("GOAL_MILESTONE_NOT_FOUND", "归属里程碑不存在"));
      milestoneId = milestone.id();
    }
    ActionInput input = command.input();
    Action action =
        new Action(
            idGenerator.nextId(),
            goal.getId(),
            goal.getCurrentPlanVersionId(),
            milestoneId,
            "extra-" + idGenerator.nextId(),
            input.title(),
            toSchedule(input),
            toDetail(input),
            ActionStatus.ACTIVE,
            0,
            now,
            now);
    repository.insertAction(action);
    // 新增行动立即补齐滚动窗口内的实例，否则用户要等到下一次每小时调度才看到它。
    generateOccurrences(
        List.of(action),
        Map.of(goal.getId(), goal),
        LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC),
        LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).plusDays(ROLLING_WINDOW_DAYS - 1),
        now);
    return actionResult(action);
  }

  @Override
  @Transactional
  public ActionResult editAction(EditActionCommand command) {
    if (command == null || isBlank(command.requestKey()) || command.userId() <= 0 || command.input() == null) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "编辑行动参数不完整");
    }
    requireCoreAccess(command.userId());
    Action action = ownedAction(command.userId(), command.actionId());
    LocalDateTime now = utc(clock.instant());
    Action updated =
        action.edited(
            command.input().title(), toSchedule(command.input()), toDetail(command.input()), now);
    return persistActionChange(action, updated, command.expectedVersion(), now);
  }

  @Override
  @Transactional
  public ActionResult cancelAction(CancelActionCommand command) {
    if (command == null || isBlank(command.requestKey()) || command.userId() <= 0) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "取消行动参数不完整");
    }
    requireCoreAccess(command.userId());
    Action action = ownedAction(command.userId(), command.actionId());
    if (action.status() == ActionStatus.CANCELLED) {
      return actionResult(action);
    }
    LocalDateTime now = utc(clock.instant());
    Action cancelled = action.cancelled(now);
    if (!repository.updateAction(cancelled, command.expectedVersion())) {
      throw new BusinessException("GOAL_ACTION_VERSION_CONFLICT", "行动已被其他终端更新");
    }
    // 取消后不再需要尚未执行的实例；已打卡的历史事实保留。
    repository.deleteFutureScheduledOccurrences(action.id(), now);
    return actionResult(cancelled);
  }

  @Override
  @Transactional
  public ActionResult copyAction(CopyActionCommand command) {
    if (command == null || isBlank(command.requestKey()) || command.userId() <= 0) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "复制行动参数不完整");
    }
    requireCoreAccess(command.userId());
    Action action = ownedAction(command.userId(), command.actionId());
    Goal goal = ownedGoal(command.userId(), action.goalId());
    LocalDateTime now = utc(clock.instant());
    Action copy =
        action.copyAs(
            idGenerator.nextId(),
            isBlank(command.clientKey()) ? "copy-" + idGenerator.nextId() : command.clientKey(),
            command.title(),
            now);
    repository.insertAction(copy);
    generateOccurrences(
        List.of(copy),
        Map.of(goal.getId(), goal),
        LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC),
        LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).plusDays(ROLLING_WINDOW_DAYS - 1),
        now);
    return actionResult(copy);
  }

  @Override
  @Transactional
  public ActionResult moveAction(MoveActionCommand command) {
    if (command == null || isBlank(command.requestKey()) || command.userId() <= 0) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "移动行动参数不完整");
    }
    requireCoreAccess(command.userId());
    Action action = ownedAction(command.userId(), command.actionId());
    LocalDateTime now = utc(clock.instant());
    Action moved = action.movedTo(command.newStartDate(), now);
    return persistActionChange(action, moved, command.expectedVersion(), now);
  }

  @Override
  @Transactional
  public OccurrenceResult adjustOccurrence(AdjustOccurrenceCommand command) {
    if (command == null || isBlank(command.requestKey()) || command.userId() <= 0 || command.type() == null) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "单次调整参数不完整");
    }
    requireCoreAccess(command.userId());
    ActionOccurrence occurrence =
        repository
            .findOccurrence(command.occurrenceId())
            .orElseThrow(() -> new BusinessException("GOAL_OCCURRENCE_NOT_FOUND", "行动实例不存在"));
    Action action = ownedAction(command.userId(), occurrence.getActionId());
    Goal goal = ownedGoal(command.userId(), action.goalId());
    if (occurrence.getStatus() != OccurrenceStatus.SCHEDULED) {
      throw new BusinessException("GOAL_OCCURRENCE_NOT_ADJUSTABLE", "只有尚未执行的实例可以单次调整");
    }
    LocalDateTime now = utc(clock.instant());
    // 目标日期原样交给领域层：跳过时带上目标日期会被领域校验拒绝，规则只在一处维护。
    ActionException exception =
        new ActionException(
            idGenerator.nextId(),
            action.id(),
            occurrence.getLocalDate(),
            command.type(),
            command.targetDate(),
            command.reason(),
            now);
    repository.upsertActionException(exception);
    // 只精确删掉「这一次」的实例：重复规则本身没变，其余未来实例仍然有效。
    repository.deleteScheduledOccurrenceAt(
        action.id(),
        LocalDateTime.ofInstant(occurrence.getScheduledAt(), ZoneOffset.UTC));
    LocalDate from = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    generateOccurrences(
        List.of(action),
        Map.of(goal.getId(), goal),
        from,
        from.plusDays(ROLLING_WINDOW_DAYS - 1),
        now);
    if (exception.type() == ActionExceptionType.SKIP) {
      return new OccurrenceResult(
          occurrence.getId(),
          action.id(),
          action.title(),
          occurrence.getScheduledAt(),
          occurrence.getLocalDate(),
          occurrence.getTimezone(),
          OccurrenceStatus.SKIPPED.name());
    }
    Instant target =
        exception
            .rescheduledDate()
            .atTime(action.localTime())
            .atZone(ZoneId.of(action.timezone()))
            .toInstant();
    return new OccurrenceResult(
        occurrence.getId(),
        action.id(),
        action.title(),
        target,
        exception.rescheduledDate(),
        occurrence.getTimezone(),
        OccurrenceStatus.SCHEDULED.name());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ActionExceptionResult> listActionExceptions(long userId, long goalId) {
    requireCoreAccess(userId);
    Goal goal = ownedGoal(userId, goalId);
    List<Long> actionIds =
        repository.findActionsByGoalIds(List.of(goal.getId())).stream().map(Action::id).toList();
    return repository.findActionExceptions(actionIds).stream()
        .map(
            x ->
                new ActionExceptionResult(
                    x.id(), x.actionId(), x.localDate(), x.type(), x.rescheduledDate(), x.reason()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CheckInView> findEffectiveCheckIn(long userId, long occurrenceId) {
    requireCoreAccess(userId);
    ActionOccurrence occurrence =
        repository
            .findOccurrence(occurrenceId)
            .orElseThrow(() -> new BusinessException("GOAL_OCCURRENCE_NOT_FOUND", "行动实例不存在"));
    Action action = ownedAction(userId, occurrence.getActionId());
    ownedGoal(userId, action.goalId());
    return repository.findEffectiveCheckIn(occurrenceId).map(this::checkInView);
  }

  private CheckInView checkInView(CheckIn c) {
    CheckInDetail d = c.getDetail();
    return new CheckInView(
        c.getId(),
        c.getOccurrenceId(),
        c.getResult(),
        d.note(),
        d.evidenceReference(),
        d.actualMinutes(),
        d.perceivedDifficulty(),
        d.energyLevel(),
        d.moodLevel(),
        d.failureReason(),
        c.getRecordedAt().toInstant(ZoneOffset.UTC));
  }

  // ---------------------------------------------------------------------------
  // 今日工作台
  // ---------------------------------------------------------------------------

  /** 逾期可见窗口：只回溯 30 天，避免长期未处理的历史把工作台压垮。 */
  private static final int OVERDUE_LOOKBACK_DAYS = 30;

  /** 单次工作中台最多返回的未执行条目数。 */
  private static final int TODAY_ITEM_LIMIT = 120;

  /** 快速记录默认返回条数。 */
  private static final int DEFAULT_QUICK_NOTE_LIMIT = 20;

  /** 判定「今天行动过量」的阈值：超过该数量才提示，避免正常清单也被打扰。 */
  private static final int TODAY_OVERLOAD_THRESHOLD = 6;

  @Override
  @Transactional(readOnly = true)
  public TodayResult today(long userId, String timezone, int upcomingDays) {
    requireCoreAccess(userId);
    ZoneId zone = resolveZone(timezone);
    LocalDate today = LocalDate.ofInstant(clock.instant(), zone);
    LocalDate windowStart = today.minusDays(OVERDUE_LOOKBACK_DAYS);
    LocalDate windowEnd = today.plusDays(Math.max(1, Math.min(upcomingDays, 30)));

    List<Goal> goals = repository.findByUserId(userId);
    List<Action> actions = repository.findActionsByGoalIds(goals.stream().map(Goal::getId).toList());
    Map<Long, Goal> goalsById = goals.stream().collect(Collectors.toMap(Goal::getId, Function.identity()));
    Map<Long, Action> actionsById = actions.stream().collect(Collectors.toMap(Action::id, Function.identity()));

    Map<Long, List<ActionException>> exceptionsByAction =
        repository.findActionExceptions(actions.stream().map(Action::id).toList()).stream()
            .collect(Collectors.groupingBy(ActionException::actionId));

    List<ActionOccurrence> occurrences =
        repository.findOccurrences(
            new ArrayList<>(actionsById.keySet()), windowStart, windowEnd);

    // 「需要先做」：统计每个行动被多少个其他行动当作前置。
    Map<Long, Long> unlockCounts =
        actions.stream()
            .map(Action::detail)
            .map(ActionDetail::prerequisiteActionId)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    List<TodayItem> todayItems = new ArrayList<>();
    List<TodayItem> overdueItems = new ArrayList<>();
    List<TodayItem> upcomingItems = new ArrayList<>();
    for (ActionOccurrence occurrence : occurrences) {
      Action action = actionsById.get(occurrence.getActionId());
      Goal goal = action == null ? null : goalsById.get(action.goalId());
      if (action == null || goal == null) {
        continue;
      }
      boolean pending = occurrence.getStatus() == OccurrenceStatus.SCHEDULED;
      boolean isOverdue = pending && occurrence.getLocalDate().isBefore(today);
      TodayItem item =
          todayItem(
              occurrence,
              action,
              goal,
              isOverdue,
              unlockCounts.getOrDefault(action.id(), 0L).intValue(),
              exceptionFor(exceptionsByAction.get(action.id()), occurrence.getLocalDate()));
      if (isOverdue) {
        overdueItems.add(item);
      } else if (occurrence.getLocalDate().isEqual(today)) {
        todayItems.add(item);
      } else if (occurrence.getLocalDate().isAfter(today)) {
        upcomingItems.add(item);
      }
    }

    todayItems.sort(TodayWorkbenchOrder.COMPARATOR);
    overdueItems.sort(TodayWorkbenchOrder.COMPARATOR);
    upcomingItems.sort(TodayWorkbenchOrder.COMPARATOR);
    if (todayItems.size() > TODAY_ITEM_LIMIT) {
      todayItems = new ArrayList<>(todayItems.subList(0, TODAY_ITEM_LIMIT));
    }

    int todayFinished =
        (int) todayItems.stream().filter(item -> item.status() != OccurrenceStatus.SCHEDULED).count();
    List<TodaySuggestion> suggestions =
        buildSuggestions(todayItems, overdueItems, todayFinished, today);

    return new TodayResult(
        today,
        todayItems,
        overdueItems,
        upcomingItems,
        suggestions,
        repository.findActiveFocusSession(userId).map(s -> focusResult(s, clock.instant())).orElse(null),
        repository.findQuickNotes(userId, 5).stream().map(this::quickNoteResult).toList(),
        todayItems.size(),
        todayFinished,
        overdueItems.size(),
        windowStart,
        clock.instant());
  }

  /** 单次调整例外只按原日期命中；被改期到别的日期的实例在生成时已落在目标日期上。 */
  private ActionExceptionType exceptionFor(List<ActionException> exceptions, LocalDate date) {
    if (exceptions == null) {
      return null;
    }
    return exceptions.stream()
        .filter(x -> x.localDate().equals(date))
        .map(ActionException::type)
        .findFirst()
        .orElse(null);
  }

  private TodayItem todayItem(
      ActionOccurrence occurrence,
      Action action,
      Goal goal,
      boolean overdue,
      int unlocksOthers,
      ActionExceptionType exceptionType) {
    return new TodayItem(
        occurrence.getId(),
        action.id(),
        goal.getId(),
        goal.getTitle(),
        action.title(),
        action.detail().description(),
        occurrence.getLocalDate(),
        occurrence.getScheduledAt(),
        action.localTime(),
        occurrence.getTimezone(),
        occurrence.getStatus(),
        overdue,
        action.detail().estimatedMinutes(),
        action.detail().priority(),
        action.detail().difficulty(),
        action.detail().completionCriteria(),
        unlocksOthers,
        exceptionType);
  }

  /**
   * 生成规则化提示。
   *
   * <p>在接入真实模型之前，这些结论完全由可核对的数据推导，并显式标注 {@code source=RULE}：
   * 宁可让用户看到「规则提示」，也不把固定规则伪装成模型判断。
   */
  private List<TodaySuggestion> buildSuggestions(
      List<TodayItem> todayItems, List<TodayItem> overdueItems, int todayFinished, LocalDate today) {
    List<TodaySuggestion> suggestions = new ArrayList<>();
    if (!overdueItems.isEmpty()) {
      TodayItem first = overdueItems.get(0);
      suggestions.add(
          TodaySuggestion.rule(
              "OVERDUE_BACKLOG",
              "有 " + overdueItems.size() + " 项行动已经逾期",
              "最早的一项计划在 " + first.localDate() + "，现在仍未记录结果",
              first.occurrenceId()));
    }
    List<TodayItem> pendingToday =
        todayItems.stream().filter(item -> item.status() == OccurrenceStatus.SCHEDULED).toList();
    if (pendingToday.size() > TODAY_OVERLOAD_THRESHOLD) {
      suggestions.add(
          TodaySuggestion.rule(
              "TODAY_OVERLOAD",
              "今天安排了 " + pendingToday.size() + " 项行动，明显多于常规",
              "阈值 " + TODAY_OVERLOAD_THRESHOLD + " 项；行动过多时建议先做优先级高、耗时短的几项",
              null));
    }
    pendingToday.stream()
        .filter(item -> item.unlocksOthers() > 0)
        .findFirst()
        .ifPresent(
            item ->
                suggestions.add(
                    TodaySuggestion.rule(
                        "FIRST_STEP",
                        "建议先做：" + item.actionTitle(),
                        "它是另外 " + item.unlocksOthers() + " 个行动的前置，先做它才能解锁后续",
                        item.occurrenceId())));
    if (suggestions.isEmpty() && todayFinished > 0) {
      suggestions.add(
          TodaySuggestion.rule(
              "PROGRESS", "今天已记录 " + todayFinished + " 项", "没有逾期积压，按当前节奏继续即可", null));
    }
    return suggestions;
  }

  @Override
  @Transactional
  public FocusSessionResult startFocus(StartFocusCommand command) {
    if (command == null || isBlank(command.requestKey()) || command.userId() <= 0) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "开始专注参数不完整");
    }
    requireCoreAccess(command.userId());
    repository
        .findActiveFocusSession(command.userId())
        .ifPresent(
            existing -> {
              throw new BusinessException("GOAL_FOCUS_ALREADY_ACTIVE", "已有进行中的专注会话，请先结束它");
            });
    Long actionId = null;
    if (command.occurrenceId() != null) {
      ActionOccurrence occurrence =
          repository
              .findOccurrence(command.occurrenceId())
              .orElseThrow(() -> new BusinessException("GOAL_OCCURRENCE_NOT_FOUND", "行动实例不存在"));
      Action action = ownedAction(command.userId(), occurrence.getActionId());
      ownedGoal(command.userId(), action.goalId());
      actionId = action.id();
    }
    Instant now = clock.instant();
    FocusSession session =
        FocusSession.start(
            idGenerator.nextId(),
            command.userId(),
            command.occurrenceId(),
            actionId,
            command.plannedMinutes(),
            now);
    repository.insertFocusSession(session);
    return focusResult(session, now);
  }

  @Override
  @Transactional
  public FocusSessionResult pauseFocus(long userId, long sessionId, long expectedVersion) {
    return transitionFocus(userId, sessionId, expectedVersion, FocusAction.PAUSE, null);
  }

  @Override
  @Transactional
  public FocusSessionResult resumeFocus(long userId, long sessionId, long expectedVersion) {
    return transitionFocus(userId, sessionId, expectedVersion, FocusAction.RESUME, null);
  }

  @Override
  @Transactional
  public FocusSessionResult finishFocus(
      long userId, long sessionId, long expectedVersion, String note) {
    return transitionFocus(userId, sessionId, expectedVersion, FocusAction.FINISH, note);
  }

  @Override
  @Transactional
  public FocusSessionResult abandonFocus(long userId, long sessionId, long expectedVersion) {
    return transitionFocus(userId, sessionId, expectedVersion, FocusAction.ABANDON, null);
  }

  /** 专注会话的四种流转共用一条乐观版本保护与落库路径。 */
  private FocusSessionResult transitionFocus(
      long userId,
      long sessionId,
      long expectedVersion,
      FocusAction action,
      String note) {
    if (userId <= 0 || sessionId <= 0) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "专注会话参数不完整");
    }
    requireCoreAccess(userId);
    FocusSession session =
        repository
            .findFocusSession(sessionId)
            .orElseThrow(() -> new BusinessException("GOAL_FOCUS_NOT_FOUND", "专注会话不存在"));
    if (session.getUserId() != userId) {
      throw new BusinessException("GOAL_FOCUS_NOT_FOUND", "专注会话不存在");
    }
    Instant now = clock.instant();
    switch (action) {
      case PAUSE -> session.pause(now);
      case RESUME -> session.resume(now);
      case FINISH -> session.finish(note, now);
      case ABANDON -> session.abandon(now);
    }
    if (!repository.updateFocusSession(session, expectedVersion)) {
      throw new BusinessException("GOAL_FOCUS_VERSION_CONFLICT", "专注会话已被其他终端更新");
    }
    return focusResult(session, now);
  }

  private enum FocusAction {
    PAUSE,
    RESUME,
    FINISH,
    ABANDON
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<FocusSessionResult> activeFocus(long userId) {
    requireCoreAccess(userId);
    Instant now = clock.instant();
    return repository.findActiveFocusSession(userId).map(s -> focusResult(s, now));
  }

  /**
   * 由聚合还原视图。
   *
   * <p>计时中的会话把「已累计 + 当前区间」一起回给客户端，客户端据此渲染，
   * 不需要自己推算起点，也就不会因为客户端时钟偏差显示错时长。
   */
  private FocusSessionResult focusResult(FocusSession session, Instant now) {
    return new FocusSessionResult(
        session.getId(),
        session.getUserId(),
        session.getOccurrenceId(),
        session.getActionId(),
        session.getStatus(),
        session.active(),
        session.currentSeconds(now),
        session.getPlannedMinutes(),
        session.getStartedAt(),
        session.getLastResumedAt(),
        session.getEndedAt(),
        session.getNote(),
        session.getVersion());
  }

  @Override
  @Transactional
  public QuickNoteResult createQuickNote(CreateQuickNoteCommand command) {
    if (command == null || isBlank(command.requestKey()) || command.userId() <= 0) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "快速记录参数不完整");
    }
    requireCoreAccess(command.userId());
    Instant now = clock.instant();
    LocalDate localDate = LocalDate.ofInstant(now, resolveZone(command.timezone()));
    QuickNote note =
        new QuickNote(
            idGenerator.nextId(),
            command.userId(),
            command.content(),
            command.moodLevel(),
            localDate,
            utc(now));
    repository.insertQuickNote(note);
    return quickNoteResult(note);
  }

  @Override
  @Transactional(readOnly = true)
  public List<QuickNoteResult> listQuickNotes(long userId, int limit) {
    requireCoreAccess(userId);
    int capped = limit <= 0 ? DEFAULT_QUICK_NOTE_LIMIT : Math.min(limit, MAX_PAGE_SIZE);
    return repository.findQuickNotes(userId, capped).stream().map(this::quickNoteResult).toList();
  }

  @Override
  @Transactional
  public void deleteQuickNote(long userId, long noteId) {
    requireCoreAccess(userId);
    QuickNote owned =
        repository.findQuickNotes(userId, MAX_PAGE_SIZE).stream()
            .filter(note -> note.id() == noteId)
            .findFirst()
            .orElseThrow(() -> new BusinessException("GOAL_QUICK_NOTE_NOT_FOUND", "快速记录不存在"));
    repository.deleteQuickNote(owned.id());
  }

  private QuickNoteResult quickNoteResult(QuickNote note) {
    return new QuickNoteResult(
        note.id(),
        note.userId(),
        note.content(),
        note.moodLevel(),
        note.localDate(),
        note.createdAt().toInstant(ZoneOffset.UTC));
  }

  /**
   * 解析调用方时区。
   *
   * <p>「今天」必须按用户所在时区判定：服务端按 UTC 判定会让东八区用户在早上 8 点前
   * 看到昨天的工作台。时区非法时回退到东八区而不是直接失败，保证工作台始终可用。
   */
  private ZoneId resolveZone(String timezone) {
    if (isBlank(timezone)) {
      return ZoneId.of("Asia/Shanghai");
    }
    try {
      return ZoneId.of(timezone);
    } catch (DateTimeException e) {
      return ZoneId.of("Asia/Shanghai");
    }
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
                + Objects.toString(command.actualMinutes(), "")
                + "|"
                + Objects.toString(command.perceivedDifficulty(), "")
                + "|"
                + Objects.toString(command.energyLevel(), "")
                + "|"
                + Objects.toString(command.moodLevel(), "")
                + "|"
                + Objects.toString(command.failureReason(), "")
                + "|"
                + command.correction());
    CheckIn repeated =
        repository.findCheckInByRequest(occurrence.getId(), command.requestKey()).orElse(null);
    if (repeated != null) {
      if (!repeated.getRequestDigest().equals(requestDigest)) {
        throw new BusinessException("GOAL_IDEMPOTENCY_CONFLICT", "同一打卡请求不能提交不同内容");
      }
      return checkInResult(repeated, occurrence, goal.getProgress(), false, List.of());
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
            new CheckInDetail(
                command.note(),
                command.evidenceReference(),
                command.actualMinutes(),
                command.perceivedDifficulty(),
                command.energyLevel(),
                command.moodLevel(),
                command.failureReason()),
            now);
    String previousStatus = occurrence.getStatus().name();
    occurrence.checkIn(command.result(), now);
    if (!repository.updateOccurrence(occurrence, previousStatus)) {
      throw new BusinessException("GOAL_OCCURRENCE_CONFLICT", "行动实例已被其他终端更新");
    }
    repository.insertCheckIn(checkIn);
    long previousGoalVersion = goal.getVersion();
    GoalStatus statusBeforeCheckIn = goal.getStatus();
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
    // 成就与打卡共用同一本地事务，授予结果随本次打卡一起返回。
    List<AchievementResult> newAchievements =
        achievementService.grantForCheckIn(
            new CheckInAchievementContext(goal, statusBeforeCheckIn, action, occurrence, command.result()),
            now);
    return checkInResult(checkIn, occurrence, goal.getProgress(), effective != null, newAchievements);
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
    LocalDate to = from.plusDays(ROLLING_WINDOW_DAYS - 1);
    LocalDateTime now = utc(clock.instant());
    List<Goal> activeGoals = repository.findActiveGoals();
    Map<Long, Goal> goalsById =
        activeGoals.stream().collect(Collectors.toMap(Goal::getId, Function.identity()));
    List<Action> actions =
        repository.findActionsByGoalIds(activeGoals.stream().map(Goal::getId).toList());
    return generateOccurrences(actions, goalsById, from, to, now);
  }

  /**
   * 为刚激活的计划立即生成滚动窗口内的实例。
   *
   * <p>滚动调度每小时执行一次；若确认计划后等待下一次调度，用户会在「今日行动」看到空列表，
   * 提醒也不会产生。因此在确认事务内按同一规则生成，重复调度由唯一键与去重逻辑兜底。
   */
  private int generateActivatedPlanOccurrences(Goal goal, long planId, LocalDateTime now) {
    List<Action> planActions =
        repository.findActionsByGoalIds(List.of(goal.getId())).stream()
            .filter(action -> action.planVersionId() == planId)
            .filter(action -> action.status() == ActionStatus.ACTIVE)
            .toList();
    if (planActions.isEmpty()) {
      return 0;
    }
    LocalDate from = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    LocalDate to = from.plusDays(ROLLING_WINDOW_DAYS - 1);
    return generateOccurrences(planActions, Map.of(goal.getId(), goal), from, to, now);
  }

  /**
   * 按重复规则展开日期窗口内的实例并只写入新实例。
   *
   * <p>单次调整例外在展开时叠加：被跳过的日期不生成；被改期的日期改为生成在目标日期上，
   * 目标日期在窗口外时也能补进来，且不会重复生成规则本身已覆盖的日期
   * （同一天同一时刻由 {@code uk_goal_occurrence_schedule} 唯一键兜底）。
   */
  private int generateOccurrences(
      List<Action> actions, Map<Long, Goal> goalsById, LocalDate from, LocalDate to, LocalDateTime now) {
    Map<Long, Action> actionsById =
        actions.stream().collect(Collectors.toMap(Action::id, Function.identity()));
    Map<Long, List<ActionException>> exceptionsByAction =
        repository.findActionExceptions(actions.stream().map(Action::id).toList()).stream()
            .collect(Collectors.groupingBy(ActionException::actionId));
    List<ActionOccurrence> occurrences = new ArrayList<>();
    for (Action action : actions) {
      for (LocalDate date : scheduleDates(action, exceptionsByAction.getOrDefault(action.id(), List.of()), from, to)) {
        Instant scheduled =
            date.atTime(action.localTime()).atZone(ZoneId.of(action.timezone())).toInstant();
        occurrences.add(
            ActionOccurrence.schedule(
                idGenerator.nextId(), action.id(), scheduled, date, action.timezone(), now));
      }
    }
    return insertNewOccurrences(occurrences, actionsById, goalsById);
  }

  /**
   * 计算行动在窗口内实际需要生成实例的日期集合。
   *
   * <p>两个方向都要覆盖：窗口内被改期到窗口外，以及窗口外被改期到窗口内。
   * 否则用户会觉得「改期没生效」或「改回来的那一次没有出现」。
   */
  private Set<LocalDate> scheduleDates(
      Action action, List<ActionException> exceptions, LocalDate from, LocalDate to) {
    Map<LocalDate, ActionException> byDate =
        exceptions.stream().collect(Collectors.toMap(ActionException::localDate, Function.identity()));
    Set<LocalDate> dates = new TreeSet<>();
    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      ActionException exception = byDate.get(date);
      if (exception == null) {
        if (action.occursOn(date)) {
          dates.add(date);
        }
        continue;
      }
      if (exception.type() == ActionExceptionType.RESCHEDULE
          && !exception.rescheduledDate().isBefore(from)
          && !exception.rescheduledDate().isAfter(to)) {
        dates.add(exception.rescheduledDate());
      }
    }
    for (ActionException exception : exceptions) {
      if (exception.type() != ActionExceptionType.RESCHEDULE
          || !exception.localDate().isBefore(from)) {
        continue;
      }
      LocalDate target = exception.rescheduledDate();
      if (!target.isBefore(from) && !target.isAfter(to)) {
        dates.add(target);
      }
    }
    return dates;
  }

  /**
   * 只写入尚不存在的实例，并只为新实例发布 {@link OccurrenceScheduledEvent}。
   *
   * <p>滚动调度每小时都会重算同一窗口，若对已存在实例重复发布事件会产生通知噪声；
   * 因此先按 action+scheduledAt 去重再写入。并发下 INSERT IGNORE 仍兜底唯一键，
   * 消费者按实例标识幂等，重复事件不会重复提醒。
   */
  private int insertNewOccurrences(
      List<ActionOccurrence> candidates,
      Map<Long, Action> actionsById,
      Map<Long, Goal> goalsById) {
    if (candidates.isEmpty()) {
      return 0;
    }
    List<Long> actionIds = candidates.stream().map(ActionOccurrence::getActionId).distinct().toList();
    LocalDate from = candidates.stream().map(ActionOccurrence::getLocalDate).min(LocalDate::compareTo).orElseThrow();
    LocalDate to = candidates.stream().map(ActionOccurrence::getLocalDate).max(LocalDate::compareTo).orElseThrow();
    Set<String> existing =
        repository.findOccurrences(actionIds, from, to).stream()
            .map(o -> o.getActionId() + "@" + o.getScheduledAt())
            .collect(Collectors.toSet());
    List<ActionOccurrence> fresh =
        candidates.stream()
            .filter(o -> !existing.contains(o.getActionId() + "@" + o.getScheduledAt()))
            .toList();
    if (fresh.isEmpty()) {
      return 0;
    }
    int inserted = repository.insertOccurrences(fresh);
    for (ActionOccurrence occurrence : fresh) {
      Action action = actionsById.get(occurrence.getActionId());
      if (action == null) {
        continue;
      }
      Goal goal = goalsById.get(action.goalId());
      if (goal == null) {
        continue;
      }
      eventPublisher.publish(
          new OccurrenceScheduledEvent(
              idGenerator.nextEventId(),
              goal.getUserId(),
              goal.getId(),
              action.id(),
              occurrence.getId(),
              action.title(),
              occurrence.getScheduledAt(),
              occurrence.getLocalDate(),
              occurrence.getTimezone()));
    }
    return inserted;
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

  /**
   * 把行动草案展开为行动聚合。
   *
   * <p>前置行动按 clientKey 引用同一份草案里的另一个行动，因此分两遍处理：
   * 第一遍建立 clientKey 到真实标识的映射，第二遍再回填前置引用，
   * 这样客户端不需要提前知道服务端将分配的标识。
   */
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
    Map<String, Long> actionIdsByClientKey = new HashMap<>();
    List<Action> created = new ArrayList<>();
    for (PlanActionDraft draft : drafts) {
      if (isBlank(draft.clientKey()) || !keys.add(draft.clientKey())) {
        throw new BusinessException("GOAL_DUPLICATE_ACTION", "行动客户端标识为空或重复");
      }
      Long milestoneId =
          draft.milestoneSequence() == null ? null : milestoneIds.get(draft.milestoneSequence());
      if (draft.milestoneSequence() != null && milestoneId == null) {
        throw new BusinessException("GOAL_MILESTONE_NOT_FOUND", "行动引用的里程碑不存在");
      }
      long actionId = idGenerator.nextId();
      actionIdsByClientKey.put(draft.clientKey(), actionId);
      created.add(
          new Action(
              actionId,
              goalId,
              planId,
              milestoneId,
              draft.clientKey(),
              draft.title(),
              new ActionSchedule(
                  draft.recurrenceType(),
                  draft.weekdays(),
                  draft.intervalDays(),
                  draft.startDate(),
                  draft.endDate(),
                  draft.localTime(),
                  draft.endLocalTime(),
                  draft.timezone()),
              new ActionDetail(
                  draft.description(),
                  draft.priority(),
                  draft.difficulty(),
                  draft.completionCriteria(),
                  draft.estimatedMinutes(),
                  null,
                  draft.reminderPolicy(),
                  draft.first()),
              initialStatus,
              0,
              now,
              now));
    }
    return wirePrerequisites(created, drafts, actionIdsByClientKey, now);
  }

  /** 回填前置行动引用；引用不存在或指向自身时拒绝，避免产生无法执行的依赖链。 */
  private List<Action> wirePrerequisites(
      List<Action> created,
      List<PlanActionDraft> drafts,
      Map<String, Long> actionIdsByClientKey,
      LocalDateTime now) {
    List<Action> wired = new ArrayList<>(created.size());
    for (int index = 0; index < created.size(); index++) {
      Action action = created.get(index);
      String prerequisiteKey = drafts.get(index).prerequisiteClientKey();
      if (isBlank(prerequisiteKey)) {
        wired.add(action);
        continue;
      }
      Long prerequisiteId = actionIdsByClientKey.get(prerequisiteKey);
      if (prerequisiteId == null) {
        throw new BusinessException("GOAL_PREREQUISITE_NOT_FOUND", "前置行动不在同一份计划中");
      }
      if (prerequisiteId == action.id()) {
        throw new BusinessException("GOAL_INVALID_PREREQUISITE", "行动不能把自己作为前置行动");
      }
      ActionDetail detail = action.detail();
      wired.add(
          action.edited(
              action.title(),
              action.schedule(),
              new ActionDetail(
                  detail.description(),
                  detail.priority(),
                  detail.difficulty(),
                  detail.completionCriteria(),
                  detail.estimatedMinutes(),
                  prerequisiteId,
                  detail.reminderPolicy(),
                  detail.first()),
              now));
    }
    return wired;
  }

  private Goal ownedGoal(long userId, long goalId) {
    Goal goal =
        repository
            .findById(goalId)
            .orElseThrow(() -> new BusinessException("GOAL_NOT_FOUND", "目标不存在"));
    goal.assertOwnedBy(userId);
    return goal;
  }

  /** 按行动标识取行动并校验归属；跨用户访问一律按「不存在」处理，不泄露存在性。 */
  private Action ownedAction(long userId, long actionId) {
    Action action =
        repository
            .findAction(actionId)
            .orElseThrow(() -> new BusinessException("GOAL_ACTION_NOT_FOUND", "行动不存在"));
    ownedGoal(userId, action.goalId());
    return action;
  }

  /** 入参重复规则映射为领域值对象；缺省时区按账号所在地兜底，避免因缺字段直接失败。 */
  private ActionSchedule toSchedule(ActionInput input) {
    return new ActionSchedule(
        input.recurrenceType(),
        input.weekdays(),
        input.intervalDays(),
        input.startDate(),
        input.endDate(),
        input.localTime(),
        input.endLocalTime(),
        isBlank(input.timezone()) ? "Asia/Shanghai" : input.timezone());
  }

  private ActionDetail toDetail(ActionInput input) {
    return new ActionDetail(
        input.description(),
        input.priority(),
        input.difficulty(),
        input.completionCriteria(),
        input.estimatedMinutes(),
        input.prerequisiteActionId(),
        input.reminderPolicy(),
        input.first());
  }

  /**
   * 落库一次行动定义变更并让新规则立刻生效。
   *
   * <p>顺序很关键：先校验前置行动，再按乐观版本写入定义，最后清掉不再符合规则的未来实例并重新生成。
   * 只清「未执行」的实例，已打卡或已跳过的历史事实不动。
   */
  private ActionResult persistActionChange(
      Action before, Action after, long expectedVersion, LocalDateTime now) {
    assertPrerequisiteUsable(after);
    if (!repository.updateAction(after, expectedVersion)) {
      throw new BusinessException("GOAL_ACTION_VERSION_CONFLICT", "行动已被其他终端更新");
    }
    repository.deleteFutureScheduledOccurrences(before.id(), now);
    Goal goal =
        repository
            .findById(after.goalId())
            .orElseThrow(() -> new BusinessException("GOAL_NOT_FOUND", "目标不存在"));
    LocalDate from = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    generateOccurrences(
        List.of(after), Map.of(goal.getId(), goal), from, from.plusDays(ROLLING_WINDOW_DAYS - 1), now);
    return actionResult(after);
  }

  /** 前置行动必须是同一目标下、未取消、且不是自己的行动。 */
  private void assertPrerequisiteUsable(Action action) {
    Long prerequisiteId = action.detail().prerequisiteActionId();
    if (prerequisiteId == null) {
      return;
    }
    if (prerequisiteId == action.id()) {
      throw new BusinessException("GOAL_INVALID_PREREQUISITE", "行动不能把自己作为前置行动");
    }
    Action prerequisite =
        repository
            .findAction(prerequisiteId)
            .orElseThrow(() -> new BusinessException("GOAL_PREREQUISITE_NOT_FOUND", "前置行动不存在"));
    if (prerequisite.goalId() != action.goalId()) {
      throw new BusinessException("GOAL_INVALID_PREREQUISITE", "前置行动必须属于同一个目标");
    }
    if (prerequisite.status() == ActionStatus.CANCELLED) {
      throw new BusinessException("GOAL_INVALID_PREREQUISITE", "前置行动已取消");
    }
  }

  private ActionResult actionResult(Action a) {
    return new ActionResult(
        a.id(),
        a.goalId(),
        a.planVersionId(),
        a.milestoneId(),
        a.clientKey(),
        a.title(),
        a.detail().description(),
        a.recurrenceType(),
        a.weekdays(),
        a.intervalDays(),
        a.startDate(),
        a.endDate(),
        a.localTime(),
        a.endLocalTime(),
        a.timezone(),
        a.detail().priority(),
        a.detail().difficulty(),
        a.detail().completionCriteria(),
        a.detail().estimatedMinutes(),
        a.detail().prerequisiteActionId(),
        a.detail().reminderPolicy(),
        a.status(),
        a.detail().first(),
        a.version());
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
        || c.definition() == null
        || isBlank(c.definition().title())
        || isBlank(c.definition().successCriteria())
        || c.userId() <= 0) {
      throw new BusinessException("GOAL_INVALID_COMMAND", "创建目标参数不完整");
    }
  }

  /** 把入参目标定义映射为领域值对象，并为缺省字段补齐 PRD 规定的默认值。 */
  private GoalDefinition toDefinition(GoalDefinitionInput input) {
    return new GoalDefinition(
        input.title(),
        input.description(),
        input.successCriteria(),
        input.goalType() == null ? GoalType.HABIT : input.goalType(),
        input.startDate(),
        input.targetEndDate(),
        input.priority(),
        input.weeklyAvailableMinutes(),
        input.resourceConstraints(),
        input.verifiableOutcomes(),
        input.privacyLevel());
  }

  /** 生命周期动作对应的目标状态。 */
  private GoalStatus targetStatus(GoalTransition transition) {
    return switch (transition) {
      case PAUSE -> GoalStatus.PAUSED;
      case RESUME -> GoalStatus.ACTIVE;
      case ABANDON -> GoalStatus.ABANDONED;
      case ARCHIVE -> GoalStatus.ARCHIVED;
    };
  }

  /** 预计恢复日期只允许今天或未来；按统一时钟判定，避免依赖机器本地时区。 */
  private void assertResumeDateNotPast(LocalDate expectedResumeDate, LocalDateTime now) {
    if (expectedResumeDate != null && expectedResumeDate.isBefore(now.toLocalDate())) {
      throw new BusinessException("GOAL_INVALID_RESUME_DATE", "预计恢复日期不能早于今天");
    }
  }

  /**
   * 激活计划前校验活跃目标配额。
   *
   * <p>计数排除当前目标本身，因为它此刻可能已是活跃状态（例如修改已生效的计划）；
   * 暂停、草稿、完成与归档的目标不计入上限。
   */
  private void assertQuotaAllowsActivation(Goal goal) {
    int others = repository.countActiveGoals(goal.getUserId(), goal.getId());
    ActiveGoalQuota.assertCanActivate(others, quotaPort.allowanceFor(goal.getUserId()));
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
    GoalDefinition d = g.getDefinition();
    return new GoalResult(
        g.getId(),
        g.getPublicId(),
        g.getUserId(),
        d.title(),
        d.description(),
        d.successCriteria(),
        d.goalType(),
        d.startDate(),
        d.targetEndDate(),
        d.priority(),
        d.weeklyAvailableMinutes(),
        d.resourceConstraints(),
        d.verifiableOutcomes(),
        d.privacyLevel(),
        g.getStatus(),
        g.getCurrentPlanVersionId(),
        g.getProgress(),
        g.getPauseResumeAt(),
        g.getAbandonReason(),
        g.getClarificationStage(),
        g.getVersion());
  }

  private CheckInResult checkInResult(
      CheckIn c,
      ActionOccurrence o,
      int progress,
      boolean corrected,
      List<AchievementResult> newAchievements) {
    CheckInDetail d = c.getDetail();
    return new CheckInResult(
        c.getId(),
        o.getId(),
        c.getResult(),
        o.getStatus().name(),
        progress,
        c.getRecordedAt().toInstant(ZoneOffset.UTC),
        d.note(),
        d.actualMinutes(),
        d.perceivedDifficulty(),
        d.energyLevel(),
        d.moodLevel(),
        d.failureReason(),
        corrected,
        newAchievements);
  }

  private ReviewResult reviewResult(Review r) {
    return new ReviewResult(
        r.getId(),
        r.getGoalId(),
        r.getPeriodKey(),
        r.getStatus().name(),
        r.getConclusionJson(),
        r.getCompletedAt() == null ? null : r.getCompletedAt().toInstant(ZoneOffset.UTC),
        r.getInputSnapshotJson(),
        r.getCreatedAt() == null ? null : r.getCreatedAt().toInstant(ZoneOffset.UTC));
  }

  private OccurrenceResult occurrenceResult(ActionOccurrence o, Action action) {
    return new OccurrenceResult(
        o.getId(),
        o.getActionId(),
        action.title(),
        o.getScheduledAt(),
        o.getLocalDate(),
        o.getTimezone(),
        o.getStatus().name());
  }

  private LocalDateTime utc(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
