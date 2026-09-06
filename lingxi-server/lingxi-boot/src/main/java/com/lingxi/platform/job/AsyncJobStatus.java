package com.lingxi.platform.job;

/** 异步任务生命周期状态。 */
enum AsyncJobStatus {
  PENDING,
  RUNNING,
  RETRY_WAIT,
  SUCCEEDED,
  FAILED,
  CANCELLED
}
