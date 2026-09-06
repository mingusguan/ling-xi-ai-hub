package com.lingxi.operations.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("ops_audit_log")
public class AuditLogEntity extends OperationsLogicalDeletionEntity {
  @TableId private Long id;
  private Long adminId;
  private String action,
      objectType,
      objectId,
      reason,
      ticketNo,
      beforeDigest,
      afterDigest,
      result,
      requestId;
  private LocalDateTime createdAt;
}
