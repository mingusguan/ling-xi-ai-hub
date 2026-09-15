package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 快速记录持久化对象。 */
@Getter
@Setter
@TableName("goal_quick_note")
public class QuickNoteEntity extends GoalLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private String content;
  private String moodLevel;
  private LocalDate localDate;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
