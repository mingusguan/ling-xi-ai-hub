package com.lingxi.commerce.domain;

import com.lingxi.kernel.BusinessException;
import java.time.*;
import java.util.Objects;

public class Subscription {
  public enum Status {
    PENDING,
    ACTIVE,
    PAUSED,
    CANCEL_AT_PERIOD_END,
    EXPIRED,
    SUSPENDED
  }

  private final long id, userId, productId;
  private final String channel, channelSubscriptionId;
  private Status status;
  private Instant periodEnd;
  private String cancelMode;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private Subscription(
      long id,
      long user,
      long product,
      String channel,
      String channelId,
      Status status,
      Instant end,
      String cancel,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    this.id = id;
    userId = user;
    productId = product;
    this.channel = channel;
    channelSubscriptionId = channelId;
    this.status = status;
    periodEnd = end;
    cancelMode = cancel;
    this.version = version;
    createdAt = created;
    updatedAt = updated;
  }

  public static Subscription activate(
      long id,
      long user,
      long product,
      String channel,
      String channelId,
      Instant end,
      LocalDateTime now) {
    return new Subscription(
        id, user, product, channel, channelId, Status.ACTIVE, end, null, 0, now, now);
  }

  public static Subscription rehydrate(
      long id,
      long user,
      long product,
      String channel,
      String channelId,
      Status status,
      Instant end,
      String cancel,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    return new Subscription(
        id, user, product, channel, channelId, status, end, cancel, version, created, updated);
  }

  public void assertCancellable(long user, String mode, long expected) {
    if (userId != user) throw new BusinessException("PAY_SUBSCRIPTION_NOT_FOUND", "订阅不存在");
    if (version != expected) throw new BusinessException("PAY_SUBSCRIPTION_CONFLICT", "订阅已变化");
    if (status != Status.ACTIVE)
      throw new BusinessException("PAY_SUBSCRIPTION_STATE_INVALID", "订阅状态不允许取消");
    if (!"IMMEDIATE".equals(mode) && !"PERIOD_END".equals(mode))
      throw new BusinessException("PAY_CANCEL_MODE_INVALID", "取消方式不合法");
  }

  public boolean isCompletedCancellation(long user, String mode, long expected) {
    if (userId != user || !Objects.equals(cancelMode, mode) || version != expected + 1) {
      return false;
    }
    return "IMMEDIATE".equals(mode)
        ? status == Status.EXPIRED
        : "PERIOD_END".equals(mode) && status == Status.CANCEL_AT_PERIOD_END;
  }

  public void cancel(long user, String mode, long expected, LocalDateTime now) {
    assertCancellable(user, mode, expected);
    cancelMode = mode;
    status = "IMMEDIATE".equals(mode) ? Status.EXPIRED : Status.CANCEL_AT_PERIOD_END;
    version++;
    updatedAt = now;
  }

  public boolean reconcile(Status channelStatus, Instant channelPeriodEnd, LocalDateTime now) {
    if (channelStatus == null) {
      throw new BusinessException("PAY_CHANNEL_STATE_INVALID", "渠道订阅状态缺失");
    }
    if (channelStatus == Status.PENDING
        || status == Status.EXPIRED && channelStatus != Status.EXPIRED) {
      throw new BusinessException("PAY_CHANNEL_STATE_INVALID", "渠道订阅状态发生非法回退");
    }
    if (status == channelStatus && Objects.equals(periodEnd, channelPeriodEnd)) return false;
    status = channelStatus;
    periodEnd = channelPeriodEnd;
    version++;
    updatedAt = now;
    return true;
  }

  public long getId() {
    return id;
  }

  public long getUserId() {
    return userId;
  }

  public long getProductId() {
    return productId;
  }

  public String getChannel() {
    return channel;
  }

  public String getChannelSubscriptionId() {
    return channelSubscriptionId;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getPeriodEnd() {
    return periodEnd;
  }

  public String getCancelMode() {
    return cancelMode;
  }

  public long getVersion() {
    return version;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
