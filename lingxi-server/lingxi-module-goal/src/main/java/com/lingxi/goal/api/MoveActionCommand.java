package com.lingxi.goal.api;

import java.time.LocalDate;

/**
 * 移动行动开始日期命令，「本次及未来」整体生效。
 *
 * <p>只平移开始日期，重复规则、时刻与时长保持不变；需要改规则时走编辑接口。
 */
public record MoveActionCommand(
    String requestKey, long userId, long actionId, long expectedVersion, LocalDate newStartDate) {}
