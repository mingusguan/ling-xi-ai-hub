package com.lingxi.identity.api;

/**
 * 向身份模块提供最小监护关系事实。
 *
 * <p>实现位于关系模块，身份模块仅消费“是否存在有效监护关系”，避免读取关系表或泄露监护详情。
 */
public interface GuardianStatusProvider {

  /** 判断青少年是否至少存在一条有效监护关系。 */
  boolean hasActiveGuardian(long teenUserId);
}
