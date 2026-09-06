package com.lingxi.companion.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@TableName("ai_run_event")
public class RunEventEntity extends CompanionLogicalDeletionEntity {
  @TableId private Long id;
  private Long runId;
  private String eventType, safePayloadJson;
  private LocalDateTime createdAt;
}
