package com.lingxi.content.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("content_export_job")
public class ContentExportJobEntity extends ContentLogicalDeletionEntity {
  @TableId private Long id;
  private String requestKey;
  private Long userId;
  private String scopeJson, format, status;
  private Long resultFileId;
  private Instant expiresAt;
  private String errorMessage;
  @Version private Long version;
  private LocalDateTime createdAt, updatedAt;
}
