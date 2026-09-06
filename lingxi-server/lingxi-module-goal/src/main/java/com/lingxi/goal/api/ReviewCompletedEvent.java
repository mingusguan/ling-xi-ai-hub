package com.lingxi.goal.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/** 目标复盘完成事件。 */
public record ReviewCompletedEvent(
    String eventId,
    long userId,
    long goalId,
    long reviewId,
    long aggregateVersion,
    Instant occurredAt)
    implements DomainEvent {
  @Override
  public String eventType() {
    return "goal.review-completed";
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
