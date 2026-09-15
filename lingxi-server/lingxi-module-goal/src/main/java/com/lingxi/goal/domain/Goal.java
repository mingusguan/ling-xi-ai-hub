package com.lingxi.goal.domain;

import com.lingxi.goal.api.GoalClarificationStage;
import com.lingxi.goal.api.GoalStatus;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** 目标聚合，维护状态、当前计划引用和并发版本。 */
public class Goal {
  private final long id;
  private final String publicId;
  private final long userId;
  private final String requestKey;
  private final String requestDigest;
  private GoalDefinition definition;
  private GoalStatus status;
  private Long currentPlanVersionId;
  private int progress;
  /** 暂停时的预计恢复日期；仅暂停状态有意义。 */
  private LocalDate pauseResumeAt;
  /** 放弃原因；仅放弃状态有意义。 */
  private String abandonReason;
  /** 首目标引导澄清阶段（PRD ONB-02）；null 表示不处于引导中。 */
  private GoalClarificationStage clarificationStage;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private Goal(
      long id,
      String publicId,
      long userId,
      String requestKey,
      String requestDigest,
      GoalDefinition definition,
      GoalStatus status,
      Long currentPlanVersionId,
      int progress,
      LocalDate pauseResumeAt,
      String abandonReason,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      GoalClarificationStage clarificationStage) {
    this.id = id;
    this.publicId = Objects.requireNonNull(publicId);
    this.userId = userId;
    this.requestKey = Objects.requireNonNull(requestKey);
    this.requestDigest = Objects.requireNonNull(requestDigest);
    this.definition = Objects.requireNonNull(definition);
    this.status = Objects.requireNonNull(status);
    this.currentPlanVersionId = currentPlanVersionId;
    this.progress = progress;
    this.pauseResumeAt = pauseResumeAt;
    this.abandonReason = abandonReason;
    this.clarificationStage = clarificationStage;
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
      GoalDefinition definition,
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
        definition,
        GoalStatus.DRAFT,
        null,
        0,
        null,
        null,
        0,
        now,
        now,
        // 新建目标默认进入首目标引导的起点；用户直接手动编排计划时会被置为 COMPLETE。
        GoalClarificationStage.AWAITING_GOAL);
  }

  public static Goal rehydrate(
      long id,
      String publicId,
      long userId,
      String requestKey,
      String requestDigest,
      GoalDefinition definition,
      GoalStatus status,
      Long currentPlanVersionId,
      int progress,
      LocalDate pauseResumeAt,
      String abandonReason,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      GoalClarificationStage clarificationStage) {
    return new Goal(
        id,
        publicId,
        userId,
        requestKey,
        requestDigest,
        definition,
        status,
        currentPlanVersionId,
        progress,
        pauseResumeAt,
        abandonReason,
        version,
        createdAt,
        updatedAt,
        clarificationStage);
  }

  /**
   * 推进新手引导澄清阶段（PRD ONB-02）。
   *
   * <p>只记录引导进度，不改变目标状态机：目标始终是草稿，用户中途切到手动创建时
   * 直接确认计划即可，不会因为引导没走完而被状态守卫拦住。
   *
   * <p>阶段变更同样推进乐观版本号——否则两个终端同时推进阶段时后写会静默覆盖前写，
   * 用户会看到自己刚回答完的问题又回来了。重复推进到同一阶段是安全的空操作。
   */
  public void advanceClarification(
      GoalClarificationStage stage, long expectedVersion, LocalDateTime now) {
    if (stage == null) {
      throw new BusinessException("GOAL_INVALID_CLARIFICATION", "引导阶段不能为空");
    }
    if (version != expectedVersion) {
      throw new BusinessException("GOAL_VERSION_CONFLICT", "目标已被其他终端更新");
    }
    if (clarificationStage == stage) {
      return;
    }
    this.clarificationStage = stage;
    version++;
    updatedAt = now;
  }

  /** 结束引导阶段：清空阶段标记，表示不再处于首目标引导中。 */
  public void finishClarification(LocalDateTime now) {
    this.clarificationStage = GoalClarificationStage.COMPLETE;
    this.updatedAt = now;
  }

  public void activatePlan(long planVersionId, long expectedVersion, LocalDateTime now) {
    if (version != expectedVersion) {
      throw new BusinessException("GOAL_VERSION_CONFLICT", "目标已被其他终端更新");
    }
    if (status == GoalStatus.COMPLETED
        || status == GoalStatus.ARCHIVED
        || status == GoalStatus.ABANDONED) {
      throw new BusinessException("GOAL_PLAN_NOT_ALLOWED", "当前目标状态不能调整计划");
    }
    currentPlanVersionId = planVersionId;
    status = GoalStatus.ACTIVE;
    // 恢复为进行中时清空暂停与放弃痕迹，避免同一目标同时带着两种终态信息。
    pauseResumeAt = null;
    abandonReason = null;
    version++;
    updatedAt = now;
  }

  /** 更新目标定义；已归档或已放弃的目标不允许再修改定义。 */
  public void updateDefinition(GoalDefinition updated, long expectedVersion, LocalDateTime now) {
    assertVersion(expectedVersion);
    if (status == GoalStatus.ARCHIVED || status == GoalStatus.ABANDONED) {
      throw new BusinessException("GOAL_DEFINITION_NOT_ALLOWED", "当前目标状态不能修改目标定义");
    }
    definition = Objects.requireNonNull(updated);
    version++;
    updatedAt = now;
  }

  /**
   * 暂停目标，可设置预计恢复日期。
   *
   * <p>暂停后目标不再计入活跃上限，也不再产生新的提醒；已生成的行动实例保留，
   * 由用户在恢复时重新评估。预计恢复日期是否早于「今天」由应用层按统一时钟校验，
   * 领域层只保证它与目标自身开始日期一致。
   */
  public void pause(LocalDate expectedResumeDate, long expectedVersion, LocalDateTime now) {
    assertVersion(expectedVersion);
    if (status != GoalStatus.ACTIVE && status != GoalStatus.PENDING_CONFIRMATION) {
      throw new BusinessException("GOAL_PAUSE_NOT_ALLOWED", "只有进行中的目标可以暂停");
    }
    LocalDate start = definition.startDate();
    if (expectedResumeDate != null && start != null && expectedResumeDate.isBefore(start)) {
      throw new BusinessException("GOAL_INVALID_RESUME_DATE", "预计恢复日期不能早于目标开始日期");
    }
    status = GoalStatus.PAUSED;
    pauseResumeAt = expectedResumeDate;
    version++;
    updatedAt = now;
  }

  /** 恢复目标；恢复后需要重新评估截止期与剩余任务，由应用层读取返回值提示用户。 */
  public void resume(long expectedVersion, LocalDateTime now) {
    assertVersion(expectedVersion);
    if (status != GoalStatus.PAUSED) {
      throw new BusinessException("GOAL_RESUME_NOT_ALLOWED", "只有已暂停的目标可以恢复");
    }
    status = GoalStatus.ACTIVE;
    pauseResumeAt = null;
    version++;
    updatedAt = now;
  }

  /** 放弃目标并记录原因；放弃后不计入活跃上限，且不再允许修改定义。 */
  public void abandon(String reason, long expectedVersion, LocalDateTime now) {
    assertVersion(expectedVersion);
    if (status == GoalStatus.COMPLETED
        || status == GoalStatus.ARCHIVED
        || status == GoalStatus.ABANDONED) {
      throw new BusinessException("GOAL_ABANDON_NOT_ALLOWED", "当前目标状态不能放弃");
    }
    if (reason == null || reason.isBlank()) {
      throw new BusinessException("GOAL_ABANDON_REASON_REQUIRED", "放弃目标必须记录原因");
    }
    status = GoalStatus.ABANDONED;
    abandonReason = reason.trim();
    pauseResumeAt = null;
    version++;
    updatedAt = now;
  }

  /** 归档目标，仅作为列表收纳，不改变已产生的历史事实。 */
  public void archive(long expectedVersion, LocalDateTime now) {
    assertVersion(expectedVersion);
    if (status == GoalStatus.ARCHIVED) {
      throw new BusinessException("GOAL_ARCHIVE_NOT_ALLOWED", "目标已归档");
    }
    if (status == GoalStatus.ACTIVE || status == GoalStatus.PENDING_CONFIRMATION) {
      throw new BusinessException("GOAL_ARCHIVE_NOT_ALLOWED", "进行中的目标需要先完成或放弃再归档");
    }
    status = GoalStatus.ARCHIVED;
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

  /** 目标是否计入活跃上限；口径集中在 {@link com.lingxi.goal.api.ActiveGoalQuota}。 */
  public boolean countsAsActive() {
    return com.lingxi.goal.api.ActiveGoalQuota.countsAsActive(status);
  }

  private void assertVersion(long expectedVersion) {
    if (version != expectedVersion) {
      throw new BusinessException("GOAL_VERSION_CONFLICT", "目标已被其他终端更新");
    }
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

  public GoalDefinition getDefinition() {
    return definition;
  }

  public String getTitle() {
    return definition.title();
  }

  public String getSuccessCriteria() {
    return definition.successCriteria();
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

  public LocalDate getPauseResumeAt() {
    return pauseResumeAt;
  }

  public String getAbandonReason() {
    return abandonReason;
  }

  /** 首目标引导澄清阶段；null 表示不处于引导中。 */
  public GoalClarificationStage getClarificationStage() {
    return clarificationStage;
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
