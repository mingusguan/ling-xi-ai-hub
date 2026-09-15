package com.lingxi.goal.api;

import com.lingxi.goal.domain.OccurrenceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 今日工作台里的一条行动。
 *
 * <p>除了实例本身，还带上排序与提示所需的行动上下文：预计时长、优先级、难度，
 * 以及「它是多少个其他行动的前置」，客户端据此展示「为什么建议先做」。
 *
 * @param localDate 计划日期（行动时区的本地日期）
 * @param overdue 是否逾期：未执行且计划日期早于今天
 * @param unlocksOthers 该行动是另外多少个行动的前置，用于「需要先做」排序
 * @param estimatedMinutes 预计时长（分钟），为空表示未填写
 */
public record TodayItem(
    long occurrenceId,
    long actionId,
    long goalId,
    String goalTitle,
    String actionTitle,
    String actionDescription,
    LocalDate localDate,
    Instant scheduledAt,
    LocalTime localTime,
    String timezone,
    OccurrenceStatus status,
    boolean overdue,
    Integer estimatedMinutes,
    PriorityLevel priority,
    ActionDifficulty difficulty,
    String completionCriteria,
    int unlocksOthers,
    /** 单次调整已登记的例外；为空表示这一次没有被单独调整过。 */
    ActionExceptionType exceptionType) {}
