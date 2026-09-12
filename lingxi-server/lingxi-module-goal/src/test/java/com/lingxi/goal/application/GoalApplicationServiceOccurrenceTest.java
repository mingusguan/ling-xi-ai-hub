package com.lingxi.goal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lingxi.goal.api.ConfirmPlanCommand;
import com.lingxi.goal.api.GoalStatus;
import com.lingxi.goal.api.OccurrenceScheduledEvent;
import com.lingxi.goal.api.PlanActionDraft;
import com.lingxi.goal.api.RecurrenceType;
import com.lingxi.goal.domain.Action;
import com.lingxi.goal.domain.ActionOccurrence;
import com.lingxi.goal.domain.ActionStatus;
import com.lingxi.goal.domain.Goal;
import com.lingxi.goal.domain.GoalRepository;
import com.lingxi.goal.domain.PlanVersion;
import com.lingxi.identity.api.AccessProfile;
import com.lingxi.identity.api.AccountStatus;
import com.lingxi.identity.api.AgeBand;
import com.lingxi.identity.api.IdentityFacade;
import com.lingxi.kernel.DomainEvent;
import com.lingxi.kernel.DomainEventPublisher;
import com.lingxi.kernel.IdGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 计划激活后立即生成行动实例的行为验证。
 *
 * <p>目标模块的实例生成既可由每小时滚动调度触发，也可在确认计划时立即触发；本测试只覆盖后者，
 * 并复用同一段去重与事件发布逻辑，保证两条入口行为一致。
 */
class GoalApplicationServiceOccurrenceTest {
  private static final long USER_ID = 9L;
  private static final long GOAL_ID = 5L;
  private static final long ACTION_ID = 41L;

  private GoalRepository repository;
  private IdGenerator idGenerator;
  private DomainEventPublisher eventPublisher;
  private GoalApplicationService service;
  /** 本次确认实际写入的计划版本，实例只应针对该版本生成。 */
  private final AtomicReference<PlanVersion> activatedPlan = new AtomicReference<>();

  @BeforeEach
  void setUp() {
    repository = mock(GoalRepository.class);
    IdentityFacade identityFacade = mock(IdentityFacade.class);
    idGenerator = mock(IdGenerator.class);
    eventPublisher = mock(DomainEventPublisher.class);
    AchievementApplicationService achievements = mock(AchievementApplicationService.class);
    activatedPlan.set(null);

    AtomicLong nextId = new AtomicLong(1000L);
    when(idGenerator.nextId()).thenAnswer(invocation -> nextId.incrementAndGet());
    when(idGenerator.nextEventId()).thenAnswer(invocation -> "event-" + nextId.incrementAndGet());
    when(identityFacade.getAccessProfile(USER_ID))
        .thenReturn(new AccessProfile(USER_ID, AgeBand.ADULT, AccountStatus.ACTIVE_ADULT, 1, true));
    // 计划激活成功是后续实例生成的前提。
    when(repository.updateActivatedPlan(any(), anyLong())).thenReturn(true);
    // 记录本次写入的计划版本，供行动查询按真实计划标识返回。
    org.mockito.Mockito.doAnswer(
            invocation -> {
              activatedPlan.set(invocation.getArgument(0));
              return null;
            })
        .when(repository)
        .insertPlanBundle(any(), any(), any());

    service =
        new GoalApplicationService(
            repository,
            identityFacade,
            idGenerator,
            eventPublisher,
            achievements,
            new ObjectMapper().registerModule(new JavaTimeModule()),
            Clock.fixed(Instant.parse("2026-09-12T02:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void confirmPlanWritesRollingWindowOccurrencesAndPublishesScheduledEvents() {
    stubConfirmation(1, List.of());

    service.confirmPlan(command());

    ArgumentCaptor<List<ActionOccurrence>> inserted = ArgumentCaptor.forClass(List.class);
    verify(repository).insertOccurrences(inserted.capture());
    // 窗口为当日 UTC 起 14 天，DAILY 行动应生成 14 个实例。
    assertThat(inserted.getValue()).hasSize(14);
    assertThat(inserted.getValue().get(0).getLocalDate()).isEqualTo(LocalDate.of(2026, 9, 12));
    assertThat(inserted.getValue()).extracting(ActionOccurrence::getActionId).containsOnly(ACTION_ID);

    ArgumentCaptor<DomainEvent> events = ArgumentCaptor.forClass(DomainEvent.class);
    verify(eventPublisher, org.mockito.Mockito.atLeast(15)).publish(events.capture());
    List<OccurrenceScheduledEvent> scheduled =
        events.getAllValues().stream()
            .filter(OccurrenceScheduledEvent.class::isInstance)
            .map(OccurrenceScheduledEvent.class::cast)
            .toList();
    assertThat(scheduled).hasSize(14);
    assertThat(scheduled).extracting(OccurrenceScheduledEvent::userId).containsOnly(USER_ID);
    assertThat(scheduled).extracting(OccurrenceScheduledEvent::goalId).containsOnly(GOAL_ID);
  }

  @Test
  void confirmPlanSkipsActionsThatAlreadyHaveOccurrences() {
    // 已存在的实例按 action+scheduledAt 去重，不重复写库、不重复发布事件。
    List<ActionOccurrence> existing = new ArrayList<>();
    for (int offset = 0; offset < 14; offset++) {
      LocalDate date = LocalDate.of(2026, 9, 12).plusDays(offset);
      existing.add(occurrence(ACTION_ID, date, 500 + offset));
    }
    stubConfirmation(1, existing);

    service.confirmPlan(command());

    verify(repository, never()).insertOccurrences(any());
    verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.isA(OccurrenceScheduledEvent.class));
  }

  @Test
  void confirmPlanIgnoresActionsFromOtherPlanVersions() {
    // 同一目标的旧版本行动不应因为新计划确认而补生成实例。
    stubConfirmationWithForeignActionOnly();

    service.confirmPlan(command());

    verify(repository, never()).insertOccurrences(any());
    verify(repository, never()).findOccurrences(any(), any(), any());
    verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.isA(OccurrenceScheduledEvent.class));
  }

  @Test
  void confirmPlanWithoutActionsWritesNoOccurrences() {
    stubConfirmation(1, List.of(), false);

    service.confirmPlan(commandWithNoActions());

    verify(repository, never()).insertOccurrences(any());
    verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.isA(OccurrenceScheduledEvent.class));
  }

