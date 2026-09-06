package com.lingxi.goal.api;

import java.time.Instant;

/** 周期复盘结果视图。 */
public record ReviewResult(
    long reviewId,
    long goalId,
    String periodKey,
    String status,
    String conclusionJson,
    Instant completedAt) {}
