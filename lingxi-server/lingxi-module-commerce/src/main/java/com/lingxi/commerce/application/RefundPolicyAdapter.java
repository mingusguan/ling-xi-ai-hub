package com.lingxi.commerce.application;

import com.lingxi.commerce.api.ConfirmRefundCommand;
import com.lingxi.commerce.domain.*;

/** 渠道退款规则端口；负责核验退款结果并计算应回收权益，未配置时退款失败关闭。 */
public interface RefundPolicyAdapter {
  String channel();

  RefundDecision evaluate(Order order, SellablePrice price, ConfirmRefundCommand command);

  record RefundDecision(long entitlementToReclaim) {
    public RefundDecision {
      if (entitlementToReclaim < 0) {
        throw new IllegalArgumentException("entitlementToReclaim must not be negative");
      }
    }
  }
}
