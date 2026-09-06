package com.lingxi.operations.domain;

import com.lingxi.kernel.BusinessException;
import java.time.*;

/** 不可原地覆盖的运营配置发布聚合。 */
public class ConfigRelease {
  public enum Status {
    DRAFT,
    VALIDATING,
    PENDING_APPROVAL,
    GRAY,
    PUBLISHED,
    FAILED,
    ROLLED_BACK
  }

  private final long id;
  private final String releaseKey, configType, contentRef, contentDigest, grayRule;
  private final int versionNo;
  private Status status;
  private final long createdBy;
  private Long approvedBy, publishedBy, previousReleaseId;
  private String failureReason;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private ConfigRelease(
      long id,
      String key,
      String type,
      int no,
      String ref,
      String digest,
      String gray,
      Status status,
      long creator,
      Long approver,
      Long publisher,
      Long previous,
      String failure,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    this.id = id;
    releaseKey = key;
    configType = type;
    versionNo = no;
    contentRef = ref;
    contentDigest = digest;
    grayRule = gray;
    this.status = status;
    createdBy = creator;
    approvedBy = approver;
    publishedBy = publisher;
    previousReleaseId = previous;
    failureReason = failure;
    this.version = version;
    createdAt = created;
    updatedAt = updated;
  }

  public static ConfigRelease create(
      long id,
      String key,
      String type,
      int no,
      String ref,
      String digest,
      String gray,
      long creator,
      LocalDateTime now) {
    if (id <= 0
        || creator <= 0
        || key == null
        || key.isBlank()
        || type == null
        || type.isBlank()
        || no <= 0
        || ref == null
        || digest == null
        || digest.isBlank()) throw new BusinessException("OPS_INVALID_RELEASE", "配置发布参数不合法");
    return new ConfigRelease(
        id,
        key,
        type,
        no,
        ref,
        digest,
        gray,
        Status.DRAFT,
        creator,
        null,
        null,
        null,
        null,
        0,
        now,
        now);
  }

  public static ConfigRelease rehydrate(
      long id,
      String key,
      String type,
      int no,
      String ref,
      String digest,
      String gray,
      Status status,
      long creator,
      Long approver,
      Long publisher,
      Long previous,
      String failure,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    return new ConfigRelease(
        id, key, type, no, ref, digest, gray, status, creator, approver, publisher, previous,
        failure, version, created, updated);
  }

  public void validated(long expected, LocalDateTime now) {
    check(expected);
    require(Status.DRAFT, Status.FAILED);
    status = Status.PENDING_APPROVAL;
    failureReason = null;
    version++;
    updatedAt = now;
  }

  public void fail(String reason, long expected, LocalDateTime now) {
    check(expected);
    status = Status.FAILED;
    failureReason = reason;
    version++;
    updatedAt = now;
  }

  public void approve(long admin, long expected, LocalDateTime now) {
    check(expected);
    require(Status.PENDING_APPROVAL);
    if (admin == createdBy) throw new BusinessException("OPS_TWO_PERSON_REQUIRED", "创建人与审批人必须不同");
    if (approvedBy != null) throw new BusinessException("OPS_RELEASE_ALREADY_APPROVED", "配置已经审批");
    approvedBy = admin;
    version++;
    updatedAt = now;
  }

  public void gray(long admin, long expected, Long previous, LocalDateTime now) {
    check(expected);
    require(Status.PENDING_APPROVAL);
    if (approvedBy == null) throw new BusinessException("OPS_APPROVAL_REQUIRED", "配置尚未审批");
    status = Status.GRAY;
    publishedBy = admin;
    previousReleaseId = previous;
    version++;
    updatedAt = now;
  }

  public void publish(long admin, long expected, LocalDateTime now) {
    check(expected);
    require(Status.GRAY);
    status = Status.PUBLISHED;
    publishedBy = admin;
    version++;
    updatedAt = now;
  }

  public void rollback(long admin, long expected, LocalDateTime now) {
    check(expected);
    require(Status.GRAY, Status.PUBLISHED);
    status = Status.ROLLED_BACK;
    publishedBy = admin;
    version++;
    updatedAt = now;
  }

  private void require(Status... s) {
    for (Status x : s) if (status == x) return;
    throw new BusinessException("OPS_RELEASE_STATE_INVALID", "配置发布状态不允许该操作");
  }

  private void check(long e) {
    if (version != e) throw new BusinessException("OPS_RELEASE_CONFLICT", "配置发布已变化");
  }

  public long getId() {
    return id;
  }

  public String getReleaseKey() {
    return releaseKey;
  }

  public String getConfigType() {
    return configType;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public String getContentRef() {
    return contentRef;
  }

  public String getContentDigest() {
    return contentDigest;
  }

  public String getGrayRule() {
    return grayRule;
  }

  public Status getStatus() {
    return status;
  }

  public long getCreatedBy() {
    return createdBy;
  }

  public Long getApprovedBy() {
    return approvedBy;
  }

  public Long getPublishedBy() {
    return publishedBy;
  }

  public Long getPreviousReleaseId() {
    return previousReleaseId;
  }

  public String getFailureReason() {
    return failureReason;
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
