package com.lingxi.goal.api;

import com.lingxi.kernel.PageResult;

/** 成就模块对 PC Web、HarmonyOS 和其他模块公开的应用门面。 */
public interface AchievementFacade {
  /** 查询用户本人成就，可按目标过滤，页码从 1 开始。 */
  PageResult<AchievementResult> listAchievements(long userId, Long goalId, int page, int pageSize);
}
