package com.lingxi.goal.api;

import java.time.LocalDate;

/**
 * 目标生命周期流转命令。
 *
 * <p>四个动作共用一个命令：目标状态本身就是幂等依据，重复提交同一动作时
 * 若目标已处于目标状态则直接返回当前结果，不再要求版本号匹配。
 *
 * @param requestKey 幂等键
 * @param userId 用户标识
 * @param goalId 目标标识
 * @param expectedVersion 客户端持有的目标版本；目标已处于目标状态时忽略
 * @param transition 目标动作
 * @param expectedResumeDate 预计恢复日期，仅 PAUSE 可填
 * @param abandonReason 放弃原因，仅 ABANDON 必填
 */
public record TransitionGoalCommand(
    String requestKey,
    long userId,
    long goalId,
    long expectedVersion,
    GoalTransition transition,
    LocalDate expectedResumeDate,
    String abandonReason) {}
