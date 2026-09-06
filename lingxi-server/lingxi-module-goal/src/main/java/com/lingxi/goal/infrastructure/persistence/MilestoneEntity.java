package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 里程碑持久化对象。 */
@Getter
@Setter
@TableName("goal_milestone")
public class MilestoneEntity extends GoalLogicalDeletionEntity {
  @TableId private Long id;
  private Long planVersionId;
  private Integer sequenceNo;
  private String title;
  private String successCriteria;
  private LocalDateTime createdAt;
}
