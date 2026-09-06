package com.lingxi.goal.api;

import java.time.Instant;

/** 行动打卡结果视图。 */
public record CheckInResult(
    long checkInId,
    long occurrenceId,
    CheckInResultType result,
    String occurrenceStatus,
    int goalProgress,
    Instant recordedAt) {}
