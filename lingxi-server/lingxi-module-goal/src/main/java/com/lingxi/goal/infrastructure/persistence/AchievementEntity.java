package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 成就持久化对象。 */
@TableName("goal_achievement")
public class AchievementEntity extends GoalLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private Long goalId;
  private String achievementType;
  private String achievementTitle;
  private String achievementDescription;
  private String referenceKey;
  private LocalDateTime achievedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long v) {
    id = v;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long v) {
    userId = v;
  }

  public Long getGoalId() {
    return goalId;
  }

  public void setGoalId(Long v) {
    goalId = v;
  }

  public String getAchievementType() {
    return achievementType;
  }

  public void setAchievementType(String v) {
    achievementType = v;
  }

  public String getAchievementTitle() {
    return achievementTitle;
  }

  public void setAchievementTitle(String v) {
    achievementTitle = v;
  }

  public String getAchievementDescription() {
    return achievementDescription;
  }

  public void setAchievementDescription(String v) {
    achievementDescription = v;
  }

  public String getReferenceKey() {
    return referenceKey;
  }

  public void setReferenceKey(String v) {
    referenceKey = v;
  }

  public LocalDateTime getAchievedAt() {
    return achievedAt;
  }

  public void setAchievedAt(LocalDateTime v) {
    achievedAt = v;
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
