package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 专注会话持久化对象。 */
@Getter
@Setter
@TableName("goal_focus_session")
public class FocusSessionEntity extends GoalLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private Long occurrenceId;
  private Long actionId;
  private String status;
  private Integer plannedMinutes;
  private Integer accumulatedSeconds;
  private LocalDateTime lastResumedAt;
  private LocalDateTime startedAt;
  private LocalDateTime endedAt;
  private String note;
  /** 进行中标记；与 user_id 组成唯一键，保证同一用户只有一个进行中的会话。 */
  private Integer activeKey;
  @Version private Long version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
