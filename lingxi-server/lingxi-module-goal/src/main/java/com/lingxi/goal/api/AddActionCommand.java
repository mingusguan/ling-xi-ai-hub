package com.lingxi.goal.api;

/**
 * 计划外新增行动命令。
 *
 * @param requestKey 幂等键
 * @param goalId 目标标识；行动会追加到该目标当前生效的计划版本上
 * @param milestoneSequence 归属里程碑序号；为空表示不归属里程碑
 */
public record AddActionCommand(
    String requestKey, long userId, long goalId, Integer milestoneSequence, ActionInput input) {}
