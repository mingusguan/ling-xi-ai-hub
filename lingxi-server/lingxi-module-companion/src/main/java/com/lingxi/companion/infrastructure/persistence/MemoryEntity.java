package com.lingxi.companion.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@TableName("ai_memory")
public class MemoryEntity extends CompanionLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private String purpose, contentText, sourceRef, sensitivity, status;
  @Version private Long version;
  private LocalDateTime createdAt, updatedAt;
}
