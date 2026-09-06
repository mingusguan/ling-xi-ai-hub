package com.lingxi.identity.application;

import com.lingxi.identity.api.PrivacyContribution;
import com.lingxi.identity.api.PrivacyDataContributor;
import com.lingxi.identity.api.PrivacyProcessingContext;
import com.lingxi.identity.domain.IdentitySecurityRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 在同一事务中完成单个模块的逻辑删除与完成审计，避免只删数据却丢失幂等检查点。 */
@Component
public class PrivacyModuleTransactionExecutor {
  private final IdentitySecurityRepository repository;

  public PrivacyModuleTransactionExecutor(IdentitySecurityRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public PrivacyContribution executeDeletion(
      PrivacyDataContributor contributor,
      PrivacyProcessingContext context,
      LocalDateTime completedAt) {
    return repository
        .findDeletionAuditAffectedRows(context.requestId(), contributor.moduleName())
        .map(PrivacyContribution::deleted)
        .orElseGet(
            () -> {
              PrivacyContribution contribution = contributor.process(context);
              if (contribution.pendingReference() == null) {
                repository.recordDeletionAudit(
                    context.requestId(),
                    context.userId(),
                    contributor.moduleName(),
                    contribution.affectedRows(),
                    completedAt);
              }
              return contribution;
            });
  }
}
