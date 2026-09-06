package com.lingxi.content.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import java.util.Set;

/** 发布后不可修改的模板版本聚合。 */
public class TemplateVersion {
  private static final Set<String> ALLOWED_AGE_SCOPES = Set.of("ALL", "TEEN", "ADULT");

  public enum Status {
    DRAFT,
    REVIEWING,
    PUBLISHED,
    REJECTED,
    RETIRED
  }

  private final long id, templateId;
  private final int versionNo;
  private final String ageScope, contentSnapshot;
  private Status status;
  private Long reviewerUserId;
  private String reviewReason;
  private LocalDateTime publishedAt;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private TemplateVersion(
      long id,
      long templateId,
      int no,
      String age,
      String content,
      Status status,
      Long reviewer,
      String reason,
      LocalDateTime published,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    this.id = id;
    this.templateId = templateId;
    versionNo = no;
    ageScope = age;
    contentSnapshot = content;
    this.status = status;
    reviewerUserId = reviewer;
    reviewReason = reason;
    publishedAt = published;
    this.version = version;
    createdAt = created;
    updatedAt = updated;
  }

  public static TemplateVersion create(
      long id, long templateId, int no, String age, String content, LocalDateTime now) {
    if (id <= 0
        || templateId <= 0
        || no <= 0
        || !ALLOWED_AGE_SCOPES.contains(age)
        || content == null
        || content.isBlank()) throw new BusinessException("CONTENT_INVALID_TEMPLATE", "模板参数不合法");
    return new TemplateVersion(
        id, templateId, no, age, content, Status.DRAFT, null, null, null, 0, now, now);
  }

  public static TemplateVersion rehydrate(
      long id,
      long templateId,
      int no,
      String age,
      String content,
      Status status,
      Long reviewer,
      String reason,
      LocalDateTime published,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    return new TemplateVersion(
        id,
        templateId,
        no,
        age,
        content,
        status,
        reviewer,
        reason,
        published,
        version,
        created,
        updated);
  }

  public void submit(long expected, LocalDateTime now) {
    check(expected);
    require(Status.DRAFT, Status.REJECTED);
    status = Status.REVIEWING;
    version++;
    updatedAt = now;
  }

  public void review(
      long reviewer, boolean approved, String reason, long expected, LocalDateTime now) {
    check(expected);
    require(Status.REVIEWING);
    reviewerUserId = reviewer;
    reviewReason = reason;
    status = approved ? Status.PUBLISHED : Status.REJECTED;
    publishedAt = approved ? now : null;
    version++;
    updatedAt = now;
  }

  public void retire(long expected, LocalDateTime now) {
    check(expected);
    require(Status.PUBLISHED);
    status = Status.RETIRED;
    version++;
    updatedAt = now;
  }

  private void require(Status... allowed) {
    for (Status s : allowed) if (status == s) return;
    throw new BusinessException("CONTENT_TEMPLATE_STATE_INVALID", "模板版本状态不允许该操作");
  }

  private void check(long expected) {
    if (version != expected) throw new BusinessException("CONTENT_TEMPLATE_CONFLICT", "模板版本已变化");
  }

  public long getId() {
    return id;
  }

  public long getTemplateId() {
    return templateId;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public String getAgeScope() {
    return ageScope;
  }

  public String getContentSnapshot() {
    return contentSnapshot;
  }

  public Status getStatus() {
    return status;
  }

  public Long getReviewerUserId() {
    return reviewerUserId;
  }

  public String getReviewReason() {
    return reviewReason;
  }

  public LocalDateTime getPublishedAt() {
    return publishedAt;
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
