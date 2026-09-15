package com.lingxi.goal.api;

/**
 * 单次调整类型。
 *
 * <p>属于对外契约：客户端在「只改这一次」时必须在跳过与改期之间二选一。
 */
public enum ActionExceptionType {
  /** 仅跳过这一次，重复规则不变。 */
  SKIP,
  /** 仅把这一次挪到另一个日期，重复规则不变。 */
  RESCHEDULE
}
