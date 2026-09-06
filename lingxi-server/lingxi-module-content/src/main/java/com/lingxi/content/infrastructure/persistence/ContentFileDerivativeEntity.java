package com.lingxi.content.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@TableName("content_file_derivative")
public class ContentFileDerivativeEntity extends ContentLogicalDeletionEntity {
  @TableId private Long id;
  private Long fileId;
  private String derivativeType;
  private String objectKey;
  private String status;
  private LocalDateTime createdAt;
}
