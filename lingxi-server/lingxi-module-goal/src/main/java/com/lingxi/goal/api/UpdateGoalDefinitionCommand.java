package com.lingxi.goal.api;

/**
 * 更新目标定义命令。
 *
 * <p>由乐观版本号保护：期望版本与当前版本不一致时拒绝写入，避免多端同时编辑互相覆盖。
 *
 * @param requestKey 幂等键
 * @param userId 用户标识
 * @param goalId 目标标识
 * @param expectedVersion 客户端持有的目标版本
 * @param definition 完整的目标定义
 */
public record UpdateGoalDefinitionCommand(
    String requestKey,
    long userId,
    long goalId,
    long expectedVersion,
    GoalDefinitionInput definition) {}
