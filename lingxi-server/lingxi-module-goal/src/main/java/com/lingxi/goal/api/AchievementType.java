package com.lingxi.goal.api;

/** 成就类型，只由确定性领域事实触发。 */
public enum AchievementType {
  /** 目标达到成功标准并进入 COMPLETED。 */
  GOAL_COMPLETED,
  /** 里程碑下未取消行动的全部已生成实例均已完成。 */
  MILESTONE_COMPLETED,
  /** 连续完成打卡达到配置阈值。 */
  CHECK_IN_STREAK
}
