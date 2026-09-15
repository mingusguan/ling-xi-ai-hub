package com.lingxi.goal.api;

/**
 * 活跃目标配额查询端口。
 *
 * <p>目标模块不直接依赖交易模块：上限的来源（会员权益、后台人工调整、活动赠送）由装配层决定。
 * 具体实现由组合根提供，模块内单元测试可以给出固定值。
 */
@FunctionalInterface
public interface ActiveGoalQuotaPort {
  /** 返回该用户当前允许同时激活的目标数上限。 */
  int allowanceFor(long userId);
}
