package com.lingxi.content.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;

/** 私有文件状态聚合，READY 前不可被业务引用。 */
public class FileAsset {
  public enum Status {
    UPLOADING,
    UPLOADED,
    SCANNING,
    READY,
    REJECTED,
    QUARANTINED,
    DELETING,
    DELETED
  }

  private final long id;
  private final String publicId, requestKey;
  private final long ownerUserId;
  private final String purpose, originalName, objectKey, contentHash, mimeType, sensitivity;
  private final long sizeBytes;
  private Status status;
  private String scanResult;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private FileAsset(
      long id,
      String publicId,
      String requestKey,
      long owner,
      String purpose,
      String name,
      String key,
      String hash,
      long size,
      String mime,
      String sensitivity,
      Status status,
      String scan,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    this.id = id;
    this.publicId = publicId;
    this.requestKey = requestKey;
    ownerUserId = owner;
    this.purpose = purpose;
    originalName = name;
    objectKey = key;
    contentHash = hash;
    sizeBytes = size;
    mimeType = mime;
    this.sensitivity = sensitivity;
    this.status = status;
    scanResult = scan;
    this.version = version;
    createdAt = created;
    updatedAt = updated;
  }

  public static FileAsset create(
      long id,
      String publicId,
      String requestKey,
      long owner,
      String purpose,
      String name,
      String key,
      String hash,
      long size,
      String mime,
      String sensitivity,
      LocalDateTime now) {
    if (id <= 0
        || owner <= 0
        || requestKey == null
        || requestKey.isBlank()
        || name == null
        || name.isBlank()
        || key == null
        || hash == null
        || hash.isBlank()
        || size <= 0
        || size > 104857600L
        || mime == null
        || mime.isBlank()) throw new BusinessException("CONTENT_INVALID_FILE", "文件参数不合法");
    return new FileAsset(
        id,
        publicId,
        requestKey,
        owner,
        purpose,
        name,
        key,
        hash,
        size,
        mime,
        sensitivity,
        Status.UPLOADING,
        null,
        0,
        now,
        now);
  }

  public static FileAsset rehydrate(
      long id,
      String publicId,
      String requestKey,
      long owner,
      String purpose,
      String name,
      String key,
      String hash,
      long size,
      String mime,
      String sensitivity,
      Status status,
      String scan,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    return new FileAsset(
        id,
        publicId,
        requestKey,
        owner,
        purpose,
        name,
        key,
        hash,
        size,
        mime,
        sensitivity,
        status,
        scan,
        version,
        created,
        updated);
  }

  public void uploaded(long owner, long expected, LocalDateTime now) {
    owner(owner);
    check(expected);
    if (status != Status.UPLOADING)
      throw new BusinessException("CONTENT_FILE_STATE_INVALID", "文件不在上传中");
    status = Status.UPLOADED;
    version++;
    updatedAt = now;
  }

  public void scanning(long expected, LocalDateTime now) {
    check(expected);
    if (status != Status.UPLOADED)
      throw new BusinessException("CONTENT_FILE_STATE_INVALID", "文件尚未上传");
    status = Status.SCANNING;
    version++;
    updatedAt = now;
  }

  public void scanned(boolean safe, String detail, long expected, LocalDateTime now) {
    check(expected);
    if (status != Status.SCANNING)
      throw new BusinessException("CONTENT_FILE_STATE_INVALID", "文件不在扫描中");
    status = safe ? Status.READY : Status.QUARANTINED;
    scanResult = detail;
    version++;
    updatedAt = now;
  }

  public void assertReady(long owner) {
    owner(owner);
    if (status != Status.READY) throw new BusinessException("CONTENT_FILE_NOT_READY", "文件尚不可引用");
  }

  public void assertOwned(long owner) {
    owner(owner);
  }

  public void beginDeletion(long owner, long expected, LocalDateTime now) {
    owner(owner);
    check(expected);
    if (status == Status.DELETING || status == Status.DELETED) return;
    status = Status.DELETING;
    version++;
    updatedAt = now;
  }

  public void completeDeletion(long expected, LocalDateTime now) {
    check(expected);
    if (status == Status.DELETED) return;
    if (status != Status.DELETING)
      throw new BusinessException("CONTENT_FILE_STATE_INVALID", "文件不在删除中");
    status = Status.DELETED;
    scanResult = null;
    version++;
    updatedAt = now;
  }

  private void owner(long user) {
    if (ownerUserId != user) throw new BusinessException("CONTENT_FILE_NOT_FOUND", "文件不存在");
  }

  private void check(long expected) {
    if (version != expected) throw new BusinessException("CONTENT_FILE_CONFLICT", "文件状态已变化");
  }

  public long getId() {
    return id;
  }

  public String getPublicId() {
    return publicId;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public long getOwnerUserId() {
    return ownerUserId;
  }

  public String getPurpose() {
    return purpose;
  }

  public String getOriginalName() {
    return originalName;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public String getContentHash() {
    return contentHash;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getSensitivity() {
    return sensitivity;
  }

  public Status getStatus() {
    return status;
  }

  public String getScanResult() {
    return scanResult;
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
