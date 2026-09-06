package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 打卡事实持久化对象。 */
@Getter
@Setter
@TableName("goal_check_in")
public class CheckInEntity extends GoalLogicalDeletionEntity {
  @TableId private Long id;
  private Long occurrenceId;
  private Long userId;
  private String requestKey;
  private String requestDigest;
  private String result;
  private String note;
  private String evidenceReference;
  private Boolean effective;
  private Integer effectiveKey;
  private LocalDateTime recordedAt;
  private LocalDateTime createdAt;
}
