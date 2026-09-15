package com.lingxi.goal.domain;

import com.lingxi.goal.api.PriorityLevel;
import com.lingxi.goal.api.GoalPrivacyLevel;
import com.lingxi.goal.api.GoalType;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDate;

/**
 * 目标定义值对象，承载 PRD「目标管理」要求的全部定义字段。
 *
 * <p>标题、描述、完成标准、目标类型、起止日期、优先级、每周可用时间、资源和限制条件、
 * 可验证成果与隐私级别都属于目标定义；状态、进度与并发版本属于聚合本身。
 * 把定义整体建模为不可变值对象后，更新目标定义时整体替换，不必逐字段漂移。
 */
public record GoalDefinition(
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
  /** 每周可用时间上界：一周总分钟数，超过该值视为填写错误。 */
  private static final int MAX_WEEKLY_MINUTES = 7 * 24 * 60;

  public GoalDefinition {
    title = requireText(title, "目标标题不能为空");
    successCriteria = requireText(successCriteria, "成功标准不能为空");
    description = trimToNull(description);
    resourceConstraints = trimToNull(resourceConstraints);
    verifiableOutcomes = trimToNull(verifiableOutcomes);
    if (goalType == null) {
      throw new BusinessException("GOAL_INVALID_DEFINITION", "目标类型不能为空");
    }
    priority = priority == null ? PriorityLevel.NORMAL : priority;
    privacyLevel = privacyLevel == null ? GoalPrivacyLevel.PRIVATE : privacyLevel;
    if (startDate != null && targetEndDate != null && targetEndDate.isBefore(startDate)) {
      throw new BusinessException("GOAL_INVALID_DATE_RANGE", "期望完成日期早于开始日期");
    }
    if (weeklyAvailableMinutes != null
        && (weeklyAvailableMinutes <= 0 || weeklyAvailableMinutes > MAX_WEEKLY_MINUTES)) {
      throw new BusinessException("GOAL_INVALID_AVAILABLE_TIME", "每周可用时间超出合理范围");
    }
  }

  /**
   * 仅提供标题与完成标准的最小定义。
   *
   * <p>供创建草稿目标以及历史数据兼容使用：目标类型按「习惯养成」归类，
   * 优先级与隐私级别分别取常规与「仅自己」默认值。
   */
  public static GoalDefinition of(String title, String successCriteria) {
    return new GoalDefinition(
        title,
        null,
        successCriteria,
        GoalType.HABIT,
        null,
        null,
        PriorityLevel.NORMAL,
        null,
        null,
        null,
        GoalPrivacyLevel.PRIVATE);
  }

  private static String requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new BusinessException("GOAL_INVALID_DEFINITION", message);
    }
    return value.trim();
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
