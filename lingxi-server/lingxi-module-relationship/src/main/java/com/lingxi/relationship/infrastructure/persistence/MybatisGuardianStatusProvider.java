package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.identity.api.GuardianStatusProvider;
import org.springframework.stereotype.Component;

/** 使用关系模块事实表提供最小监护状态，不向身份模块暴露监护人信息。 */
@Component
public class MybatisGuardianStatusProvider implements GuardianStatusProvider {
  private final GuardianRelationMapper relations;

  public MybatisGuardianStatusProvider(GuardianRelationMapper relations) {
    this.relations = relations;
  }

  @Override
  public boolean hasActiveGuardian(long teenUserId) {
    return relations.selectCount(
            Wrappers.<GuardianRelationEntity>lambdaQuery()
                .eq(GuardianRelationEntity::getTeenUserId, teenUserId)
                .eq(GuardianRelationEntity::getStatus, "ACTIVE"))
        > 0;
  }
}
