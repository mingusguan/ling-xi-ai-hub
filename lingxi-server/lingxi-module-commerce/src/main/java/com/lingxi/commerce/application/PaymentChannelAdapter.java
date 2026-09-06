package com.lingxi.commerce.application;

import com.lingxi.commerce.domain.*;
import java.time.Instant;
import java.util.Map;

/** 支付渠道端口。发起支付和取消订阅必须分别按订单号、requestKey保证渠道侧幂等。 */
public interface PaymentChannelAdapter {
  String channel();

  PaymentInitiation initiate(Order order);

  VerifiedPayment verify(String rawPayload, Map<String, String> headers);

  void cancel(Subscription subscription, String mode, String requestKey);

  /** 查询渠道订阅事实。实现必须是只读且可安全重试。 */
  SubscriptionSnapshot querySubscription(Subscription subscription);

  record PaymentInitiation(String opaqueReference) {}

  record SubscriptionSnapshot(Subscription.Status status, Instant periodEnd, String digest) {}

  record VerifiedPayment(
      String callbackId,
      String transactionId,
      String orderNo,
      long amountMinor,
      String currency,
      Instant paidAt,
      String rawDigest,
      String subscriptionId,
      Instant periodEnd) {}
}
