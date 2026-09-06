package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("pay_subscription")
public class SubscriptionEntity extends CommerceLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId, productId;
  private String channel, channelSubscriptionId, status;
  private Instant periodEnd;
  private String cancelMode;
  @Version private Long version;
  private LocalDateTime createdAt, updatedAt;
  private LocalDateTime lastReconciledAt;
}
