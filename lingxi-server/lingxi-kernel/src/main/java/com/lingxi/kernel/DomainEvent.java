package com.lingxi.kernel;

import java.time.Instant;

/** 模块间持久化领域事件的稳定契约。 */
public interface DomainEvent {

  String eventId();

  String eventType();

  String aggregateId();

  long aggregateVersion();

  int schemaVersion();

  Instant occurredAt();
}
