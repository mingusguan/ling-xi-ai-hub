package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 周期复盘持久化对象。 */
@Getter
@Setter
@TableName("goal_review")
public class ReviewEntity extends GoalLogicalDeletionEntity {
  @TableId private Long id;
  private Long goalId;
  private String periodKey;
  private String inputSnapshotJson;
  private String status;
  private String conclusionJson;
  private String completionRequestKey;
  private String completionRequestDigest;
  @Version private Long version;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
}
