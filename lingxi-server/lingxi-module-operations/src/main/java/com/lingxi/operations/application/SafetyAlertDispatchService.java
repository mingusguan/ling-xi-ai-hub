package com.lingxi.operations.application;

import com.lingxi.engagement.api.EngagementFacade;
import com.lingxi.engagement.api.NotificationChannel;
import com.lingxi.engagement.api.ScheduleNotificationCommand;
import com.lingxi.operations.domain.SafetyAlertDeliveryRepository;
import com.lingxi.operations.domain.SafetyAlertDeliveryRepository.SafetyAlertDelivery;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** 投递值班队列与监护人站内安全通知，失败后按上限一小时退避重试。 */
@Service
public class SafetyAlertDispatchService {
  private final SafetyAlertDeliveryRepository alerts;
  private final EngagementFacade engagement;

  @Value("${lingxi.operations.safety-alert-lease:PT2M}")
  private Duration lease = Duration.ofMinutes(2);

  public SafetyAlertDispatchService(
      SafetyAlertDeliveryRepository alerts, EngagementFacade engagement) {
    this.alerts = alerts;
    this.engagement = engagement;
  }

  @Scheduled(fixedDelayString = "${lingxi.operations.safety-alert-poll-delay-ms:5000}")
  public void dispatch() {
    LocalDateTime current = now();
    LocalDateTime staleBefore = current.minus(lease);
    for (SafetyAlertDelivery alert : alerts.findDue(current, staleBefore, 50)) {
      dispatch(alert);
    }
  }

  private void dispatch(SafetyAlertDelivery alert) {
    LocalDateTime claimedAt = now();
    LocalDateTime staleBefore = claimedAt.minus(lease);
    if (!alerts.claim(alert.id(), alert.version(), staleBefore, claimedAt)) {
      return;
    }
    long claimedVersion = alert.version() + 1;
    try {
      if ("GUARDIAN_INBOX".equals(alert.alertChannel())) {
        long guardianId = guardianId(alert.recipientRef());
        engagement.scheduleNotification(
            new ScheduleNotificationCommand(
                "SAFETY_ALERT:" + alert.id(),
                guardianId,
                NotificationChannel.INBOX,
                "SAFETY_GUARDIAN_ALERT",
                "SAFETY_CASE",
                String.valueOf(alert.safetyCaseId()),
                "{\"disclosure\":\"MINIMUM\"}",
                Instant.now()));
      } else if (!"DUTY_QUEUE".equals(alert.alertChannel())) {
        throw new IllegalStateException("UNSUPPORTED_ALERT_CHANNEL");
      }
      alerts.markSent(alert.id(), claimedVersion, now());
    } catch (RuntimeException exception) {
      SafetyAlertDelivery current = alerts.find(alert.id());
      if (current != null && "RESOLVED".equals(current.status())) {
        return;
      }
      long seconds = Math.min(3600L, 60L << Math.min(alert.attemptCount(), 5));
      LocalDateTime failureTime = now();
      alerts.markFailed(
          alert.id(),
          claimedVersion,
          exception.getClass().getSimpleName(),
          failureTime.plusSeconds(seconds),
          failureTime);
    }
  }

  private long guardianId(String ref) {
    if (ref == null || !ref.startsWith("USER:")) {
      throw new IllegalStateException("INVALID_GUARDIAN_RECIPIENT");
    }
    return Long.parseLong(ref.substring(5));
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }
}
