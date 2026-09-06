package com.lingxi.content.domain;

import com.lingxi.content.api.TemplateGovernanceFacade.*;
import java.time.LocalDate;import java.time.LocalDateTime;import java.util.List;

public interface TemplateGovernanceRepository {
  ReviewResult review(long id,long adminId,long templateId,long aggregateVersion,String dimension,
      String decision,String comment,LocalDateTime now);
  List<ReviewResult> reviews(long templateId);
  boolean allApproved(long templateId,long aggregateVersion);
  List<MetricSummary> metrics(long templateId,LocalDate from,LocalDate to);
}
