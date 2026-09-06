package com.lingxi.relationship.domain;

import com.lingxi.kernel.BusinessException;
import java.time.*;
import java.util.Set;

/** 仅保存令牌摘要和创建时字段快照的受控分享聚合。 */
public class ShareLink {
  public enum Status {
    ACTIVE,
    EXPIRED,
    REVOKED,
    EXHAUSTED
  }

  private final long id;
  private final String requestKey, requestDigest;
  private final long ownerUserId;
  private final String resourceType, resourceId;
  private final Set<String> fields;
  private final String snapshotJson, tokenHash, passwordHash;
  private final Instant expiresAt;
  private final Integer visitLimit;
  private final int visitCount;
  private Status status;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private ShareLink(
      long id,
      String key,
      String digest,
      long owner,
      String type,
      String resourceId,
      Set<String> fields,
      String snapshot,
      String tokenHash,
      String passwordHash,
      Instant expiresAt,
      Integer limit,
      int count,
      Status status,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    requestKey = key;
    requestDigest = digest;
    ownerUserId = owner;
    resourceType = type;
    this.resourceId = resourceId;
    this.fields = Set.copyOf(fields);
    snapshotJson = snapshot;
    this.tokenHash = tokenHash;
    this.passwordHash = passwordHash;
    this.expiresAt = expiresAt;
    visitLimit = limit;
    visitCount = count;
    this.status = status;
    this.version = version;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static ShareLink create(
      long id,
      String key,
      String digest,
      long owner,
      String type,
      String resourceId,
      Set<String> fields,
      String snapshot,
      String tokenHash,
      String passwordHash,
      Instant expiresAt,
      Integer limit,
      LocalDateTime now) {
    if (id <= 0
        || owner <= 0
        || key == null
        || key.isBlank()
        || type == null
        || resourceId == null
        || fields == null
        || fields.isEmpty()
        || snapshot == null
        || tokenHash == null
        || expiresAt == null
        || !expiresAt.isAfter(now.toInstant(ZoneOffset.UTC))
        || (limit != null && limit <= 0))
      throw new BusinessException("REL_INVALID_SHARE", "分享参数不合法");
    return new ShareLink(
        id,
        key,
        digest,
        owner,
        type,
        resourceId,
        fields,
        snapshot,
        tokenHash,
        passwordHash,
        expiresAt,
        limit,
        0,
        Status.ACTIVE,
        0,
        now,
        now);
  }

  public static ShareLink rehydrate(
      long id,
      String key,
      String digest,
      long owner,
      String type,
      String resourceId,
      Set<String> fields,
      String snapshot,
      String tokenHash,
      String passwordHash,
      Instant expiresAt,
      Integer limit,
      int count,
      Status status,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    return new ShareLink(
        id,
        key,
        digest,
        owner,
        type,
        resourceId,
        fields,
        snapshot,
        tokenHash,
        passwordHash,
        expiresAt,
        limit,
        count,
        status,
        version,
        createdAt,
        updatedAt);
  }

  public void assertAccessible(Instant now) {
    if (status != Status.ACTIVE) throw new BusinessException("REL_SHARE_UNAVAILABLE", "分享已失效");
    if (!expiresAt.isAfter(now)) throw new BusinessException("REL_SHARE_EXPIRED", "分享已过期");
    if (visitLimit != null && visitCount >= visitLimit)
      throw new BusinessException("REL_SHARE_EXHAUSTED", "分享访问次数已用尽");
  }

  public void revoke(long owner, long expected, LocalDateTime now) {
    if (owner != ownerUserId) throw new BusinessException("REL_SHARE_NOT_FOUND", "分享不存在");
    if (version != expected) throw new BusinessException("REL_SHARE_CONFLICT", "分享已变化");
    if (status != Status.REVOKED) {
      status = Status.REVOKED;
      version++;
      updatedAt = now;
    }
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

  public long getOwnerUserId() {
    return ownerUserId;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public Set<String> getFields() {
    return fields;
  }

  public String getSnapshotJson() {
    return snapshotJson;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Integer getVisitLimit() {
    return visitLimit;
  }

  public int getVisitCount() {
    return visitCount;
  }

  public ShareLink.Status getStatus() {
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
