package com.lingxi.goal.api;

import com.lingxi.goal.api.ActionExceptionType;
import java.time.LocalDate;

/**
 * 单次调整命令。
 *
 * <p>对应 PRD 的「单次修改」分支：只影响这一次，不改写行动定义。
 *
 * @param type 跳过或改期
 * @param targetDate 改期目标日期；跳过时必须为空
 * @param reason 调整原因，可为空
 */
public record AdjustOccurrenceCommand(
    String requestKey,
    long userId,
    long occurrenceId,
    ActionExceptionType type,
    LocalDate targetDate,
    String reason) {}
