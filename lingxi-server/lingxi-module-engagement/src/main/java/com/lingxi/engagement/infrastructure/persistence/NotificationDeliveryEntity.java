package com.lingxi.engagement.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("eng_notification_delivery")
public class NotificationDeliveryEntity extends EngagementLogicalDeletionEntity {
  @TableId private Long id;
  private Long taskId;
  private Integer attemptNo;
  private String providerMessageId, result;
  private LocalDateTime deliveredAt;
  private String errorCode;
  private LocalDateTime createdAt;
}
