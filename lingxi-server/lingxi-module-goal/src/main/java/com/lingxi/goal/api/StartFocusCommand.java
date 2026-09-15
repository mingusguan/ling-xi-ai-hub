package com.lingxi.goal.api;

/**
 * 开始专注会话命令。
 *
 * <p>关联行动实例是可选的：用户也可以先开始专注、之后再决定这次专注属于哪个行动。
 * 给出 {@code occurrenceId} 时会自动带出所属行动与目标，供统计使用。
 */
public record StartFocusCommand(
    String requestKey, long userId, Long occurrenceId, Integer plannedMinutes) {}
