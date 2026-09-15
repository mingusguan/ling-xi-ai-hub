package com.lingxi.goal.api;

/**
 * 推进首目标引导澄清阶段（PRD 8.2 ONB-02）。
 *
 * @param requestKey 请求幂等键
 * @param userId 当前登录用户
 * @param goalId 目标标识
 * @param stage 要推进到的阶段
 * @param expectedVersion 客户端持有的目标版本；不一致时拒绝写入
 */
public record AdvanceClarificationCommand(
    String requestKey, long userId, long goalId, GoalClarificationStage stage, long expectedVersion) {}
