package com.lingxi.goal.domain;

import com.lingxi.goal.api.GoalStatus;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import java.util.Objects;

/** 目标聚合，维护状态、当前计划引用和并发版本。 */
public class Goal {
  private final long id;
  private final String publicId;
  private final long userId;
  private final String requestKey;
  private final String requestDigest;
  private final String title;
  private final String successCriteria;
  private GoalStatus status;
  private Long currentPlanVersionId;
  private int progress;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private Goal(
      long id,
      String publicId,
      long userId,
      String requestKey,
      String requestDigest,
      String title,
      String successCriteria,
      GoalStatus status,
      Long currentPlanVersionId,
      int progress,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.publicId = Objects.requireNonNull(publicId);
    this.userId = userId;
    this.requestKey = Objects.requireNonNull(requestKey);
    this.requestDigest = Objects.requireNonNull(requestDigest);
    this.title = requireText(title, "目标标题不能为空");
    this.successCriteria = requireText(successCriteria, "成功标准不能为空");
    this.status = Objects.requireNonNull(status);
    this.currentPlanVersionId = currentPlanVersionId;
    this.progress = progress;
    this.version = version;
    this.createdAt = Objects.requireNonNull(createdAt);
    this.updatedAt = Objects.requireNonNull(updatedAt);
  }

  public static Goal create(
      long id,
      String publicId,
      long userId,
      String requestKey,
      String requestDigest,
      String title,
      String successCriteria,
      LocalDateTime now) {
    if (id <= 0 || userId <= 0) {
      throw new BusinessException("GOAL_INVALID_OWNER", "目标标识或用户标识不合法");
    }
    return new Goal(
        id,
        publicId,
        userId,
        requestKey,
        requestDigest,
        title,
        successCriteria,
        GoalStatus.DRAFT,
        null,
        0,
        0,
        now,
        now);
  }

  public static Goal rehydrate(
      long id,
      String publicId,
      long userId,
      String requestKey,
      String requestDigest,
      String title,
      String successCriteria,
      GoalStatus status,
      Long currentPlanVersionId,
      int progress,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    return new Goal(
        id,
        publicId,
        userId,
        requestKey,
        requestDigest,
        title,
        successCriteria,
        status,
        currentPlanVersionId,
        progress,
        version,
        createdAt,
        updatedAt);
  }

  public void activatePlan(long planVersionId, long expectedVersion, LocalDateTime now) {
    if (version != expectedVersion) {
      throw new BusinessException("GOAL_VERSION_CONFLICT", "目标已被其他终端更新");
    }
    if (status == GoalStatus.COMPLETED || status == GoalStatus.ARCHIVED) {
      throw new BusinessException("GOAL_PLAN_NOT_ALLOWED", "当前目标状态不能调整计划");
    }
    currentPlanVersionId = planVersionId;
    status = GoalStatus.ACTIVE;
    version++;
    updatedAt = now;
  }

  public void updateProgress(int calculatedProgress, LocalDateTime now) {
    if (calculatedProgress < 0 || calculatedProgress > 100) {
      throw new BusinessException("GOAL_INVALID_PROGRESS", "目标进度不合法");
    }
    progress = calculatedProgress;
    if (progress == 100 && status == GoalStatus.ACTIVE) {
      status = GoalStatus.COMPLETED;
    }
    version++;
    updatedAt = now;
  }

  public void assertOwnedBy(long operatorUserId) {
    if (userId != operatorUserId) {
      throw new BusinessException("GOAL_NOT_FOUND", "目标不存在");
    }
  }

  private static String requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new BusinessException("GOAL_INVALID_DEFINITION", message);
    }
    return value.trim();
  }

  public long getId() {
    return id;
  }

  public String getPublicId() {
    return publicId;
  }

  public long getUserId() {
    return userId;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public String getRequestDigest() {
    return requestDigest;
  }

  public String getTitle() {
    return title;
  }

  public String getSuccessCriteria() {
    return successCriteria;
  }

  public GoalStatus getStatus() {
    return status;
  }

  public Long getCurrentPlanVersionId() {
    return currentPlanVersionId;
  }

  public int getProgress() {
    return progress;
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
