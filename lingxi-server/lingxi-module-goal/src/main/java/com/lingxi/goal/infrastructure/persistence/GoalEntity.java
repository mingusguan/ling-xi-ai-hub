package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
  private String successCriteria;
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

  public String getSuccessCriteria() {
    return successCriteria;
  }

  public void setSuccessCriteria(String v) {
    successCriteria = v;
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
