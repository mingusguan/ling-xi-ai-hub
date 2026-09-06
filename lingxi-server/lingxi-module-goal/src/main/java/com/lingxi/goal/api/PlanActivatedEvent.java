package com.lingxi.goal.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/** 计划版本激活事件。 */
public record PlanActivatedEvent(
    String eventId, long userId, long goalId, long planVersionId, long aggregateVersion, Instant occurredAt)
    implements DomainEvent {
  @Override
  public String eventType() {
    return "goal.plan-activated";
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
