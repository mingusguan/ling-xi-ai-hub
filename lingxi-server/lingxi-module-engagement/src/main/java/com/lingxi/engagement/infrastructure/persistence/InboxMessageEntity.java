package com.lingxi.engagement.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

/** R05 持久化对象。 */
@Getter
@Setter
@TableName("eng_inbox_message")
public class InboxMessageEntity extends EngagementLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private String type;
  private String resourceType;
  private String resourceId;
  private String summary;
  @TableField("cursor_no")
  private Long cursor;
  private LocalDateTime readAt;
  private LocalDateTime createdAt;
}
