package com.lingxi.goal.api;

import com.lingxi.goal.domain.ActionStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** 行动对外结果。 */
public record ActionResult(
    long actionId,
    long goalId,
    long planVersionId,
    Long milestoneId,
    String clientKey,
    String title,
    String description,
    RecurrenceType recurrenceType,
    Set<DayOfWeek> weekdays,
    Integer intervalDays,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime localTime,
    LocalTime endLocalTime,
    String timezone,
    PriorityLevel priority,
    ActionDifficulty difficulty,
    String completionCriteria,
    Integer estimatedMinutes,
    Long prerequisiteActionId,
    String reminderPolicy,
    ActionStatus status,
    /**
     * 是否为新手引导的首行动（PRD ONB-02）。
     *
     * <p>必须对外暴露：客户端要据此在今日工作台里把首行动单独呈现为「先做这件小事」，
     * 并决定是否允许把它改成重复行动。
     */
    boolean first,
    long version) {}
