package com.lingxi.goal.domain;

import java.util.List;

/** 成就事实持久化端口。 */
public interface AchievementRepository {
  /** 按引用键幂等写入成就；引用键已存在时返回 false，不重复授予。 */
  boolean insertIfAbsent(Achievement achievement);

  /** 统计用户成就总数，可按目标过滤。 */
  long countByUser(long userId, Long goalId);

  /** 分页查询用户成就，按达成时间倒序，页码从 1 开始。 */
  List<Achievement> findByUser(long userId, Long goalId, int page, int pageSize);

  /** 用户删除数据时逻辑删除其全部成就。 */
  int logicallyDeleteUserData(long userId);
}
