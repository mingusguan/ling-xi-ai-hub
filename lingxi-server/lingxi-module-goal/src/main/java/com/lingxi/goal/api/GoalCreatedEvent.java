package com.lingxi.goal.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/** 目标创建事件。 */
public record GoalCreatedEvent(
    String eventId, long goalId, long userId, long aggregateVersion, Instant occurredAt)
    implements DomainEvent {
  @Override
  public String eventType() {
    return "goal.created";
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
