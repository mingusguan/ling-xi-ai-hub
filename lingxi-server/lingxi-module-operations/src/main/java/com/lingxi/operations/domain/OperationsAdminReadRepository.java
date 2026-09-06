package com.lingxi.operations.domain;

import com.lingxi.kernel.PageResult;
import com.lingxi.operations.api.OperationsAdminReadFacade.*;

/** operations 自有表的管理投影读取端口。 */
public interface OperationsAdminReadRepository {
  DashboardSnapshot localDashboard();
  PageResult<TicketSummary> tickets(String status, String category, int page, int size);
  PageResult<ReleaseSummary> releases(String type, String status, int page, int size);
  PageResult<AuditSummary> audits(String action, Long operatorId, int page, int size);
  PageResult<SafetyCaseSummary> safetyCases(String status, String riskLevel, int page, int size);
  PageResult<ResourceSummary> resources(String resourceType, String status, int page, int size);
  TicketDetail ticketDetail(long ticketId);
  java.util.List<MetricSeries> metrics(String metricKey,String dimensionType,
      java.time.LocalDateTime from,java.time.LocalDateTime to);
  PageResult<SafetyAlertSummary> safetyAlerts(String status,int page,int size);
}
