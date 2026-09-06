package com.lingxi.identity.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/** identity 管理命令的不可变审计事实，由 operations 幂等落入统一审计表。 */
public record AdminManagementAuditedEvent(String eventId, long operatorAdminId, String action,
    String objectType, String objectId, long aggregateVersion, String reason, String ticketNo,
    Instant occurredAt) implements DomainEvent {
  public String eventType(){return "identity.admin-management-audited.v1";}
  public String aggregateId(){return objectType+":"+objectId;}
  public int schemaVersion(){return 1;}
}
