package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 目标持久化对象。 */
@TableName("goal_goal")
public class GoalEntity extends GoalLogicalDeletionEntity {
  @TableId private Long id;
  private String publicId;
  private Long userId;
  private String requestKey;
  private String requestDigest;
  private String title;
  private String description;
  private String goalType;
  private String successCriteria;
  private LocalDate startDate;
  private LocalDate targetEndDate;
  private String priority;
  private Integer weeklyAvailableMinutes;
  private String resourceConstraints;
  private String verifiableOutcomes;
  private String privacyLevel;
  private LocalDate pauseResumeAt;
  private String abandonReason;
  /** 首目标引导澄清阶段（PRD ONB-02）；null 表示不处于引导中。 */
  private String clarificationStage;
  private String status;
  private Long currentPlanVersionId;
  private Integer progress;
  @Version private Long version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long v) {
    id = v;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String v) {
    publicId = v;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long v) {
    userId = v;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public void setRequestKey(String v) {
    requestKey = v;
  }

  public String getRequestDigest() {
    return requestDigest;
  }

  public void setRequestDigest(String v) {
    requestDigest = v;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String v) {
    title = v;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String v) {
    description = v;
  }

  public String getGoalType() {
    return goalType;
  }

  public void setGoalType(String v) {
    goalType = v;
  }

  public String getSuccessCriteria() {
    return successCriteria;
  }

  public void setSuccessCriteria(String v) {
    successCriteria = v;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate v) {
    startDate = v;
  }

  public LocalDate getTargetEndDate() {
    return targetEndDate;
  }

  public void setTargetEndDate(LocalDate v) {
    targetEndDate = v;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String v) {
    priority = v;
  }

  public Integer getWeeklyAvailableMinutes() {
    return weeklyAvailableMinutes;
  }

  public void setWeeklyAvailableMinutes(Integer v) {
    weeklyAvailableMinutes = v;
  }

  public String getResourceConstraints() {
    return resourceConstraints;
  }

  public void setResourceConstraints(String v) {
    resourceConstraints = v;
  }

  public String getVerifiableOutcomes() {
    return verifiableOutcomes;
  }

  public void setVerifiableOutcomes(String v) {
    verifiableOutcomes = v;
  }

  public String getPrivacyLevel() {
    return privacyLevel;
  }

  public void setPrivacyLevel(String v) {
    privacyLevel = v;
  }

  public LocalDate getPauseResumeAt() {
    return pauseResumeAt;
  }

  public void setPauseResumeAt(LocalDate v) {
    pauseResumeAt = v;
  }

  public String getAbandonReason() {
    return abandonReason;
  }

  public void setAbandonReason(String v) {
    abandonReason = v;
  }

  public String getClarificationStage() {
    return clarificationStage;
  }

  public void setClarificationStage(String v) {
    clarificationStage = v;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String v) {
    status = v;
  }

  public Long getCurrentPlanVersionId() {
    return currentPlanVersionId;
  }

  public void setCurrentPlanVersionId(Long v) {
    currentPlanVersionId = v;
  }

  public Integer getProgress() {
    return progress;
  }

  public void setProgress(Integer v) {
    progress = v;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long v) {
    version = v;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime v) {
    createdAt = v;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime v) {
    updatedAt = v;
  }
}
