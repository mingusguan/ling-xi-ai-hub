package com.lingxi.goal.api;

import java.time.Instant;

/**
 * 周期复盘结果视图。
 *
 * @param reviewId 复盘标识
 * @param goalId 目标标识
 * @param periodKey 周期键，例如 2026-W37
 * @param status 复盘状态（PENDING/COMPLETED）
 * @param conclusionJson 复盘结论 JSON；未完成时为 null
 * @param completedAt 完成时刻；未完成时为 null
 * @param inputSnapshotJson 生成复盘时的输入快照 JSON，供客户端展示当时的事实依据
 * @param createdAt 创建时刻
 */
public record ReviewResult(
    long reviewId,
    long goalId,
    String periodKey,
    String status,
    String conclusionJson,
    Instant completedAt,
    String inputSnapshotJson,
    Instant createdAt) {}
