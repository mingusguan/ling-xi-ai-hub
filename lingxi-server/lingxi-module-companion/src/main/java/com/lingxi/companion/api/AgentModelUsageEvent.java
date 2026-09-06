package com.lingxi.companion.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/** 模型调用的非敏感计量事件，不包含用户输入或模型正文。 */
public record AgentModelUsageEvent(
    String eventId,
    long runId,
    String scene,
    String ageBand,
    String modelVersion,
    String promptVersion,
    long latencyMillis,
    long inputTokens,
    long outputTokens,
    long costMinor,
    Instant occurredAt)
    implements DomainEvent {

  @Override
  public String eventType() {
    return "companion.model-used.v1";
  }

  @Override
  public String aggregateId() {
    return String.valueOf(runId);
  }

  @Override
  public long aggregateVersion() {
    return 0;
  }

  @Override
  public int schemaVersion() {
    return 1;
  }
}
