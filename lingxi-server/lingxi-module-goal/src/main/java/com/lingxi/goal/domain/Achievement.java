package com.lingxi.goal.domain;

import com.lingxi.goal.api.AchievementType;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 不可撤销的用户成就事实。
 *
 * <p>成就由确定性领域规则授予。引用键在数据库上唯一，保证同一成就事实只写入一次；成就一旦授予不再撤销，
 * 用户删除数据时按统一逻辑删除规则处理。
 */
public record Achievement(
    long id,
    long userId,
    Long goalId,
    AchievementType type,
    String title,
    String description,
    String referenceKey,
    LocalDateTime achievedAt,
    LocalDateTime createdAt) {

  public Achievement {
    if (id <= 0 || userId <= 0) {
      throw new BusinessException("GOAL_INVALID_ACHIEVEMENT", "成就标识或用户标识不合法");
    }
    Objects.requireNonNull(type, "成就类型不能为空");
    Objects.requireNonNull(achievedAt, "成就达成时间不能为空");
    Objects.requireNonNull(createdAt, "成就创建时间不能为空");
    if (title == null || title.isBlank()) {
      throw new BusinessException("GOAL_INVALID_ACHIEVEMENT", "成就名称不能为空");
    }
    if (referenceKey == null || referenceKey.isBlank()) {
      throw new BusinessException("GOAL_INVALID_ACHIEVEMENT", "成就引用键不能为空");
    }
  }
}
