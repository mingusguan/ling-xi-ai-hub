package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.identity.api.GuardianNotificationProvider;
import java.util.List;
import org.springframework.stereotype.Component;

/** 从有效监护关系中读取通知目标，只返回用户标识。 */
@Component
public class MybatisGuardianNotificationProvider implements GuardianNotificationProvider {
  private final GuardianRelationMapper relations;
  public MybatisGuardianNotificationProvider(GuardianRelationMapper relations){this.relations=relations;}

  @Override
  public List<Long> activeGuardianUserIds(long teenUserId){
    return relations.selectList(Wrappers.<GuardianRelationEntity>lambdaQuery()
            .select(GuardianRelationEntity::getGuardianUserId)
            .eq(GuardianRelationEntity::getTeenUserId,teenUserId)
            .eq(GuardianRelationEntity::getStatus,"ACTIVE"))
        .stream().map(GuardianRelationEntity::getGuardianUserId).filter(java.util.Objects::nonNull)
        .distinct().toList();
  }
}
