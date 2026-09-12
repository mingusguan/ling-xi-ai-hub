package com.lingxi.goal.api;

import java.time.Instant;

/**
 * 成就展示视图。
 *
 * @param achievementId 成就标识
 * @param goalId 关联目标标识；连续打卡等非目标成就为 null
 * @param type 成就类型
 * @param title 成就名称
 * @param description 成就说明，已包含触发时的业务事实
 * @param achievedAt 成就达成时间
 */
public record AchievementResult(
    long achievementId,
    Long goalId,
    AchievementType type,
    String title,
    String description,
    Instant achievedAt) {}
