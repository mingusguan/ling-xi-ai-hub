package com.lingxi.identity.api;

/** 隐私请求处理状态。 */
public enum PrivacyRequestStatus {
  PENDING,
  PROCESSING,
  WAITING_MANUAL,
  COMPLETED,
  FAILED,
  CANCELLED
}
