package com.lingxi.companion.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/** Agent 安全策略命中的最小披露业务事实，不包含用户原始输入。 */
public record SafetyRiskDetectedEvent(
    String eventId,
    long safetyEventId,
    long runId,
    long userId,
    String riskType,
    String riskLevel,
    String disclosureScope,
    Instant occurredAt) implements DomainEvent {
  @Override public String eventType() { return "companion.safety-risk-detected.v1"; }
  @Override public String aggregateId() { return String.valueOf(safetyEventId); }
  @Override public long aggregateVersion() { return 0; }
  @Override public int schemaVersion() { return 1; }
}
