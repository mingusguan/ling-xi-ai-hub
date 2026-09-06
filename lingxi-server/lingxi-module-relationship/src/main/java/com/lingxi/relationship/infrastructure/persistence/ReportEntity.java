package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("rel_report")
public class ReportEntity extends RelationshipLogicalDeletionEntity {
  @TableId private Long id;
  private Long reporterUserId;
  private String targetType, targetId, reasonCode, evidenceRef, status;
  private LocalDateTime createdAt;
}
