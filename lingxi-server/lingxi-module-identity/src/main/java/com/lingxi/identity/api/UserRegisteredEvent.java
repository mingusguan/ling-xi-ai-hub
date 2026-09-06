package com.lingxi.identity.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/** 用户完成可信年龄登记后的领域事件。 */
public record UserRegisteredEvent(
    String eventId,
    long userId,
    String publicId,
    AgeBand ageBand,
    AccountStatus status,
    long aggregateVersion,
    Instant occurredAt)
    implements DomainEvent {

  @Override
  public String eventType() {
    return "identity.user-registered";
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
