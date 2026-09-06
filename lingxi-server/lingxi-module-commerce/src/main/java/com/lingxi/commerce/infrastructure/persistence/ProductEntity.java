package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Getter
@Setter
@TableName("pay_product")
public class ProductEntity extends CommerceLogicalDeletionEntity {
  @TableId private Long id;
  private String productKey, name, scene, agePolicy, entitlementKey, billingPeriod, status;
  private Long entitlementAmount;
}
