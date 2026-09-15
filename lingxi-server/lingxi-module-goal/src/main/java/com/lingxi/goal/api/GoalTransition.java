package com.lingxi.goal.api;

/** 目标生命周期动作。 */
public enum GoalTransition {
  /** 暂停：可带预计恢复日期，暂停后不计入活跃上限。 */
  PAUSE,
  /** 恢复：回到进行中，需要重新评估截止期与剩余任务。 */
  RESUME,
  /** 放弃：必须记录原因。 */
  ABANDON,
  /** 归档：把已结束的目标收进归档列表。 */
  ARCHIVE
}
