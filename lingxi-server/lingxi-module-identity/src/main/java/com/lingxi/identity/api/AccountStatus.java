package com.lingxi.identity.api;

/** C 端账号生命周期状态。 */
public enum AccountStatus {
  PENDING_GUARDIAN,
  ACTIVE_TEEN,
  ACTIVE_ADULT,
  RESTRICTED,
  AGE_TRANSITION,
  CLOSING,
  CLOSED
}
