package com.lingxi.goal.api;

import java.time.Instant;
import java.util.List;

/**
 * 行动打卡结果视图。
 *
 * <p>除最新状态外一并回显本次记录的内容，客户端可以就地更新「已记录」展示，
 * 不必再发一次查询。
 *
 * @param checkInId 打卡标识
 * @param occurrenceId 行动实例标识
 * @param result 本次打卡结果
 * @param occurrenceStatus 行动实例最新状态
 * @param goalProgress 目标最新进度
 * @param recordedAt 打卡记录时间
 * @param note 备注
 * @param actualMinutes 实际耗时（分钟）
 * @param perceivedDifficulty 主观难度
 * @param energyLevel 精力自评
 * @param moodLevel 情绪自评
 * @param failureReason 失败原因；仅结果为失败时非空
 * @param corrected 本次是否为修正：为 true 时原记录已被标记失效并保留为历史
 * @param newAchievements 本次打卡新获得的成就；重复提交或未触发成就时为空列表
 */
public record CheckInResult(
    long checkInId,
    long occurrenceId,
    CheckInResultType result,
    String occurrenceStatus,
    int goalProgress,
    Instant recordedAt,
    String note,
    Integer actualMinutes,
    ActionDifficulty perceivedDifficulty,
    EnergyLevel energyLevel,
    MoodLevel moodLevel,
    String failureReason,
    boolean corrected,
    List<AchievementResult> newAchievements) {

  public CheckInResult {
    newAchievements = newAchievements == null ? List.of() : List.copyOf(newAchievements);
  }
}
