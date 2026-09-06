package com.lingxi.commerce.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.commerce.domain.*;
import com.lingxi.kernel.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** 订单关闭和渠道订阅对账任务处理器。 */
@Component
public class CommerceJobHandler implements AsyncJobHandler {
  public static final String ORDER_CLOSE = "commerce.order.close";
  public static final String SUBSCRIPTION_RECONCILE = "commerce.subscription.reconcile";

  private final CommerceRepository repository;
  private final CommerceTransactionService transactions;
  private final Map<String, PaymentChannelAdapter> channels = new HashMap<>();
  private final ObjectMapper json;

  public CommerceJobHandler(
      CommerceRepository repository,
      CommerceTransactionService transactions,
      List<PaymentChannelAdapter> channelAdapters,
      ObjectMapper json) {
    this.repository = repository;
    this.transactions = transactions;
    channelAdapters.forEach(adapter -> channels.put(adapter.channel(), adapter));
    this.json = json;
  }

  public boolean supports(String jobType) {
    return ORDER_CLOSE.equals(jobType) || SUBSCRIPTION_RECONCILE.equals(jobType);
  }

  public String handle(AsyncJobMessage message) throws Exception {
    if (ORDER_CLOSE.equals(message.jobType())) {
      long orderId = json.readTree(message.payloadJson()).path("orderId").asLong();
      Order order = transactions.closeExpiredOrder(orderId);
      return "{\"status\":\"" + order.getStatus().name() + "\"}";
    }
    long subscriptionId = json.readTree(message.payloadJson()).path("subscriptionId").asLong();
    Subscription subscription =
        repository
            .findSubscription(subscriptionId)
            .orElseThrow(() -> new BusinessException("PAY_SUBSCRIPTION_NOT_FOUND", "订阅不存在"));
    PaymentChannelAdapter channel = channels.get(subscription.getChannel());
    if (channel == null) {
      throw new BusinessException("PAY_CHANNEL_UNAVAILABLE", "订阅渠道尚未配置");
    }
    PaymentChannelAdapter.SubscriptionSnapshot snapshot = channel.querySubscription(subscription);
    Subscription reconciled = transactions.reconcileSubscription(subscriptionId, snapshot);
    repository.markSubscriptionReconciled(
        subscriptionId, java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
    return "{\"status\":\"" + reconciled.getStatus().name() + "\"}";
  }
}
