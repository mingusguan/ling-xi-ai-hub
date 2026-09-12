package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingxi.goal.api.AchievementType;
import com.lingxi.goal.domain.Achievement;
import com.lingxi.goal.domain.AchievementRepository;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

/** 成就事实的 MyBatis-Plus 仓储实现。 */
@Repository
public class MybatisAchievementRepository implements AchievementRepository {
  private final AchievementMapper achievementMapper;

  public MybatisAchievementRepository(AchievementMapper achievementMapper) {
    this.achievementMapper = achievementMapper;
  }

  @Override
  public boolean insertIfAbsent(Achievement achievement) {
    try {
      return achievementMapper.insert(toEntity(achievement)) == 1;
    } catch (DuplicateKeyException ignored) {
      // 引用键唯一约束兜底多实例并发，重复写入按已获得成就处理。
      return false;
    }
  }

  @Override
  public long countByUser(long userId, Long goalId) {
    return achievementMapper.selectCount(ownerQuery(userId, goalId));
  }

  @Override
  public List<Achievement> findByUser(long userId, Long goalId, int page, int pageSize) {
    Page<AchievementEntity> result =
        achievementMapper.selectPage(
            Page.of(page, pageSize),
            ownerQuery(userId, goalId)
                .orderByDesc(AchievementEntity::getAchievedAt)
                .orderByDesc(AchievementEntity::getId));
    return result.getRecords().stream().map(this::toDomain).toList();
  }

  @Override
  public int logicallyDeleteUserData(long userId) {
    return achievementMapper.delete(
        Wrappers.<AchievementEntity>lambdaQuery().eq(AchievementEntity::getUserId, userId));
  }

  /** 用户成就查询条件；goalId 为空时查询用户全部成就。 */
  private LambdaQueryWrapper<AchievementEntity> ownerQuery(long userId, Long goalId) {
    return Wrappers.<AchievementEntity>lambdaQuery()
        .eq(AchievementEntity::getUserId, userId)
        .eq(goalId != null, AchievementEntity::getGoalId, goalId);
  }

  private Achievement toDomain(AchievementEntity e) {
    return new Achievement(
        e.getId(),
        e.getUserId(),
        e.getGoalId(),
        AchievementType.valueOf(e.getAchievementType()),
        e.getAchievementTitle(),
        e.getAchievementDescription(),
        e.getReferenceKey(),
        e.getAchievedAt(),
        e.getCreatedAt());
  }

  private AchievementEntity toEntity(Achievement a) {
    AchievementEntity e = new AchievementEntity();
    e.setId(a.id());
    e.setUserId(a.userId());
    e.setGoalId(a.goalId());
    e.setAchievementType(a.type().name());
    e.setAchievementTitle(a.title());
    e.setAchievementDescription(a.description());
    e.setReferenceKey(a.referenceKey());
    e.setAchievedAt(a.achievedAt());
    e.setCreatedAt(a.createdAt());
    e.setUpdatedAt(a.createdAt());
    e.setDeleted(false);
    return e;
  }
}
