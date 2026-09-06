package com.lingxi.engagement.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

/** R05 持久化对象。 */
@Getter
@Setter
@TableName("eng_notification_rule")
public class NotificationPreferenceEntity extends EngagementLogicalDeletionEntity {
  @TableId private String id;
  private Long userId;
  private String scene;
  private String channelsJson;
  private LocalTime quietStart;
  private LocalTime quietEnd;
  private String timezone;
  @Version private Long version;
  private LocalDateTime updatedAt;
}
