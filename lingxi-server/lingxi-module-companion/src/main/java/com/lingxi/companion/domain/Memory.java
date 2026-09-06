package com.lingxi.companion.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;

/** 用户可控长期记忆；删除先阻断读取，再异步传播到向量和缓存投影。 */
public class Memory {
  public enum Status {
    ACTIVE,
    DELETING,
    DELETED
  }

  private final long id, userId;
  private final String purpose, sourceRef, sensitivity;
  private String contentText;
  private Status status;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private Memory(
      long id,
      long userId,
      String purpose,
      String contentText,
      String sourceRef,
      String sensitivity,
      Status status,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.userId = userId;
    this.purpose = purpose;
    this.contentText = contentText;
    this.sourceRef = sourceRef;
    this.sensitivity = sensitivity;
    this.status = status;
    this.version = version;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Memory rehydrate(
      long id,
      long userId,
      String purpose,
      String contentText,
      String sourceRef,
      String sensitivity,
      Status status,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    return new Memory(
        id,
        userId,
        purpose,
        contentText,
        sourceRef,
        sensitivity,
        status,
        version,
        createdAt,
        updatedAt);
  }

  public void update(long userId, String contentText, long expectedVersion, LocalDateTime now) {
    owner(userId);
    check(expectedVersion);
    if (status != Status.ACTIVE || contentText == null || contentText.isBlank()) invalid();
    this.contentText = contentText;
    advance(now);
  }

  public void beginDeletion(long userId, long expectedVersion, LocalDateTime now) {
    owner(userId);
    check(expectedVersion);
    if (status == Status.DELETED || status == Status.DELETING) return;
    status = Status.DELETING;
    contentText = "";
    advance(now);
  }

  public void completeDeletion(long expectedVersion, LocalDateTime now) {
    check(expectedVersion);
    if (status == Status.DELETED) return;
    if (status != Status.DELETING) invalid();
    status = Status.DELETED;
    advance(now);
  }

  public void assertOwner(long userId) {
    owner(userId);
  }

  private void owner(long userId) {
    if (this.userId != userId) throw new BusinessException("AGENT_MEMORY_NOT_FOUND", "记忆不存在");
  }

  private void check(long expected) {
    if (version != expected) throw new BusinessException("AGENT_MEMORY_CONFLICT", "记忆已变化");
  }

  private void invalid() {
    throw new BusinessException("AGENT_MEMORY_STATE_INVALID", "记忆状态不允许当前操作");
  }

  private void advance(LocalDateTime now) {
    version++;
    updatedAt = now;
  }

  public long getId() {
    return id;
  }

  public long getUserId() {
    return userId;
  }

  public String getPurpose() {
    return purpose;
  }

  public String getContentText() {
    return contentText;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getSensitivity() {
    return sensitivity;
  }

  public Status getStatus() {
    return status;
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
