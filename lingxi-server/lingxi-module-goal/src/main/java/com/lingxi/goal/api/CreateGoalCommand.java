package com.lingxi.goal.api;

/**
 * 创建目标命令。
 *
 * @param requestKey 幂等键
 * @param userId 用户标识
 * @param title 目标标题
 * @param successCriteria 可验证的成功标准
 */
public record CreateGoalCommand(
    String requestKey, long userId, String title, String successCriteria) {}
