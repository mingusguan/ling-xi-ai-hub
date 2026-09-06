package com.lingxi.operations.api;

import java.time.LocalDateTime;

/** 运营治理资源的结构化写入门面。 */
public interface OperationsGovernanceFacade {
  ManagedResource saveCampaign(long adminId, CampaignCommand command);
  ManagedResource saveAppRelease(long adminId, AppReleaseCommand command);
  ManagedResource saveComplianceDocument(long adminId, ComplianceDocumentCommand command);
  ManagedResource saveFeatureFlag(long adminId, FeatureFlagCommand command);
  ManagedResource saveExperiment(long adminId, ExperimentCommand command);
  ManagedResource changeRuntimeStatus(long adminId, RuntimeStatusCommand command);
  ManagedResource retire(long adminId, RetireResourceCommand command);
  TicketMessageResult replyTicket(long adminId, TicketReplyCommand command);
  MetricResult recordMetric(long adminId, MetricCommand command);

  record CampaignCommand(long id, String campaignKey, String name, String channel,
      String audienceRule, String templateContent, String frequencyRule,
      boolean teenMarketingEnabled, LocalDateTime scheduledAt, String status,
      long expectedVersion, AuditContext context) {}
  record AppReleaseCommand(long id, String platform, String versionName, long versionCode,
      long minimumVersionCode, boolean forceUpgrade, String grayRule, String releaseNotes,
      String status, long expectedVersion, AuditContext context) {}
  record ComplianceDocumentCommand(long id, String documentType, String versionNo,
      String title, String contentRef, String contentDigest, LocalDateTime effectiveAt,
      String status, long expectedVersion, AuditContext context) {}
  record FeatureFlagCommand(long id, String flagKey, Long currentReleaseId,
      boolean mandatoryPolicy, String status, long expectedVersion, AuditContext context) {}
  record ExperimentCommand(long id, String experimentKey, String hypothesis,
      String audienceRule, String metricsJson, Long currentReleaseId, String status,
      long expectedVersion, AuditContext context) {}
  record RetireResourceCommand(String resourceType, long id, long expectedVersion,
      AuditContext context) {}
  /** 结构化资源只能关联已经发布且类型匹配的配置发布单后进入运行状态。 */
  record RuntimeStatusCommand(String resourceType, long id, long releaseId, String targetStatus,
      long expectedVersion, AuditContext context) {}
  record TicketReplyCommand(long ticketId, String content, boolean internalNote,
      LocalDateTime followUpAt, AuditContext context) {}
  record MetricCommand(java.time.LocalDate metricDate, String metricKey, String dimensionType,
      String dimensionValue, java.math.BigDecimal metricValue, long sampleCount,
      AuditContext context) {}
  record ManagedResource(long id, String resourceType, String resourceKey, String status,
      long version, LocalDateTime updatedAt) {}
  record TicketMessageResult(long id, long ticketId, boolean internalNote,
      LocalDateTime createdAt) {}
  record MetricResult(java.time.LocalDate metricDate, String metricKey, String dimensionType,
      String dimensionValue, java.math.BigDecimal metricValue, long sampleCount) {}
}
