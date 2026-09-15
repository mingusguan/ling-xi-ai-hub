package com.lingxi.goal.domain;

import com.lingxi.goal.api.ActionExceptionType;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 单次调整例外。
 *
 * <p>PRD 要求「单次修改」与「修改未来全部」严格分离：单次调整不能改写行动定义，
 * 否则一次临时改期就会污染整个重复规则。因此单次调整记录为按「行动 + 原日期」唯一的例外，
 * 由实例生成器在展开重复规则时叠加：{@link ActionExceptionType#SKIP} 跳过该日，
 * {@link ActionExceptionType#RESCHEDULE} 把该日实例挪到另一天。
 */
public record ActionException(
    long id,
    long actionId,
    LocalDate localDate,
    ActionExceptionType type,
    LocalDate rescheduledDate,
    String reason,
    LocalDateTime createdAt) {

  public ActionException {
    if (id <= 0 || actionId <= 0 || localDate == null || type == null) {
      throw new BusinessException("GOAL_INVALID_ACTION_EXCEPTION", "单次调整记录不合法");
    }
    if (type == ActionExceptionType.SKIP) {
      // 跳过不需要目标日期；带上目标日期说明调用方意图自相矛盾，静默丢弃会掩盖客户端错误。
      if (rescheduledDate != null) {
        throw new BusinessException("GOAL_INVALID_ACTION_EXCEPTION", "跳过这一次不需要目标日期");
      }
    } else {
      if (rescheduledDate == null) {
        throw new BusinessException("GOAL_RESCHEDULE_TARGET_REQUIRED", "单次改期必须给出目标日期");
      }
      if (rescheduledDate.equals(localDate)) {
        throw new BusinessException("GOAL_INVALID_ACTION_EXCEPTION", "目标日期与原日期相同");
      }
    }
    reason = reason == null || reason.isBlank() ? null : reason.trim();
  }

  /** 跳过指定日期的这一次。 */
  public static ActionException skip(
      long id, long actionId, LocalDate localDate, String reason, LocalDateTime now) {
    return new ActionException(
        id, actionId, localDate, ActionExceptionType.SKIP, null, reason, now);
  }

  /** 把指定日期的这一次改到另一天。 */
  public static ActionException reschedule(
      long id,
      long actionId,
      LocalDate localDate,
      LocalDate targetDate,
      String reason,
      LocalDateTime now) {
    return new ActionException(
        id, actionId, localDate, ActionExceptionType.RESCHEDULE, targetDate, reason, now);
  }
}
