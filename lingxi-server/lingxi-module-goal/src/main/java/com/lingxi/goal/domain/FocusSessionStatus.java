package com.lingxi.goal.domain;

/** 专注会话状态。 */
public enum FocusSessionStatus {
  /** 计时中。 */
  RUNNING,
  /** 已暂停；暂停期间不计入累计时长。 */
  PAUSED,
  /** 已结束。 */
  FINISHED,
  /** 已作废；用于用户中途放弃且不想保留为一次专注记录的情形。 */
  ABANDONED
}
