package com.lingxi.goal.api;

import java.time.LocalDate;

/** 目标对外结果。 */
public record GoalResult(
    long goalId,
    String publicId,
    long userId,
    String title,
    String description,
    String successCriteria,
    GoalType goalType,
    LocalDate startDate,
    LocalDate targetEndDate,
    PriorityLevel priority,
    Integer weeklyAvailableMinutes,
    String resourceConstraints,
    String verifiableOutcomes,
    GoalPrivacyLevel privacyLevel,
    GoalStatus status,
    Long currentPlanVersionId,
    int progress,
    /** 暂停时的预计恢复日期；仅暂停状态有值。 */
    LocalDate pauseResumeAt,
    /** 放弃原因；仅放弃状态有值。 */
    String abandonReason,
    /**
     * 首目标引导澄清阶段（PRD ONB-02）；null 表示不处于引导中。
     *
     * <p>必须对外暴露：客户端要据此知道「下一个该问用户什么」，换设备后也不必从头问。
     */
    GoalClarificationStage clarificationStage,
    long version) {}
