package com.lingxi.companion.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("ai_action_proposal")
public class ActionProposalEntity extends CompanionLogicalDeletionEntity {
  @TableId private Long id;
  private String publicId;
  private Long runId, userId;
  private String toolName, riskLevel, argumentsJson, requestDigest;
  private Long authorizationVersion;
  private Instant expiresAt;
  private String status, decision, resultJson;
  @Version private Long version;
  private LocalDateTime createdAt, updatedAt;
}
