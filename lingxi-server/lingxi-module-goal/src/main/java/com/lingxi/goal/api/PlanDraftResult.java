package com.lingxi.goal.api;

/** 待确认计划视图。 */
public record PlanDraftResult(
    long planVersionId, long goalId, int versionNo, String status, String source) {}
