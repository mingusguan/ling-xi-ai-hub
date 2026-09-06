package com.lingxi.content.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("content_file_reference")
public class ContentFileReferenceEntity extends ContentLogicalDeletionEntity {
  @TableId private Long id;
  private Long fileId, ownerUserId;
  private String resourceType, resourceId;
  private LocalDateTime createdAt;
}
