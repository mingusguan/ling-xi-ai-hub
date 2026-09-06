package com.lingxi.goal.domain;

/** 打卡授权和进度计算所需的单表查询组装结果。 */
public record OccurrenceContext(Goal goal, Action action, ActionOccurrence occurrence) {}
