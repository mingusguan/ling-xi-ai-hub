package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Getter
@Setter
@TableName("pay_order_item")
public class OrderItemEntity extends CommerceLogicalDeletionEntity {
  @TableId private Long id;
  private Long orderId;
  private Long productId;
  private Long priceId;
  private Integer quantity;
  private Long amountMinor;
  private String snapshotJson;
}
