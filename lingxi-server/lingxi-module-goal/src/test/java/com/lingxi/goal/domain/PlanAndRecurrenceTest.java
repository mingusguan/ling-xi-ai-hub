package com.lingxi.goal.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.goal.api.RecurrenceType;
import com.lingxi.kernel.BusinessException;
import java.time.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanAndRecurrenceTest {
  @Test
  void draftActivationCreatesNewImmutableValue() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    PlanVersion draft = PlanVersion.draft(1, 2, 1, "key", "digest", "{}", null, "AGENT", now);
    PlanVersion active = draft.activate(now.plusMinutes(1));
    assertThat(draft.status()).isEqualTo(PlanVersionStatus.PENDING_CONFIRMATION);
    assertThat(draft.activatedAt()).isNull();
    assertThat(active.status()).isEqualTo(PlanVersionStatus.ACTIVE);
  }

  @Test
  void daylightSavingGapResolvesToSingleValidInstant() {
    Action action =
        new Action(
            1,
            2,
            3,
            null,
            "client",
            "晨间行动",
            RecurrenceType.ONCE,
            Set.of(),
            LocalDate.of(2026, 3, 8),
            null,
            LocalTime.of(2, 30),
            "America/New_York",
            ActionStatus.ACTIVE,
            0,
            LocalDateTime.now(),
            LocalDateTime.now());
    ZonedDateTime resolved =
        action.startDate().atTime(action.localTime()).atZone(ZoneId.of(action.timezone()));
    assertThat(resolved.toLocalTime()).isEqualTo(LocalTime.of(3, 30));
    assertThat(resolved.toInstant()).isEqualTo(Instant.parse("2026-03-08T07:30:00Z"));
  }

  @Test
  void correctionKeepsHistoricalCheckInAndChangesOccurrenceResult() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    ActionOccurrence occurrence =
        ActionOccurrence.schedule(
            1,
            2,
            Instant.parse("2026-08-05T02:00:00Z"),
            LocalDate.of(2026, 8, 5),
            "Asia/Shanghai",
            now);
    CheckIn original =
        CheckIn.record(
            3, 1, 9, "first", "d1", com.lingxi.goal.api.CheckInResultType.PARTIAL, null, null, now);
    original.supersede();
    occurrence.checkIn(com.lingxi.goal.api.CheckInResultType.COMPLETED, now.plusMinutes(1));
    CheckIn correction =
        CheckIn.record(
            4,
            1,
            9,
            "second",
            "d2",
            com.lingxi.goal.api.CheckInResultType.COMPLETED,
            null,
            null,
            now.plusMinutes(1));
    assertThat(original.isEffective()).isFalse();
    assertThat(correction.isEffective()).isTrue();
    assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.COMPLETED);
  }

  @Test
  void completedReviewIsIdempotentButCannotBeOverwritten() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    Review review = Review.schedule(1, 2, "2026-W32", "{}", now);
    review.complete("key", "digest", "{\"done\":true}", now);
    review.complete("key", "digest", "{\"done\":true}", now);
    assertThat(review.getVersion()).isEqualTo(1);
    assertThatThrownBy(() -> review.complete("other", "other", "{}", now))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_REVIEW_ALREADY_COMPLETED");
  }

  @Test
  void weeklyActionRequiresWeekdays() {
    assertThatThrownBy(
            () ->
                new Action(
                    1,
                    2,
                    3,
                    null,
                    "client",
                    "行动",
                    RecurrenceType.WEEKLY,
                    Set.of(),
                    LocalDate.now(),
                    null,
                    LocalTime.NOON,
                    "Asia/Shanghai",
                    ActionStatus.DRAFT,
                    0,
                    LocalDateTime.now(),
                    LocalDateTime.now()))
        .isInstanceOf(BusinessException.class);
  }
}
