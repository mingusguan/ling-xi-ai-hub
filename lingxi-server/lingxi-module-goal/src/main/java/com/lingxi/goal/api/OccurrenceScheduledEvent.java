package com.lingxi.goal.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 行动实例生成事件。
 *
 * <p>仅在实例真正新建时发布，供 engagement 按用户时区、免打扰与年龄策略生成提醒任务；
 * 重复调度不会重复发布，消费者仍须按实例标识幂等。
 *
 * @param eventId 事件标识
 * @param userId 目标归属用户
 * @param goalId 目标标识
 * @param actionId 行动标识
 * @param occurrenceId 行动实例标识
 * @param actionTitle 行动标题，用于生成提醒文案
 * @param scheduledAt 计划执行时刻
 * @param localDate 用户本地日期
 * @param timezone 行动时区
 */
public record OccurrenceScheduledEvent(
    String eventId,
    long userId,
    long goalId,
    long actionId,
    long occurrenceId,
    String actionTitle,
    Instant scheduledAt,
    LocalDate localDate,
    String timezone)
    implements DomainEvent {
  @Override
  public String eventType() {
    return "goal.occurrence-scheduled";
  }

  @Override
  public String aggregateId() {
    return Long.toString(occurrenceId);
  }

  @Override
  public long aggregateVersion() {
    return 0;
  }

  @Override
  public int schemaVersion() {
    return 1;
  }

  @Override
  public Instant occurredAt() {
    return scheduledAt;
  }
}
