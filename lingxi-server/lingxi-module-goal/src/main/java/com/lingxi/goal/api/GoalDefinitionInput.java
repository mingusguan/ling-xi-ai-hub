package com.lingxi.goal.api;

import java.time.LocalDate;

/**
 * 目标定义入参，创建与更新共用同一份字段清单。
 *
 * <p>字段与 PRD「目标管理」的目标定义一致。除标题与完成标准外均可为空：
 * 应用层会为目标类型补齐「习惯养成」、为优先级补齐「常规」、为隐私级别补齐「仅自己」，
 * 保证早期只填两个字段的入口继续可用。
 */
public record GoalDefinitionInput(
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
    GoalPrivacyLevel privacyLevel) {

  /** 只提供标题与完成标准的最小输入，供离线同步等简化入口复用。 */
  public static GoalDefinitionInput minimal(String title, String successCriteria) {
    return new GoalDefinitionInput(
        title, null, successCriteria, null, null, null, null, null, null, null, null);
  }
}
