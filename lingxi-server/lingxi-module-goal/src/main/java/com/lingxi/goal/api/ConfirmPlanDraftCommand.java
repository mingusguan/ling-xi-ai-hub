package com.lingxi.goal.api;

/** 用户确认既有计划草案。 */
public record ConfirmPlanDraftCommand(
    String requestKey, long userId, long planVersionId, long expectedGoalVersion) {}
