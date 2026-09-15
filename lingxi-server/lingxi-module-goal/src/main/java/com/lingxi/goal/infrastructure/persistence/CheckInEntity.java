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
  /** 实际耗时，单位分钟。 */
  private Integer actualMinutes;
  /** 主观难度；与计划中的行动难度使用同一把尺子。 */
  private String perceivedDifficulty;
  /** 精力自评。 */
  private String energyLevel;
  /** 情绪自评；仅用于趋势观察，不产出诊断结论。 */
  private String moodLevel;
  /** 失败原因；仅结果为「失败」时有值。 */
  private String failureReason;
  private Boolean effective;
  private Integer effectiveKey;
  private LocalDateTime recordedAt;
  private LocalDateTime createdAt;
}
