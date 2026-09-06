package com.lingxi.goal.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import java.util.Objects;

/** 不可变计划版本快照；草案与激活均通过创建新值表达状态变化。 */
public record PlanVersion(
    long id,
    long goalId,
    int versionNo,
    String requestKey,
    String requestDigest,
    String snapshotJson,
    String adjustmentReason,
    PlanVersionStatus status,
    String source,
    LocalDateTime activatedAt,
    LocalDateTime createdAt) {
  public PlanVersion {
    if (id <= 0 || goalId <= 0 || versionNo <= 0) {
      throw new BusinessException("GOAL_INVALID_PLAN_VERSION", "计划版本标识不合法");
    }
    Objects.requireNonNull(requestKey);
    Objects.requireNonNull(requestDigest);
    Objects.requireNonNull(status);
    if (snapshotJson == null || snapshotJson.isBlank()) {
      throw new BusinessException("GOAL_EMPTY_PLAN", "计划内容不能为空");
    }
    Objects.requireNonNull(source);
    Objects.requireNonNull(createdAt);
    if (status == PlanVersionStatus.ACTIVE && activatedAt == null) {
      throw new BusinessException("GOAL_PLAN_NOT_ACTIVATED", "生效计划缺少确认时间");
    }
  }

  public PlanVersion(
      long id,
      long goalId,
      int versionNo,
      String requestKey,
      String requestDigest,
      String snapshotJson,
      String adjustmentReason,
      LocalDateTime activatedAt,
      LocalDateTime createdAt) {
    this(
        id,
        goalId,
        versionNo,
        requestKey,
        requestDigest,
        snapshotJson,
        adjustmentReason,
        PlanVersionStatus.ACTIVE,
        "USER_CONFIRMED",
        activatedAt,
        createdAt);
  }

  public static PlanVersion draft(
      long id,
      long goalId,
      int versionNo,
      String requestKey,
      String requestDigest,
      String snapshotJson,
      String adjustmentReason,
      String source,
      LocalDateTime createdAt) {
    return new PlanVersion(
        id,
        goalId,
        versionNo,
        requestKey,
        requestDigest,
        snapshotJson,
        adjustmentReason,
        PlanVersionStatus.PENDING_CONFIRMATION,
        source,
        null,
        createdAt);
  }

  public PlanVersion activate(LocalDateTime now) {
    if (status != PlanVersionStatus.PENDING_CONFIRMATION && status != PlanVersionStatus.DRAFT) {
      throw new BusinessException("GOAL_PLAN_NOT_CONFIRMABLE", "计划当前状态不可确认");
    }
    return new PlanVersion(
        id,
        goalId,
        versionNo,
        requestKey,
        requestDigest,
        snapshotJson,
        adjustmentReason,
        PlanVersionStatus.ACTIVE,
        source,
        now,
        createdAt);
  }
}
