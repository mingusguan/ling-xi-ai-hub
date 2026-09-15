package com.lingxi.engagement.domain;

import com.lingxi.engagement.api.*;
import com.lingxi.kernel.BusinessException;
import java.time.*;
import java.util.Objects;

/** 可恢复通知任务聚合。 */
public class NotificationTask {
  /** 因免打扰被推迟时记录的固定原因，便于后台区分「策略拒绝」与「延后发送」。 */
  public static final String DEFERRED_REASON = "DEFERRED_QUIET_HOURS";

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
  /** 因免打扰时段被推迟到的时刻；null 表示没有被推迟过。 */
  private Instant deferredUntil;
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
      Instant deferredUntil,
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
    this.deferredUntil = deferredUntil;
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
      Instant deferredUntil,
      LocalDateTime created,
      LocalDateTime updated) {
    return new NotificationTask(
        id, key, user, channel, scene, rt, rid, payload, scheduled, status, attempts, error,
        deferredUntil, created, updated);
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

  /** 取消尚未开始投递的任务，例如行动已完成或计划已变更。 */
  public void cancelBeforeSend(String reason, LocalDateTime now) {
    if (status != NotificationTaskStatus.PENDING) {
      throw new BusinessException("ENG_TASK_NOT_PENDING", "通知任务不在待投递状态");
    }
    status = NotificationTaskStatus.CANCELLED;
    lastError = reason;
    updatedAt = now;
  }

  /**
   * 因为当前处于免打扰时段而把任务推迟到指定时刻。
   *
   * <p>与 {@link #cancel} 的区别是产品语义的核心：命中免打扰不是「不发了」，
   * 而是「换个时间再发」。用户把行动设在 22:00、免打扰设为 22:30–07:00 时，
   * 那条提醒应当顺延到 07:00，而不是凭空消失。
   *
   * <p>推迟次数计入 {@code attemptCount}：免打扰配置异常时不能让任务被无限推迟，
   * 达到上限后由调用方按 {@link #failed} 收口。
   *
   * @param nextAttempt 下一次允许投递的时刻；必须晚于当前时刻
   * @return 是否接受了这次推迟
   */
  public boolean deferredTo(Instant nextAttempt, int maxDeferrals, LocalDateTime now) {
    if (status != NotificationTaskStatus.SENDING) {
      throw new BusinessException("ENG_TASK_NOT_SENDING", "通知任务不在投递中");
    }
    if (nextAttempt == null || !nextAttempt.isAfter(Instant.now())) {
      return false;
    }
    if (attemptCount >= maxDeferrals) {
      return false;
    }
    deferredUntil = nextAttempt;
    attemptCount++;
    status = NotificationTaskStatus.PENDING;
    lastError = DEFERRED_REASON;
    updatedAt = now;
    return true;
  }

  public void failed(String error, boolean retryable, LocalDateTime now) {    attemptCount++;
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

  /** 因免打扰被推迟到的时刻；null 表示没有被推迟过。 */
  public Instant getDeferredUntil() {
    return deferredUntil;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
