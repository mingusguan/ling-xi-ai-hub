package com.lingxi.goal.api;

import com.lingxi.goal.domain.FocusSessionStatus;
import java.time.Instant;

/**
 * 专注会话视图。
 *
 * @param accumulatedSeconds 已累计专注秒数；计时中时包含正在进行的区间
 * @param plannedMinutes 计划时长（分钟），为空表示未设定
 * @param active 是否进行中（计时中或已暂停）
 */
public record FocusSessionResult(
    long sessionId,
    long userId,
    Long occurrenceId,
    Long actionId,
    FocusSessionStatus status,
    boolean active,
    int accumulatedSeconds,
    Integer plannedMinutes,
    Instant startedAt,
    Instant lastResumedAt,
    Instant endedAt,
    String note,
    long version) {}
