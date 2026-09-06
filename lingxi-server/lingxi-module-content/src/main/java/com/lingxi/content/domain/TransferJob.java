package com.lingxi.content.domain;

import com.lingxi.kernel.BusinessException;
import java.time.*;

/** 导入/导出任务聚合。 */
public class TransferJob {
  public enum Type {
    IMPORT,
    EXPORT
  }

  public enum Status {
    PENDING,
    PARSING,
    PREVIEW_READY,
    CONFIRMED,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED
  }

  private final long id;
  private final String requestKey;
  private final long userId;
  private final Type type;
  private final Long sourceFileId;
  private final String scopeJson, format;
  private Status status;
  private String previewJson, errorJson;
  private Long resultFileId;
  private Instant expiresAt;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private TransferJob(
      long id,
      String key,
      long user,
      Type type,
      Long source,
      String scope,
      String format,
      Status status,
      String preview,
      String error,
      Long result,
      Instant expires,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    this.id = id;
    requestKey = key;
    userId = user;
    this.type = type;
    sourceFileId = source;
    scopeJson = scope;
    this.format = format;
    this.status = status;
    previewJson = preview;
    errorJson = error;
    resultFileId = result;
    expiresAt = expires;
    this.version = version;
    createdAt = created;
    updatedAt = updated;
  }

  public static TransferJob importJob(
      long id, String key, long user, long source, String format, LocalDateTime now) {
    return create(id, key, user, Type.IMPORT, source, null, format, now);
  }

  public static TransferJob exportJob(
      long id, String key, long user, String scope, String format, LocalDateTime now) {
    return create(id, key, user, Type.EXPORT, null, scope, format, now);
  }

  private static TransferJob create(
      long id,
      String key,
      long user,
      Type type,
      Long source,
      String scope,
      String format,
      LocalDateTime now) {
    if (id <= 0 || user <= 0 || key == null || key.isBlank() || format == null || format.isBlank())
      throw new BusinessException("CONTENT_INVALID_JOB", "传输任务参数不合法");
    return new TransferJob(
        id,
        key,
        user,
        type,
        source,
        scope,
        format,
        Status.PENDING,
        null,
        null,
        null,
        null,
        0,
        now,
        now);
  }

  public static TransferJob rehydrate(
      long id,
      String key,
      long user,
      Type type,
      Long source,
      String scope,
      String format,
      Status status,
      String preview,
      String error,
      Long result,
      Instant expires,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    return new TransferJob(
        id, key, user, type, source, scope, format, status, preview, error, result, expires,
        version, created, updated);
  }

  public void preview(long user, String preview, String error, long expected, LocalDateTime now) {
    owner(user);
    check(expected);
    if (type != Type.IMPORT || status != Status.PENDING && status != Status.PARSING)
      throw new BusinessException("CONTENT_JOB_STATE_INVALID", "导入任务状态不允许预览");
    previewJson = preview;
    errorJson = error;
    status = error == null ? Status.PREVIEW_READY : Status.FAILED;
    version++;
    updatedAt = now;
  }

  public void confirm(long user, long expected, LocalDateTime now) {
    owner(user);
    check(expected);
    if (type != Type.IMPORT || status != Status.PREVIEW_READY)
      throw new BusinessException("CONTENT_JOB_PREVIEW_REQUIRED", "必须先完成无错误预览");
    status = Status.CONFIRMED;
    version++;
    updatedAt = now;
  }

  public void complete(Long fileId, String error, long expected, LocalDateTime now) {
    check(expected);
    if (type != Type.EXPORT || status == Status.COMPLETED)
      throw new BusinessException("CONTENT_JOB_STATE_INVALID", "导出任务状态不允许完成");
    resultFileId = fileId;
    errorJson = error;
    status = error == null ? Status.COMPLETED : Status.FAILED;
    expiresAt = error == null ? now.toInstant(ZoneOffset.UTC).plus(Duration.ofHours(24)) : null;
    version++;
    updatedAt = now;
  }

  public void completeImport(String error, long expected, LocalDateTime now) {
    check(expected);
    if (type != Type.IMPORT || status != Status.CONFIRMED)
      throw new BusinessException("CONTENT_JOB_STATE_INVALID", "导入任务状态不允许完成");
    errorJson = error;
    status = error == null ? Status.COMPLETED : Status.FAILED;
    version++;
    updatedAt = now;
  }

  public void expire(long user, long expected, LocalDateTime now) {
    owner(user);
    check(expected);
    if (status == Status.COMPLETED
        && expiresAt != null
        && !expiresAt.isAfter(now.toInstant(ZoneOffset.UTC))) {
      status = Status.EXPIRED;
      resultFileId = null;
      version++;
      updatedAt = now;
    }
  }

  public void owner(long user) {
    if (userId != user) throw new BusinessException("CONTENT_JOB_NOT_FOUND", "任务不存在");
  }

  private void check(long expected) {
    if (version != expected) throw new BusinessException("CONTENT_JOB_CONFLICT", "任务已变化");
  }

  public long getId() {
    return id;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public long getUserId() {
    return userId;
  }

  public Type getType() {
    return type;
  }

  public Long getSourceFileId() {
    return sourceFileId;
  }

  public String getScopeJson() {
    return scopeJson;
  }

  public String getFormat() {
    return format;
  }

  public Status getStatus() {
    return status;
  }

  public String getPreviewJson() {
    return previewJson;
  }

  public String getErrorJson() {
    return errorJson;
  }

  public Long getResultFileId() {
    return resultFileId;
  }

  public Instant getExpiresAt() {
    return expiresAt;
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
