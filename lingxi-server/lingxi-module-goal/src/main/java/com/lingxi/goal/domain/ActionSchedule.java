package com.lingxi.goal.domain;

import com.lingxi.goal.api.RecurrenceType;
import com.lingxi.kernel.BusinessException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * 行动的时间与重复规则值对象。
 *
 * <p>把「什么时候做、按什么规则重复」从行动聚合里整体提出来后，重复规则的校验只存在一处，
 * 单次调整与未来全部调整也能以不可变值对象整体替换，不必逐字段漂移。
 */
public record ActionSchedule(
    RecurrenceType recurrenceType,
    Set<DayOfWeek> weekdays,
    Integer intervalDays,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime localTime,
    LocalTime endLocalTime,
    String timezone) {
  /** 间隔重复允许的最小与最大间隔天数，避免产生「每天」或超过一年的无效规则。 */
  private static final int MIN_INTERVAL_DAYS = 1;

  private static final int MAX_INTERVAL_DAYS = 365;

  public ActionSchedule {
    if (recurrenceType == null || startDate == null || localTime == null || timezone == null) {
      throw new BusinessException("GOAL_INVALID_ACTION", "行动时间与重复规则不完整");
    }
    ZoneId.of(timezone);
    weekdays = weekdays == null ? Set.of() : Set.copyOf(weekdays);
    if (recurrenceType == RecurrenceType.WEEKLY && weekdays.isEmpty()) {
      throw new BusinessException("GOAL_WEEKDAYS_REQUIRED", "每周行动必须指定星期");
    }
    if (recurrenceType == RecurrenceType.INTERVAL) {
      if (intervalDays == null) {
        throw new BusinessException("GOAL_INTERVAL_REQUIRED", "间隔重复行动必须指定间隔天数");
      }
      if (intervalDays < MIN_INTERVAL_DAYS || intervalDays > MAX_INTERVAL_DAYS) {
        throw new BusinessException(
            "GOAL_INVALID_INTERVAL",
            "间隔天数必须在 " + MIN_INTERVAL_DAYS + " 到 " + MAX_INTERVAL_DAYS + " 之间");
      }
    } else {
      // 非间隔重复不允许残留间隔天数，避免仓储与规则判定出现两套事实。
      intervalDays = null;
    }
    if (endDate != null && endDate.isBefore(startDate)) {
      throw new BusinessException("GOAL_INVALID_ACTION_RANGE", "行动结束日期早于开始日期");
    }
    if (endLocalTime != null && !endLocalTime.isAfter(localTime)) {
      throw new BusinessException("GOAL_INVALID_ACTION_WINDOW", "行动时间段结束时间必须晚于开始时间");
    }
  }

  /** 判断行动在给定本地日期是否发生；不包含单次调整例外，例外由行动聚合叠加判断。 */
  public boolean occursOn(LocalDate date) {
    if (date.isBefore(startDate) || (endDate != null && date.isAfter(endDate))) {
      return false;
    }
    return switch (recurrenceType) {
      case ONCE -> date.equals(startDate);
      case DAILY -> true;
      case WEEKLY -> weekdays.contains(date.getDayOfWeek());
      case WEEKDAYS ->
          date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
      case INTERVAL -> ChronoUnit.DAYS.between(startDate, date) % intervalDays == 0;
    };
  }
}
