package com.lingxi.engagement.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.engagement.domain.EngagementRepository;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.kernel.PublishedEventMessage;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GoalSyncProjectionHandlerTest {

  private final EngagementRepository repository = mock(EngagementRepository.class);
  private final IdGenerator ids = mock(IdGenerator.class);
  private final ObjectMapper mapper = new ObjectMapper();

  private final GoalSyncProjectionHandler handler =
      new GoalSyncProjectionHandler(repository, ids, mapper);

  @Test
  void supportsOnlyGoalDomainEvents() {
    assertThat(handler.supports("goal.created")).isTrue();
    assertThat(handler.supports("goal.plan-activated")).isTrue();
    assertThat(handler.supports("goal.action-checked-in")).isTrue();
    assertThat(handler.supports("goal.review-completed")).isTrue();
    assertThat(handler.supports("identity.user-registered")).isFalse();
    assertThat(handler.supports("engagement.task")).isFalse();
  }

  @Test
  void projectsCheckedInEventToGoalDomainCursor() {
    when(ids.nextId()).thenReturn(1L, 2L);
    handler.handle(
        new PublishedEventMessage(
            "evt-1",
            "goal.action-checked-in",
            "99",
            7,
            1,
            Instant.parse("2026-08-11T01:00:00Z"),
            "{\"eventId\":\"evt-1\",\"userId\":42,\"goalId\":99,\"occurrenceId\":555,"
                + "\"result\":\"COMPLETED\",\"aggregateVersion\":7}"));

    verify(repository)
        .insertChange(
            eq(1L),
            eq(42L),
            eq(2L),
            eq("goal"),
            eq("goal"),
            eq("99"),
            eq(7L),
            eq("CHECKED_IN"),
            anyString(),
            any());
  }

  @Test
  void projectsPlanActivatedToGoalResource() {
    when(ids.nextId()).thenReturn(10L, 11L);
    handler.handle(
        new PublishedEventMessage(
            "evt-2",
            "goal.plan-activated",
            "99",
            3,
            1,
            Instant.parse("2026-08-11T02:00:00Z"),
            "{\"eventId\":\"evt-2\",\"userId\":42,\"goalId\":99,\"planVersionId\":88,"
                + "\"aggregateVersion\":3}"));

    verify(repository)
        .insertChange(
            eq(10L), eq(42L), eq(11L), eq("goal"), eq("goal"), eq("99"), eq(3L),
            eq("PLAN_ACTIVATED"), anyString(), any());
  }

  @Test
  void skipsEventWithoutUserId() {
    handler.handle(
        new PublishedEventMessage(
            "evt-old",
            "goal.created",
            "99",
            1,
            1,
            Instant.parse("2026-08-01T00:00:00Z"),
            "{\"eventId\":\"evt-old\",\"goalId\":99,\"aggregateVersion\":1}"));
    verify(repository, never()).insertChange(anyLong(), anyLong(), anyLong(), anyString(),
        anyString(), anyString(), anyLong(), anyString(), anyString(), any());
  }
}
