package com.lingxi.goal.api;

import com.lingxi.kernel.DomainEvent;
import java.time.Instant;

/**
 * 成就达成事件。
 *
 * @param eventId 事件标识
 * @param achievementId 成就标识
 * @param userId 成就归属用户
 * @param goalId 关联目标标识；非目标成就为 null
 * @param achievementType 成就类型
 * @param aggregateVersion 触发成就时的目标版本；非目标成就为 0
 * @param occurredAt 事件发生时间
 */
public record AchievementEarnedEvent(
    String eventId,
    long achievementId,
    long userId,
    Long goalId,
    AchievementType achievementType,
    long aggregateVersion,
    Instant occurredAt)
    implements DomainEvent {
  @Override
  public String eventType() {
    return "goal.achievement-earned";
  }

  @Override
  public String aggregateId() {
    return Long.toString(achievementId);
  }

  @Override
  public int schemaVersion() {
    return 1;
  }
}