  /** 桩：新计划确认成功，行动查询返回属于本次写入计划版本的真实行动。 */
  private void stubConfirmation(int planVersionNo, List<ActionOccurrence> existing) {
    stubConfirmation(planVersionNo, existing, true);
  }

  /**
   * 桩：新计划确认成功。
   *
   * <p>计划版本标识由 IdGenerator 在调用时分配，因此行动查询按“本次写入的计划版本”动态构造；
   * {@code includeActivatedAction=false} 时返回空行动列表，用于验证无行动时不会生成实例。
   */
  private void stubConfirmation(
      int planVersionNo, List<ActionOccurrence> existing, boolean includeActivatedAction) {
    when(repository.findById(GOAL_ID)).thenReturn(Optional.of(goal(GoalStatus.DRAFT)));
    when(repository.findPlanByRequestKey("req-1")).thenReturn(Optional.empty());
    when(repository.nextPlanVersionNo(GOAL_ID)).thenReturn(planVersionNo);
    when(repository.findActionsByGoalIds(List.of(GOAL_ID)))
        .thenAnswer(
            invocation -> {
              PlanVersion plan = activatedPlan.get();
              List<Action> actions = new ArrayList<>();
              if (includeActivatedAction && plan != null) {
                actions.add(action(ACTION_ID, plan.id()));
              }
              return actions;
            });
    when(repository.findOccurrences(any(), any(), any())).thenReturn(existing);
    when(repository.insertOccurrences(any()))
        .thenAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size());
  }

  /**
   * 桩：行动查询只返回旧版本计划下的行动。
   *
   * <p>用于验证跨版本行动不会因为新计划确认而补生成实例。
   */
  private void stubConfirmationWithForeignActionOnly() {
    when(repository.findById(GOAL_ID)).thenReturn(Optional.of(goal(GoalStatus.DRAFT)));
    when(repository.findPlanByRequestKey("req-1")).thenReturn(Optional.empty());
    when(repository.nextPlanVersionNo(GOAL_ID)).thenReturn(2);
    when(repository.findActionsByGoalIds(List.of(GOAL_ID)))
        .thenAnswer(
            invocation -> {
              PlanVersion plan = activatedPlan.get();
              // 旧版本计划下的同名行动，实例生成必须忽略它。
              return List.of(action(ACTION_ID, plan == null ? 1L : plan.id() - 1));
            });
    when(repository.insertOccurrences(any()))
        .thenAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size());
  }

  private static ActionOccurrence occurrence(long actionId, LocalDate date, long id) {
    return ActionOccurrence.schedule(
        id,
        actionId,
        date.atTime(LocalTime.of(21, 0)).atZone(ZoneId.of("Asia/Shanghai")).toInstant(),
        date,
        "Asia/Shanghai",
        LocalDateTime.of(2026, 9, 12, 2, 0));
  }

  private ConfirmPlanCommand command() {
    return new ConfirmPlanCommand(
        "req-1",
        USER_ID,
        GOAL_ID,
        0,
        "{\"source\":\"TEST\"}",
        null,
        List.of(),
        List.of(
            new PlanActionDraft(
                "client-41",
                null,
                "每日复盘",
                RecurrenceType.DAILY,
                Set.of(),
                LocalDate.of(2026, 9, 12),
                null,
                LocalTime.of(21, 0),
                "Asia/Shanghai")));
  }

  private ConfirmPlanCommand commandWithNoActions() {
    return new ConfirmPlanCommand(
        "req-1", USER_ID, GOAL_ID, 0, "{\"source\":\"TEST\"}", null, List.of(), List.of());
  }

  private static Goal goal(GoalStatus status) {
    return Goal.rehydrate(
        GOAL_ID, "G-5", USER_ID, "goal-key", "digest", "阅读", "读完 12 本", status, null, 0, 0,
        LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 1, 0, 0));
  }

  private static Action action(long id, long planVersionId) {
    return new Action(
        id,
        GOAL_ID,
        planVersionId,
        null,
        "client-" + id,
        "每日复盘",
        RecurrenceType.DAILY,
        Set.of(),
        LocalDate.of(2026, 9, 12),
        null,
        LocalTime.of(21, 0),
        "Asia/Shanghai",
        ActionStatus.ACTIVE,
        0,
        LocalDateTime.of(2026, 9, 1, 0, 0),
        LocalDateTime.of(2026, 9, 1, 0, 0));
  }
}
