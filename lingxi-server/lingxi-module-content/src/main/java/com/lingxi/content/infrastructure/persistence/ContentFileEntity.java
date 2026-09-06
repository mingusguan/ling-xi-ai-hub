package com.lingxi.content.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("content_file")
public class ContentFileEntity extends ContentLogicalDeletionEntity {
  @TableId private Long id;
  private String publicId, requestKey;
  private Long ownerUserId;
  private String purpose, originalName, objectKey, contentHash;
  private Long sizeBytes;
  private String mimeType, sensitivity, status, scanResult;
  @Version private Long version;
  private LocalDateTime createdAt, updatedAt;
}
