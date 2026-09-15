package com.lingxi.goal.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/**
 * 目标生命周期状态变更事件。
 *
 * <p>暂停、恢复、放弃与归档都是用户可感知的状态变化，需要用于触达抑制与后续运营分析，
 * 因此统一发布同一个事件，由 {@code toStatus} 区分动作。
 */
public record GoalStatusChangedEvent(
    String eventId,
    long goalId,
    long userId,
    GoalStatus fromStatus,
    GoalStatus toStatus,
    long aggregateVersion,
    Instant occurredAt)
    implements DomainEvent {
  @Override
  public String eventType() {
    return "goal.status-changed";
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
