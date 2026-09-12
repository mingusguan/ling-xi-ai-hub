package com.lingxi.goal.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lingxi.goal.api.*;
import com.lingxi.goal.domain.*;
import com.lingxi.kernel.DomainEventPublisher;
import com.lingxi.kernel.IdGenerator;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AchievementApplicationServiceTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 12, 10, 0);
  private static final long USER_ID = 9L;

  private GoalRepository goalRepository;
  private AchievementRepository achievementRepository;
  private IdGenerator idGenerator;
  private DomainEventPublisher eventPublisher;
  private AchievementApplicationService service;

  @BeforeEach
  void setUp() {
    goalRepository = mock(GoalRepository.class);
    achievementRepository = mock(AchievementRepository.class);
    idGenerator = mock(IdGenerator.class);
    eventPublisher = mock(DomainEventPublisher.class);
    when(idGenerator.nextId()).thenReturn(1001L);
    when(idGenerator.nextEventId()).thenReturn("event-1");
    when(achievementRepository.insertIfAbsent(any())).thenReturn(true);
    service =
        new AchievementApplicationService(
            goalRepository, achievementRepository, idGenerator, eventPublisher, 7);
  }

  @Test
  void completedGoalGrantsGoalAchievementAndPublishesEvent() {
    // 打卡前目标为 ACTIVE，打卡后进度达到 100 进入 COMPLETED。
    Goal goal = goal(GoalStatus.COMPLETED);
    when(goalRepository.findCompletedCheckInDates(anyLong(), any(), any()))
        .thenReturn(List.of(LocalDate.of(2026, 9, 12)));

    List<AchievementResult> granted = service.grantForCheckIn(context(goal, null), NOW);

    assertThat(granted).hasSize(1);
    assertThat(granted.get(0).type()).isEqualTo(AchievementType.GOAL_COMPLETED);
    ArgumentCaptor<Achievement> saved = ArgumentCaptor.forClass(Achievement.class);
    verify(achievementRepository).insertIfAbsent(saved.capture());
    assertThat(saved.getValue().referenceKey()).isEqualTo("goal-completed:5");
    verify(eventPublisher).publish(any(AchievementEarnedEvent.class));
  }

  @Test
  void alreadyCompletedGoalDoesNotGrantAgain() {
    Goal goal = goal(GoalStatus.COMPLETED);

    List<AchievementResult> granted =
        service.grantForCheckIn(
            new CheckInAchievementContext(
                goal, GoalStatus.COMPLETED, action(41, null), occurrence(), CheckInResultType.COMPLETED),
            NOW);

    assertThat(granted).isEmpty();
    verify(achievementRepository, never()).insertIfAbsent(any());
  }

  @Test
  void repeatedGrantIsSkippedWhenReferenceKeyExists() {
    when(achievementRepository.insertIfAbsent(any())).thenReturn(false);
    when(goalRepository.findCompletedCheckInDates(anyLong(), any(), any())).thenReturn(List.of());

    List<AchievementResult> granted =
        service.grantForCheckIn(context(goal(GoalStatus.COMPLETED), null), NOW);

    assertThat(granted).isEmpty();
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void milestoneGrantedWhenAllOccurrencesCompleted() {
    Goal goal = goal(GoalStatus.ACTIVE);
    Action action = action(41, 31L);
    when(goalRepository.findMilestone(31L))
        .thenReturn(Optional.of(new Milestone(31, 3, 1, "第一周适应", "完成三次行动", NOW)));
    when(goalRepository.findActionsByMilestone(31L)).thenReturn(List.of(action));
    when(goalRepository.findOccurrencesByActionIds(List.of(41L)))
        .thenReturn(List.of(occurrence(51, OccurrenceStatus.COMPLETED)));
    when(goalRepository.findCompletedCheckInDates(anyLong(), any(), any())).thenReturn(List.of());

    List<AchievementResult> granted =
        service.grantForCheckIn(
            new CheckInAchievementContext(
                goal, GoalStatus.ACTIVE, action, occurrence(), CheckInResultType.COMPLETED),
            NOW);

    assertThat(granted).extracting(AchievementResult::type)
        .containsExactly(AchievementType.MILESTONE_COMPLETED);
  }

  @Test
  void streakGrantedForFullWindowAndSkippedWhenWindowHasGap() {
    Goal goal = goal(GoalStatus.ACTIVE);
    LocalDate endDate = LocalDate.of(2026, 9, 12);
    List<LocalDate> fullWindow = new ArrayList<>();
    for (int offset = 0; offset < 7; offset++) {
      fullWindow.add(endDate.minusDays(offset));
    }
    when(goalRepository.findCompletedCheckInDates(anyLong(), any(), any())).thenReturn(fullWindow);

    List<AchievementResult> granted = service.grantForCheckIn(context(goal, null), NOW);
    assertThat(granted).extracting(AchievementResult::type)
        .containsExactly(AchievementType.CHECK_IN_STREAK);

    reset(achievementRepository);
    when(achievementRepository.insertIfAbsent(any())).thenReturn(true);
    when(goalRepository.findCompletedCheckInDates(anyLong(), any(), any()))
        .thenReturn(List.of(endDate, endDate.minusDays(1)));

    assertThat(service.grantForCheckIn(context(goal, null), NOW)).isEmpty();
  }

  @Test
  void partialCheckInDoesNotCountTowardStreak() {
    Goal goal = goal(GoalStatus.ACTIVE);
    LocalDate endDate = LocalDate.of(2026, 9, 12);
    List<LocalDate> fullWindow = new ArrayList<>();
    for (int offset = 0; offset < 7; offset++) {
      fullWindow.add(endDate.minusDays(offset));
    }
    when(goalRepository.findCompletedCheckInDates(anyLong(), any(), any())).thenReturn(fullWindow);

    List<AchievementResult> granted =
        service.grantForCheckIn(
            new CheckInAchievementContext(
                goal, GoalStatus.ACTIVE, action(41, null), occurrence(), CheckInResultType.PARTIAL),
            NOW);

    assertThat(granted).isEmpty();
    verify(goalRepository, never()).findCompletedCheckInDates(anyLong(), any(), any());
  }

  @Test
  void listAchievementsRejectsInvalidPaging() {
    assertThatThrownBy(() -> service.listAchievements(USER_ID, null, 0, 20))
        .hasMessageContaining("成就查询参数不合法");
  }

  private CheckInAchievementContext context(Goal goal, Long milestoneId) {
    return new CheckInAchievementContext(
        goal, GoalStatus.ACTIVE, action(41, milestoneId), occurrence(), CheckInResultType.COMPLETED);
  }

  private static Goal goal(GoalStatus status) {
    return Goal.rehydrate(
        5, "G-5", USER_ID, "goal-key", "digest", "每日阅读", "连续阅读 30 天", status, 3L, 100, 4, NOW, NOW);
  }

  private static Action action(long id, Long milestoneId) {
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

  private static ActionOccurrence occurrence() {
    return occurrence(51, OccurrenceStatus.COMPLETED);
  }

  private static ActionOccurrence occurrence(long id, OccurrenceStatus status) {
    ActionOccurrence occurrence =
        ActionOccurrence.schedule(
            id,
            41,
            Instant.parse("2026-09-12T13:00:00Z"),
            LocalDate.of(2026, 9, 12),
            "Asia/Shanghai",
            NOW);
    if (status == OccurrenceStatus.COMPLETED) {
      occurrence.checkIn(CheckInResultType.COMPLETED, NOW);
    }
    return occurrence;
  }
}
