package com.lingxi.engagement.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("eng_calendar_event_binding")
public class CalendarEventBindingEntity extends EngagementLogicalDeletionEntity {
  @TableId private Long id;
  private Long bindingId;
  private String resourceType, resourceId, externalId, externalVersion, syncStatus;
  private LocalDateTime updatedAt;
}
