package com.lingxi.identity.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/** 年龄、监护、协议或账号状态变化后的授权失效事件。 */
public record AuthorizationChangedEvent(
    String eventId,
    long userId,
    AccountStatus status,
    long authorizationVersion,
    String reason,
    long aggregateVersion,
    Instant occurredAt)
    implements DomainEvent {
  @Override
  public String eventType() {
    return "identity.authorization-changed";
  }

  @Override
  public String aggregateId() {
    return Long.toString(userId);
  }

  @Override
  public int schemaVersion() {
    return 1;
  }
}
