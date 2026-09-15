package com.lingxi.goal.api;

/**
 * 创建目标命令。
 *
 * @param requestKey 幂等键
 * @param userId 用户标识
 * @param definition 目标定义，见 PRD「目标管理」
 */
public record CreateGoalCommand(String requestKey, long userId, GoalDefinitionInput definition) {}
