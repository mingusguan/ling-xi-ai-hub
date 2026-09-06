package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("pay_refund")
public class RefundEntity extends CommerceLogicalDeletionEntity {
  @TableId private Long id;
  private Long orderId;
  private String channel, refundTransactionId;
  private Long amountMinor;
  private String reason, status;
  private LocalDateTime createdAt;
}
