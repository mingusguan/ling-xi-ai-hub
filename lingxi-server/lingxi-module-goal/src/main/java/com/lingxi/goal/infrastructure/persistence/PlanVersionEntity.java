package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 计划版本持久化对象。 */
@Getter
@Setter
@TableName("goal_plan_version")
public class PlanVersionEntity extends GoalLogicalDeletionEntity {
  @TableId private Long id;
  private Long goalId;
  private Integer versionNo;
  private String requestKey;
  private String requestDigest;
  private String snapshotJson;
  private String adjustmentReason;
  private String status;
  private String source;
  private LocalDateTime activatedAt;
  private LocalDateTime createdAt;
}
