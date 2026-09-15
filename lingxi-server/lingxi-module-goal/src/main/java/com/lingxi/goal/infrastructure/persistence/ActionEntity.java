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
  private String description;
  private String priority;
  private String difficulty;
  private String completionCriteria;
  private Integer estimatedMinutes;
  private String recurrenceType;
  private String weekdaysJson;
  /** 间隔重复的间隔天数；仅 recurrenceType=INTERVAL 时有值。 */
  private Integer intervalDays;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalTime localTime;
  /** 时间段结束时刻；为空表示只有开始时刻、没有明确结束时刻。 */
  private LocalTime endLocalTime;
  /** 前置行动标识；为空表示没有前置依赖。 */
  private Long prerequisiteActionId;
  /** 行动级提醒策略；为空表示沿用账号级提醒偏好。 */
  private String reminderPolicy;
  private String timezone;
  private String status;
  /** 是否为新手引导生成的首行动（PRD ONB-02）。 */
  private Boolean isFirst;
  @Version private Long version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
