package com.lingxi.goal.domain;

/** 行动实例状态。 */
public enum OccurrenceStatus {
  /** 待执行。 */
  SCHEDULED,
  /** 已完成。 */
  COMPLETED,
  /** 部分完成。 */
  PARTIAL,
  /** 已跳过：用户主动决定不做。 */
  SKIPPED,
  /** 已失败：尝试过但没做成。 */
  FAILED,
  /** 已错过：到期未处理，由维护任务判定。 */
  MISSED
}
