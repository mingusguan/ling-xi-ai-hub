package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("pay_order")
public class OrderEntity extends CommerceLogicalDeletionEntity {
  @TableId private Long id;
  private String orderNo, businessOrderKey;
  private Long userId, productId, priceId;
  private Integer priceVersion;
  private Long amountMinor;
  private String currency, channel, status, paymentReference;
  private Long refundedMinor;
  @Version private Long version;
  private LocalDateTime createdAt, updatedAt;
}
