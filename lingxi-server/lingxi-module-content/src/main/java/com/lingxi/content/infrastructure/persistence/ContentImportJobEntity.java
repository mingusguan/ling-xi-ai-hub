package com.lingxi.content.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("content_import_job")
public class ContentImportJobEntity extends ContentLogicalDeletionEntity {
  @TableId private Long id;
  private String requestKey;
  private Long userId, sourceFileId;
  private String format, status, previewJson, errorJson;
  @Version private Long version;
  private LocalDateTime createdAt, updatedAt;
}
