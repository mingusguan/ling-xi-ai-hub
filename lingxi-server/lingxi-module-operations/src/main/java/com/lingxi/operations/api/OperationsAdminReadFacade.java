package com.lingxi.operations.api;

import com.lingxi.kernel.PageResult;
import java.time.LocalDateTime;
import java.util.List;

/** 运营后台查询门面，所有投影均为脱敏字段。 */
public interface OperationsAdminReadFacade {
  DashboardSnapshot dashboard(long adminId);
  PageResult<TicketSummary> tickets(long adminId, String status, String category, int page, int pageSize);
  PageResult<ReleaseSummary> releases(long adminId, String type, String status, int page, int pageSize);
  PageResult<AuditSummary> audits(long adminId, String action, Long operatorId, int page, int pageSize);
  PageResult<SafetyCaseSummary> safetyCases(long adminId, String status, String riskLevel, int page, int pageSize);
  PageResult<ResourceSummary> resources(long adminId, String resourceType, String status, int page, int pageSize);
  TicketDetail ticketDetail(long adminId, long ticketId);
  List<MetricSeries> metrics(long adminId, String metricKey, String dimensionType,
      LocalDateTime from, LocalDateTime to);
  PageResult<SafetyAlertSummary> safetyAlerts(long adminId, String status, int page, int pageSize);

  record DashboardSnapshot(long openTickets, long pendingReleases, long highRiskCases,
      long auditActionsToday, long orders, long paidOrders, long revenueMinor,
      long activeSubscriptions, long users, long teenUsers) {}
  record TicketSummary(long id, String ticketNo, long userId, String category, String subject,
      String status, String priority, Long assigneeAdminId, long version,
      LocalDateTime slaDueAt, LocalDateTime followUpAt, LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
  record ReleaseSummary(long id, String releaseKey, String configType, int versionNo,
      String contentRef, String contentDigest, String grayRule, String status,
      long createdBy, Long approvedBy, Long publishedBy, Long previousReleaseId,
      String failureReason, long version, LocalDateTime createdAt, LocalDateTime updatedAt) {}
  record AuditSummary(long id, long adminId, String action, String objectType, String objectId,
      String reason, String ticketNo, String result, String requestId, LocalDateTime createdAt) {}
  record SafetyCaseSummary(long id, String caseNo, Long userId, String sourceType,
      String riskCategory, String riskLevel, String contentExcerpt, String status,
      Long reviewerAdminId, String resolution, long version, LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
  record ResourceSummary(long id, String resourceType, String resourceKey, String name,
      String status, String detailJson, long version, LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
  record TicketDetail(TicketSummary ticket, java.util.List<TicketMessageSummary> messages,
      java.util.List<TicketHistorySummary> history) {}
  record TicketMessageSummary(long id,String senderType,long senderId,String content,
      boolean internalNote,LocalDateTime createdAt) {}
  record TicketHistorySummary(long id,String action,String fromStatus,String toStatus,
      String operatorType,long operatorId,String detail,LocalDateTime createdAt) {}
  record MetricSeries(java.time.LocalDate metricDate,String metricKey,String dimensionType,
      String dimensionValue,java.math.BigDecimal metricValue,long sampleCount) {}
  record SafetyAlertSummary(long id,long safetyCaseId,String alertChannel,String recipientRef,
      String status,int attemptCount,String failureReason,long version,LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
