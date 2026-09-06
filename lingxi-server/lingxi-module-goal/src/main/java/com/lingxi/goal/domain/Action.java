package com.lingxi.goal.domain;

import com.lingxi.goal.api.RecurrenceType;
import com.lingxi.kernel.BusinessException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

/** 行动定义聚合，生效后重复规则不可原地修改。 */
public record Action(
    long id,
    long goalId,
    long planVersionId,
    Long milestoneId,
    String clientKey,
    String title,
    RecurrenceType recurrenceType,
    Set<DayOfWeek> weekdays,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime localTime,
    String timezone,
    ActionStatus status,
    long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public Action {
    if (id <= 0
        || goalId <= 0
        || planVersionId <= 0
        || title == null
        || title.isBlank()
        || startDate == null
        || localTime == null
        || recurrenceType == null) {
      throw new BusinessException("GOAL_INVALID_ACTION", "行动定义不合法");
    }
    ZoneId.of(timezone);
    weekdays = weekdays == null ? Set.of() : Set.copyOf(weekdays);
    if (recurrenceType == RecurrenceType.WEEKLY && weekdays.isEmpty()) {
      throw new BusinessException("GOAL_WEEKDAYS_REQUIRED", "每周行动必须指定星期");
    }
    if (endDate != null && endDate.isBefore(startDate)) {
      throw new BusinessException("GOAL_INVALID_ACTION_RANGE", "行动结束日期早于开始日期");
    }
  }

  public boolean occursOn(LocalDate date) {
    if (date.isBefore(startDate) || (endDate != null && date.isAfter(endDate))) {
      return false;
    }
    return switch (recurrenceType) {
      case ONCE -> date.equals(startDate);
      case DAILY -> true;
      case WEEKLY -> weekdays.contains(date.getDayOfWeek());
    };
  }
}
