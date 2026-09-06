package com.lingxi.companion.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@TableName("ai_message")
public class MessageEntity extends CompanionLogicalDeletionEntity {
  @TableId private Long id;
  private Long conversationId, runId;
  private String role, contentText, contentDigest;
  private Boolean aiGenerated;
  private LocalDateTime createdAt;
}
