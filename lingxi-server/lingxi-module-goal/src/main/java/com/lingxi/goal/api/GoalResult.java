package com.lingxi.goal.api;

/** 目标对外结果。 */
public record GoalResult(
    long goalId,
    String publicId,
    long userId,
    String title,
    String successCriteria,
    GoalStatus status,
    Long currentPlanVersionId,
    int progress,
    long version) {}
