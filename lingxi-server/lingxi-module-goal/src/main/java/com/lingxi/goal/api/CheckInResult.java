package com.lingxi.goal.api;

import java.time.Instant;
import java.util.List;

/**
 * 行动打卡结果视图。
 *
 * @param checkInId 打卡标识
 * @param occurrenceId 行动实例标识
 * @param result 本次打卡结果
 * @param occurrenceStatus 行动实例最新状态
 * @param goalProgress 目标最新进度
 * @param recordedAt 打卡记录时间
 * @param newAchievements 本次打卡新获得的成就；重复提交或未触发成就时为空列表
 */
public record CheckInResult(
    long checkInId,
    long occurrenceId,
    CheckInResultType result,
    String occurrenceStatus,
    int goalProgress,
    Instant recordedAt,
    List<AchievementResult> newAchievements) {

  public CheckInResult {
    newAchievements = newAchievements == null ? List.of() : List.copyOf(newAchievements);
  }
}
