package com.lingxi.engagement.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

/** R05 持久化对象。 */
@Getter
@Setter
@TableName("eng_calendar_binding")
public class CalendarBindingEntity extends EngagementLogicalDeletionEntity {
  @TableId private Long id;
  private String requestKey;
  private Long userId;
  private String provider;
  private String credentialReference;
  private String status;
  private Boolean deleteCreatedEvents;
  @Version private Long version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
