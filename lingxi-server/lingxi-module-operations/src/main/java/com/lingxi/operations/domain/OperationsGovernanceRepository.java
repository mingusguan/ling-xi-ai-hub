package com.lingxi.operations.domain;

import com.lingxi.operations.api.OperationsGovernanceFacade.*;
import java.time.LocalDateTime;

/** 运营治理资源仓储。 */
public interface OperationsGovernanceRepository {
  ManagedResource saveCampaign(long id, CampaignCommand command, LocalDateTime now);
  ManagedResource saveAppRelease(long id, AppReleaseCommand command, LocalDateTime now);
  ManagedResource saveComplianceDocument(long id, ComplianceDocumentCommand command, LocalDateTime now);
  ManagedResource saveFeatureFlag(long id, FeatureFlagCommand command, LocalDateTime now);
  ManagedResource saveExperiment(long id, ExperimentCommand command, LocalDateTime now);
  ResourceState findResource(String resourceType, long id);
  boolean isPublishedRelease(long releaseId, String configType, String contentRef);
  ManagedResource changeRuntimeStatus(String resourceType, long id, long releaseId,
      String targetStatus, long expectedVersion, LocalDateTime now);
  ManagedResource retire(String resourceType, long id, long expectedVersion, LocalDateTime now);
  TicketMessageResult replyTicket(long id, long historyId, long adminId, TicketReplyCommand command,
      LocalDateTime now);
  MetricResult recordMetric(long id, MetricCommand command, LocalDateTime now);

  record ResourceState(long id, String status, boolean mandatoryPolicy, Long currentReleaseId,
      long version) {}
}
