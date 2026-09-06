package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("pay_transaction")
public class PaymentTransactionEntity extends CommerceLogicalDeletionEntity {
  @TableId private Long id;
  private Long orderId;
  private String channel, transactionId;
  private Long amountMinor;
  private String currency, rawDigest;
  private Instant verifiedAt;
}
