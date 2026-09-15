package com.lingxi.goal.api;

import com.lingxi.kernel.BusinessException;

/**
 * 活跃目标上限规则：免费档默认上限，会员权益可提升上限。
 *
 * <p>计数口径来自 PRD「目标管理」：只有 {@link GoalStatus#ACTIVE} 与等待确认计划的
 * {@link GoalStatus#PENDING_CONFIRMATION} 计入活跃目标；草稿、暂停、完成与归档均不计入。
 * 上限由会员权益余额表达，因此后台可以通过权益调整给单个用户放宽，而不需要改代码。
 */
public final class ActiveGoalQuota {
  /** 免费档可同时激活的目标数。 */
  public static final int FREE_ACTIVE_GOAL_LIMIT = 2;

  /** 会员权益资源键；余额即允许的活跃目标上限。 */
  public static final String ENTITLEMENT_RESOURCE_KEY = "GOAL_ACTIVE_LIMIT";

  private ActiveGoalQuota() {}

  /** 由权益余额解析当前允许的活跃目标上限；未持有权益时取免费档上限。 */
  public static int allowanceFromEntitlement(long entitlementBalance) {
    if (entitlementBalance <= FREE_ACTIVE_GOAL_LIMIT) {
      return FREE_ACTIVE_GOAL_LIMIT;
    }
    return (int) Math.min(entitlementBalance, Integer.MAX_VALUE);
  }

  /** 该状态是否计入活跃目标上限。 */
  public static boolean countsAsActive(GoalStatus status) {
    return status == GoalStatus.ACTIVE || status == GoalStatus.PENDING_CONFIRMATION;
  }

  /**
   * 校验再激活一个目标后是否仍在配额内。
   *
   * @param otherActiveGoalCount 除当前目标外，该用户已计入上限的目标数
   * @param allowance 当前允许的活跃目标上限
   */
  public static void assertCanActivate(int otherActiveGoalCount, int allowance) {
    if (otherActiveGoalCount + 1 > allowance) {
      throw new BusinessException(
          "GOAL_ACTIVE_LIMIT_EXCEEDED",
          "当前可同时进行 " + allowance + " 个目标，请先完成或暂停其中一个，或升级会员");
    }
  }
}
