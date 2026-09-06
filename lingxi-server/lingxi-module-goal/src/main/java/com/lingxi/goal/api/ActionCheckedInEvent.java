package com.lingxi.goal.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/** 行动实例打卡事件。 */
public record ActionCheckedInEvent(
    String eventId,
    long userId,
    long goalId,
    long occurrenceId,
    CheckInResultType result,
    long aggregateVersion,
    Instant occurredAt)
    implements DomainEvent {
  @Override
  public String eventType() {
    return "goal.action-checked-in";
  }

  @Override
  public String aggregateId() {
    return Long.toString(goalId);
  }

  @Override
  public int schemaVersion() {
    return 1;
  }
}
