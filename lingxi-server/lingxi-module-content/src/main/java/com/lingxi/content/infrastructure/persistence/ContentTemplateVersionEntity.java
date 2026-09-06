package com.lingxi.content.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("content_template_version")
public class ContentTemplateVersionEntity extends ContentLogicalDeletionEntity {
  @TableId private Long id;
  private Long templateId;
  private Integer versionNo;
  private String ageScope, contentSnapshot, status;
  private Long reviewerUserId;
  private String reviewReason;
  private LocalDateTime publishedAt;
  @Version private Long lockVersion;
  private LocalDateTime createdAt, updatedAt;
}
