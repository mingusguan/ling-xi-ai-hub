package com.lingxi.operations.api;

/** 安全事件人工复核与处置命令门面。 */
public interface SafetyCaseAdminFacade {
  SafetyCaseResult transition(TransitionSafetyCaseCommand command);
  SafetyAlertResult transitionAlert(TransitionSafetyAlertCommand command);

  record TransitionSafetyCaseCommand(long adminId, long caseId, String targetStatus,
      String resolution, long expectedVersion, AuditContext audit) {}
  record SafetyCaseResult(long id, String caseNo, String riskCategory, String riskLevel,
      String status, Long reviewerAdminId, String resolution, long version) {}
  record TransitionSafetyAlertCommand(long adminId,long alertId,String targetStatus,
      long expectedVersion,AuditContext audit) {}
  record SafetyAlertResult(long id,long safetyCaseId,String alertChannel,String recipientRef,
      String status,int attemptCount,String failureReason,long version) {}
}
