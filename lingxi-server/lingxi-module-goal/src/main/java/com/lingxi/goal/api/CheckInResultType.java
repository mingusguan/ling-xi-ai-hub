package com.lingxi.goal.api;

/** 打卡结果。 */
public enum CheckInResultType {
  /** 完成。 */
  COMPLETED,
  /** 部分完成。 */
  PARTIAL,
  /** 跳过：用户主动决定这次不做，不等于失败。 */
  SKIPPED,
  /** 失败：尝试了但没做成，必须记录原因，供复盘定位阻塞。 */
  FAILED
}
