package com.lingxi.goal.api;

import com.lingxi.goal.api.ActionExceptionType;
import java.time.LocalDate;

/**
 * 单次调整例外结果。
 *
 * @param localDate 原计划日期
 * @param rescheduledDate 改期后的日期；跳过时为 null
 */
public record ActionExceptionResult(
    long exceptionId,
    long actionId,
    LocalDate localDate,
    ActionExceptionType type,
    LocalDate rescheduledDate,
    String reason) {}
