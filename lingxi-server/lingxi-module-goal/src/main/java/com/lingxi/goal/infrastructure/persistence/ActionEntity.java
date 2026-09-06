package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/** 行动定义持久化对象。 */
@Getter
@Setter
@TableName("goal_action")
public class ActionEntity extends GoalLogicalDeletionEntity {
  @TableId private Long id;
  private Long goalId;
  private Long planVersionId;
  private Long milestoneId;
  private String clientKey;
  private String title;
  private String recurrenceType;
  private String weekdaysJson;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalTime localTime;
  private String timezone;
  private String status;
  @Version private Long version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
