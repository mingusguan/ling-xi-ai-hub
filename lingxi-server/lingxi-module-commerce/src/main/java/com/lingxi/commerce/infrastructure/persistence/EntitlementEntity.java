package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("pay_entitlement")
public class EntitlementEntity extends CommerceLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private String resourceKey;
  private Long balance;
  private Instant expiresAt;
  @Version private Long version;
  private LocalDateTime createdAt, updatedAt;
}
