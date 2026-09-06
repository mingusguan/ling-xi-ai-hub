package com.lingxi.engagement.api;

/** 通知任务状态。 */
public enum NotificationTaskStatus {
  PENDING,
  SENDING,
  SENT,
  FAILED_RETRYABLE,
  FAILED_FINAL,
  CANCELLED
}
