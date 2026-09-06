package com.lingxi.companion.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@TableName("ai_conversation")
public class ConversationEntity extends CompanionLogicalDeletionEntity {
  @TableId private Long id;
  private String publicId, requestKey;
  private Long userId;
  private String scene, title, status;
  private LocalDateTime createdAt, updatedAt;
}
