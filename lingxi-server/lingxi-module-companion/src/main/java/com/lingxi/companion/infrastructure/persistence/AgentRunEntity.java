package com.lingxi.companion.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@TableName("ai_agent_run")
public class AgentRunEntity extends CompanionLogicalDeletionEntity {
  @TableId private Long id;
  private String publicId, requestKey, requestDigest;
  private Long conversationId, userId;
  private String scene, status;
  private Long authorizationVersion;
  private String modelVersion, promptVersion, resultText, errorCode;
  @Version private Long version;
  private LocalDateTime createdAt, updatedAt;
}
