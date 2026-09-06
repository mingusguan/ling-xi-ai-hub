package com.lingxi.companion.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@TableName("ai_safety_event")
public class SafetyEventEntity extends CompanionLogicalDeletionEntity {
  @TableId private Long id;
  private Long runId, userId;
  private String riskType, riskLevel, disclosureScope, status;
  private LocalDateTime createdAt, handledAt;
}
