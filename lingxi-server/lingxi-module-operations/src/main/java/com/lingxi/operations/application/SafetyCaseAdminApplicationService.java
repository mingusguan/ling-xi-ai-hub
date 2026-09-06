package com.lingxi.operations.application;

import com.lingxi.kernel.BusinessException;
import com.lingxi.operations.api.*;
import com.lingxi.operations.domain.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class SafetyCaseAdminApplicationService implements SafetyCaseAdminFacade {
  private final SafetyCaseRepository repository;
  private final SafetyAlertDeliveryRepository alerts;
  private final AdminPermissionAdapter permissions;
  private final AuditService audit;

  public SafetyCaseAdminApplicationService(
      SafetyCaseRepository repository,
      SafetyAlertDeliveryRepository alerts,
      AdminPermissionAdapter permissions,
      AuditService audit) {
    this.repository = repository;
    this.alerts = alerts;
    this.permissions = permissions;
    this.audit = audit;
  }

  @Transactional
  public SafetyCaseResult transition(TransitionSafetyCaseCommand command) {
    secure(command.adminId(), command.audit());
    SafetyCase value =
        repository
            .find(command.caseId())
            .orElseThrow(
                () -> new BusinessException("OPS_SAFETY_CASE_NOT_FOUND", "安全事件不存在"));
    long oldVersion = value.getVersion();
    String before = value.getStatus() + ":" + oldVersion;
    value.transition(
        command.adminId(),
        command.targetStatus(),
        command.resolution(),
        command.expectedVersion(),
        now());
    if (!repository.update(value, oldVersion)) {
      throw new BusinessException("OPS_SAFETY_CASE_CONFLICT", "安全事件已变化");
    }
    if (value.getStatus() == SafetyCase.Status.RESOLVED
        || value.getStatus() == SafetyCase.Status.CLOSED) {
      alerts.resolveByCase(value.getId(), now());
    }
    audit.append(
        command.adminId(),
        "SAFETY_CASE_TRANSITION",
        "SAFETY_CASE",
        String.valueOf(value.getId()),
        command.audit(),
        before,
        value.getStatus() + ":" + value.getVersion(),
        "SUCCESS");
    return new SafetyCaseResult(
        value.getId(),
        value.getCaseNo(),
        value.getRiskCategory(),
        value.getRiskLevel(),
        value.getStatus().name(),
        value.getReviewerAdminId(),
        value.getResolution(),
        value.getVersion());
  }

  @Transactional
  public SafetyAlertResult transitionAlert(TransitionSafetyAlertCommand command) {
    secure(command.adminId(), command.audit());
    var value =
        alerts.transition(
            command.alertId(), command.targetStatus(), command.expectedVersion(), now());
    audit.append(
        command.adminId(),
        "SAFETY_ALERT_TRANSITION",
        "SAFETY_ALERT",
        String.valueOf(value.id()),
        command.audit(),
        null,
        value.status() + ":" + value.version(),
        "SUCCESS");
    return new SafetyAlertResult(
        value.id(),
        value.safetyCaseId(),
        value.alertChannel(),
        value.recipientRef(),
        value.status(),
        value.attemptCount(),
        value.failureReason(),
        value.version());
  }

  private void secure(long adminId, AuditContext context) {
    if (context == null
        || !context.recentAuthentication()
        || blank(context.reason())
        || blank(context.ticketNo())) {
      throw new BusinessException(
          "OPS_HIGH_RISK_CONTEXT_REQUIRED", "安全处置需要近期认证、原因和工单号");
    }
    if (!permissions.allowed(adminId, "safety:case:manage")) {
      throw new BusinessException("OPS_ADMIN_FORBIDDEN", "管理员权限不足");
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
