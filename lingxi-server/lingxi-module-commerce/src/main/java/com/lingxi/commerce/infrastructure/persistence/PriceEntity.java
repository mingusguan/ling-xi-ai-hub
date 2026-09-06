package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Getter
@Setter
@TableName("pay_price")
public class PriceEntity extends CommerceLogicalDeletionEntity {
  @TableId private Long id;
  private Long productId;
  private Integer versionNo;
  private Long amountMinor;
  private String currency, status;
}
