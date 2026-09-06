package com.lingxi.engagement.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

/** R05 持久化对象。 */
@Getter
@Setter
@TableName("eng_notification_task")
public class NotificationTaskEntity extends EngagementLogicalDeletionEntity {
  @TableId private Long id;
  private String dedupeKey;
  private Long recipientUserId;
  private String channel;
  private String scene;
  private String resourceType;
  private String resourceId;
  private String payloadJson;
  private LocalDateTime scheduledAt;
  private String status;
  private Integer attemptCount;
  private String lastError;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
