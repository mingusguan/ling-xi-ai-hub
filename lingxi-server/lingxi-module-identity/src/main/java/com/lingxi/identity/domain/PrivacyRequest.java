package com.lingxi.identity.domain;

import com.lingxi.identity.api.PrivacyRequestStatus;
import com.lingxi.identity.api.PrivacyRequestType;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import java.util.Objects;

/** 可恢复的隐私权利请求聚合。 */
public class PrivacyRequest {
  private final long id;
  private final String requestKey;
  private final String requestDigest;
  private final long userId;
  private final PrivacyRequestType type;
  private final String scopeJson;
  private PrivacyRequestStatus status;
  private int progress;
  private final LocalDateTime deadline;
  private String resultReference;
  private String lastError;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private PrivacyRequest(
      long id,
      String requestKey,
      String requestDigest,
      long userId,
      PrivacyRequestType type,
      String scopeJson,
      PrivacyRequestStatus status,
      int progress,
      LocalDateTime deadline,
      String resultReference,
      String lastError,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.requestKey = Objects.requireNonNull(requestKey);
    this.requestDigest = Objects.requireNonNull(requestDigest);
    this.userId = userId;
    this.type = Objects.requireNonNull(type);
    this.scopeJson = Objects.requireNonNull(scopeJson);
    this.status = Objects.requireNonNull(status);
    this.progress = progress;
    this.deadline = Objects.requireNonNull(deadline);
    this.resultReference = resultReference;
    this.lastError = lastError;
    this.version = version;
    this.createdAt = Objects.requireNonNull(createdAt);
    this.updatedAt = Objects.requireNonNull(updatedAt);
  }

  public static PrivacyRequest create(
      long id,
      String requestKey,
      String digest,
      long userId,
      PrivacyRequestType type,
      String scopeJson,
      LocalDateTime now) {
    if (id <= 0 || userId <= 0) {
      throw new BusinessException("PRIVACY_INVALID_REQUEST", "隐私请求标识不合法");
    }
    return new PrivacyRequest(
        id,
        requestKey,
        digest,
        userId,
        type,
        scopeJson,
        PrivacyRequestStatus.PENDING,
        0,
        now.plusDays(30),
        null,
        null,
        0,
        now,
        now);
  }

  public static PrivacyRequest rehydrate(
      long id,
      String requestKey,
      String digest,
      long userId,
      PrivacyRequestType type,
      String scopeJson,
      PrivacyRequestStatus status,
      int progress,
      LocalDateTime deadline,
      String resultReference,
      String lastError,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    return new PrivacyRequest(
        id,
        requestKey,
        digest,
        userId,
        type,
        scopeJson,
        status,
        progress,
        deadline,
        resultReference,
        lastError,
        version,
        createdAt,
        updatedAt);
  }

  /** 将待处理请求推进到执行中；重复执行保持幂等。 */
  public void start(LocalDateTime now) {
    if (status == PrivacyRequestStatus.COMPLETED || status == PrivacyRequestStatus.CANCELLED) {
      return;
    }
    status = PrivacyRequestStatus.PROCESSING;
    progress = Math.max(progress, 1);
    lastError = null;
    touch(now);
  }

  /** 记录可恢复检查点，进度只能前进。 */
  public void checkpoint(int nextProgress, LocalDateTime now) {
    if (nextProgress < progress || nextProgress < 1 || nextProgress > 99) {
      throw new BusinessException("PRIVACY_INVALID_PROGRESS", "隐私请求进度不合法");
    }
    status = PrivacyRequestStatus.PROCESSING;
    progress = nextProgress;
    touch(now);
  }

  /** 完成请求并保存不含敏感明文的结果引用。 */
  public void complete(String reference, LocalDateTime now) {
    status = PrivacyRequestStatus.COMPLETED;
    progress = 100;
    resultReference = reference;
    lastError = null;
    touch(now);
  }

  /** 自动处理完成后进入人工阶段，不得向用户声明请求已经完成。 */
  public void awaitManual(String reference, LocalDateTime now) {
    if (reference == null || reference.isBlank()) {
      throw new BusinessException("PRIVACY_MANUAL_REFERENCE_REQUIRED", "人工处理引用不能为空");
    }
    status = PrivacyRequestStatus.WAITING_MANUAL;
    progress = Math.max(progress, 95);
    resultReference = reference;
    lastError = null;
    touch(now);
  }

  /** 由关联人工工单完成更正请求，严格校验工单引用，防止串单。 */
  public void completeManual(String expectedReference, LocalDateTime now) {
    if (status == PrivacyRequestStatus.COMPLETED) {
      return;
    }
    if (status != PrivacyRequestStatus.WAITING_MANUAL
        || !Objects.equals(resultReference, expectedReference)) {
      throw new BusinessException("PRIVACY_MANUAL_COMPLETION_INVALID", "隐私更正请求与人工工单不匹配");
    }
    complete(expectedReference, now);
  }

  public void resumeManual(String expectedReference, LocalDateTime now) {
    if (type != PrivacyRequestType.CLOSE_ACCOUNT
        || status != PrivacyRequestStatus.WAITING_MANUAL
        || !Objects.equals(resultReference, expectedReference)) {
      throw new BusinessException("PRIVACY_MANUAL_RESUME_INVALID", "注销请求与人工工单不匹配");
    }
    status = PrivacyRequestStatus.PENDING;
    resultReference = null;
    lastError = null;
    touch(now);
  }

  /** 记录失败原因，后台任务可据此继续重试。 */
  public void fail(String error, LocalDateTime now) {
    status = PrivacyRequestStatus.FAILED;
    lastError = error == null ? "UNKNOWN" : error.substring(0, Math.min(error.length(), 1000));
    touch(now);
  }

  public void cancel(LocalDateTime now) {
    if (type != PrivacyRequestType.CLOSE_ACCOUNT || status != PrivacyRequestStatus.PENDING) {
      throw new BusinessException("PRIVACY_CLOSURE_NOT_CANCELLABLE", "注销请求已不在可撤销冷静期");
    }
    status = PrivacyRequestStatus.CANCELLED;
    resultReference = null;
    lastError = null;
    touch(now);
  }

  private void touch(LocalDateTime now) {
    version++;
    updatedAt = Objects.requireNonNull(now);
  }

  public long getId() {
    return id;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public String getRequestDigest() {
    return requestDigest;
  }

  public long getUserId() {
    return userId;
  }

  public PrivacyRequestType getType() {
    return type;
  }

  public String getScopeJson() {
    return scopeJson;
  }

  public PrivacyRequestStatus getStatus() {
    return status;
  }

  public int getProgress() {
    return progress;
  }

  public LocalDateTime getDeadline() {
    return deadline;
  }

  public String getResultReference() {
    return resultReference;
  }

  public String getLastError() {
    return lastError;
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
