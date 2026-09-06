package com.lingxi.identity.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/** 跨业务模块统一发布的管理员操作审计事实。 */
public record AdminActionAuditedEvent(
    String eventId,
    long operatorAdminId,
    String action,
    String objectType,
    String objectId,
    long aggregateVersion,
    String reason,
    String ticketNo,
    Instant occurredAt) implements DomainEvent {

  @Override
  public String eventType() {
    return "admin-action-audited.v1";
  }

  @Override
  public String aggregateId() {
    return objectType + ":" + objectId;
  }

  @Override
  public int schemaVersion() {
    return 1;
  }
}
