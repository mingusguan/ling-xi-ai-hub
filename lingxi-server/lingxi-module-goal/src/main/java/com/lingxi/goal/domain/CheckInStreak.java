package com.lingxi.goal.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * 连续打卡判定输入。
 *
 * @param checkInDate 本次打卡对应的本地日期
 * @param completedDates 用户当前有效行动下已完成打卡的本地日期集合
 * @param thresholdDays 连续打卡阈值天数
 */
public record CheckInStreak(LocalDate checkInDate, Set<LocalDate> completedDates, int thresholdDays) {

  public CheckInStreak {
    Objects.requireNonNull(checkInDate, "打卡日期不能为空");
    completedDates = completedDates == null ? Set.of() : Set.copyOf(completedDates);
    if (thresholdDays < 2) {
      throw new IllegalArgumentException("连续打卡阈值必须大于 1 天");
    }
  }
}
