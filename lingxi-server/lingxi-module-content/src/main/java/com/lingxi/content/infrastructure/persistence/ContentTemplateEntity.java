package com.lingxi.content.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("content_goal_template")
public class ContentTemplateEntity extends ContentLogicalDeletionEntity {
  @TableId private Long id;
  private String templateKey, name, status;
  private Integer currentVersion;
  private LocalDateTime createdAt, updatedAt;
}
