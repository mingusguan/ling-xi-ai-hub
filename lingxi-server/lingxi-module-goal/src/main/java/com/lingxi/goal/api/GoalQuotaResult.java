package com.lingxi.goal.api;

/**
 * 活跃目标配额结果。
 *
 * @param allowance 当前允许同时激活的目标数上限
 * @param activeCount 当前已计入上限的目标数
 */
public record GoalQuotaResult(int allowance, int activeCount) {

  /** 是否还可以再激活一个目标。 */
  public boolean canActivate() {
    return activeCount < allowance;
  }
}
