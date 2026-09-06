package com.lingxi.goal.api;

/** 待确认计划中的里程碑草案。 */
public record PlanMilestoneDraft(int sequenceNo, String title, String successCriteria) {}
