package com.lingxi.operations.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("ops_support_ticket")
public class SupportTicketEntity extends OperationsLogicalDeletionEntity {
  @TableId private Long id;
  private String ticketNo;
  private Long userId;
  private Long privacyRequestId;
  private String category, subject, description, status, priority;
  private Long assigneeAdminId;
  @Version private Long version;
  private LocalDateTime slaDueAt, followUpAt, createdAt, updatedAt;
}
