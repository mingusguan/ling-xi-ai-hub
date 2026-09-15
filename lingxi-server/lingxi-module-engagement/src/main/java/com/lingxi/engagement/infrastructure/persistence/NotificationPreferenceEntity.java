package com.lingxi.engagement.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

/** 用户通知偏好的持久化对象：渠道、免打扰时段与时区。 */
@Getter
@Setter
@TableName("eng_notification_preference")
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
