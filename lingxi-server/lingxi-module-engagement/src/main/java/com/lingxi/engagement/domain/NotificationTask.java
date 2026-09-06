package com.lingxi.engagement.domain;

import com.lingxi.engagement.api.*;
import com.lingxi.kernel.BusinessException;
import java.time.*;
import java.util.Objects;

/** 可恢复通知任务聚合。 */
public class NotificationTask {
  private final long id;
  private final String dedupeKey;
  private final long recipientUserId;
  private final NotificationChannel channel;
  private final String scene;
  private final String resourceType;
  private final String resourceId;
  private final String payloadJson;
  private final Instant scheduledAt;
  private NotificationTaskStatus status;
  private int attemptCount;
  private String lastError;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private NotificationTask(
      long id,
      String dedupeKey,
      long userId,
      NotificationChannel channel,
      String scene,
      String resourceType,
      String resourceId,
      String payloadJson,
      Instant scheduledAt,
      NotificationTaskStatus status,
      int attempts,
      String error,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.dedupeKey = Objects.requireNonNull(dedupeKey);
    this.recipientUserId = userId;
    this.channel = Objects.requireNonNull(channel);
    this.scene = Objects.requireNonNull(scene);
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.payloadJson = Objects.requireNonNull(payloadJson);
    this.scheduledAt = Objects.requireNonNull(scheduledAt);
    this.status = status;
    this.attemptCount = attempts;
    this.lastError = error;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static NotificationTask schedule(
      long id, ScheduleNotificationCommand c, LocalDateTime now) {
    if (id <= 0 || c.recipientUserId() <= 0 || c.dedupeKey() == null || c.dedupeKey().isBlank()) {
      throw new BusinessException("ENG_INVALID_TASK", "通知任务参数不合法");
    }
    return new NotificationTask(
        id,
        c.dedupeKey(),
        c.recipientUserId(),
        c.channel(),
        c.scene(),
        c.resourceType(),
        c.resourceId(),
        c.payloadJson(),
        c.scheduledAt(),
        NotificationTaskStatus.PENDING,
        0,
        null,
        now,
        now);
  }

  public static NotificationTask rehydrate(
      long id,
      String key,
      long user,
      NotificationChannel channel,
      String scene,
      String rt,
      String rid,
      String payload,
      Instant scheduled,
      NotificationTaskStatus status,
      int attempts,
      String error,
      LocalDateTime created,
      LocalDateTime updated) {
    return new NotificationTask(
        id, key, user, channel, scene, rt, rid, payload, scheduled, status, attempts, error,
        created, updated);
  }

  public void start(LocalDateTime now) {
    if (status != NotificationTaskStatus.PENDING
        && status != NotificationTaskStatus.FAILED_RETRYABLE) {
      throw new BusinessException("ENG_TASK_NOT_READY", "通知任务不可投递");
    }
    status = NotificationTaskStatus.SENDING;
    updatedAt = now;
  }

  public void sent(LocalDateTime now) {
    status = NotificationTaskStatus.SENT;
    updatedAt = now;
  }

  public void cancel(String reason, LocalDateTime now) {
    if (status != NotificationTaskStatus.SENDING) {
      throw new BusinessException("ENG_TASK_NOT_SENDING", "通知任务不在投递中");
    }
    status = NotificationTaskStatus.CANCELLED;
    lastError = reason;
    updatedAt = now;
  }

  public void failed(String error, boolean retryable, LocalDateTime now) {
    attemptCount++;
    status =
        retryable && attemptCount < 5
            ? NotificationTaskStatus.FAILED_RETRYABLE
            : NotificationTaskStatus.FAILED_FINAL;
    lastError = error;
    updatedAt = now;
  }

  public long getId() {
    return id;
  }

  public String getDedupeKey() {
    return dedupeKey;
  }

  public long getRecipientUserId() {
    return recipientUserId;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public String getScene() {
    return scene;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public Instant getScheduledAt() {
    return scheduledAt;
  }

  public NotificationTaskStatus getStatus() {
    return status;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public String getLastError() {
    return lastError;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
