package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("pay_entitlement_ledger")
public class EntitlementLedgerEntity extends CommerceLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private String resourceKey, sourceType, sourceId, commandId;
  private Long delta, balanceAfter;
  private LocalDateTime createdAt;
}
