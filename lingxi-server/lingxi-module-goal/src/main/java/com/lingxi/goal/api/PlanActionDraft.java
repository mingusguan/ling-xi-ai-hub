package com.lingxi.goal.api;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** 待确认计划中的行动草案。 */
public record PlanActionDraft(
    String clientKey,
    Integer milestoneSequence,
    String title,
    RecurrenceType recurrenceType,
    Set<DayOfWeek> weekdays,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime localTime,
    String timezone) {}
