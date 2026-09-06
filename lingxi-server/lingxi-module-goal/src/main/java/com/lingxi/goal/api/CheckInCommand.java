package com.lingxi.goal.api;

/** 行动实例打卡或修正命令。 */
public record CheckInCommand(
    String requestKey,
    long userId,
    long occurrenceId,
    CheckInResultType result,
    String note,
    String evidenceReference,
    boolean correction) {}
