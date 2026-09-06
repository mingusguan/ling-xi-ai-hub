package com.lingxi.operations.domain;

import java.time.LocalDateTime;
import java.util.List;

/** 安全告警投递状态仓储。 */
public interface SafetyAlertDeliveryRepository {
  List<SafetyAlertDelivery> findDue(LocalDateTime now, LocalDateTime staleBefore, int limit);

  SafetyAlertDelivery find(long id);

  boolean claim(long id, long expectedVersion, LocalDateTime staleBefore, LocalDateTime now);

  boolean markSent(long id, long claimedVersion, LocalDateTime now);

  boolean markFailed(
      long id, long claimedVersion, String reason, LocalDateTime nextRetryAt, LocalDateTime now);

  SafetyAlertDelivery transition(
      long id, String targetStatus, long expectedVersion, LocalDateTime now);

  int resolveByCase(long caseId, LocalDateTime now);

  record SafetyAlertDelivery(
      long id,
      long safetyCaseId,
      String alertChannel,
      String recipientRef,
      String status,
      int attemptCount,
      LocalDateTime nextRetryAt,
      String failureReason,
      long version) {}
}
