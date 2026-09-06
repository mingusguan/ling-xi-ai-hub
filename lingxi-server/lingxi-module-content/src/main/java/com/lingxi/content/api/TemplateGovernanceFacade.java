package com.lingxi.content.api;

import java.time.LocalDate;
import java.util.List;

/** 模板安全性、可行性、质量评审与效果指标门面。 */
public interface TemplateGovernanceFacade {
  ReviewResult reviewDimension(ReviewCommand command);
  List<ReviewResult> reviews(long adminId,long templateVersionId);
  List<MetricSummary> metrics(long adminId,long templateVersionId,LocalDate from,LocalDate to);
  record OperationContext(String reason,String ticketNo,String requestId,boolean recentAuthentication){}
  record ReviewCommand(long adminId,long templateVersionId,String dimension,String decision,
      String comment,long expectedTemplateVersion,OperationContext context){}
  record ReviewResult(long id,long templateVersionId,long templateVersion,String dimension,
      String decision,String comment,long reviewerAdminId,java.time.LocalDateTime createdAt){}
  record MetricSummary(LocalDate metricDate,long templateVersionId,long usageCount,
      long completionCount,long negativeFeedbackCount){}
}
