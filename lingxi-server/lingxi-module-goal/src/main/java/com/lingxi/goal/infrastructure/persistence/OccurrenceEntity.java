package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 行动发生实例持久化对象。 */
@Getter
@Setter
@TableName("goal_action_occurrence")
public class OccurrenceEntity extends GoalLogicalDeletionEntity {
  @TableId private Long id;
  private Long actionId;
  private LocalDateTime scheduledAt;
  private LocalDate localDate;
  private String timezone;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
