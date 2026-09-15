package com.lingxi.goal.api;

import java.time.Instant;

/**
 * 当前有效打卡记录视图。
 *
 * <p>用于「更正」入口：修正前必须先知道当前记录了什么，否则用户只能盲改。
 * 只返回当前有效记录，已被修正替代的历史记录不出现在这里。
 *
 * @param actualMinutes 实际耗时（分钟）
 * @param perceivedDifficulty 主观难度
 * @param energyLevel 精力自评
 * @param moodLevel 情绪自评
 * @param failureReason 失败原因；仅结果为失败时非空
 */
public record CheckInView(
    long checkInId,
    long occurrenceId,
    CheckInResultType result,
    String note,
    String evidenceReference,
    Integer actualMinutes,
    ActionDifficulty perceivedDifficulty,
    EnergyLevel energyLevel,
    MoodLevel moodLevel,
    String failureReason,
    Instant recordedAt) {}
