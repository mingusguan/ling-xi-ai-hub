package com.lingxi.commerce.application;

import com.lingxi.commerce.api.*;
import com.lingxi.commerce.domain.*;
import com.lingxi.identity.api.AdminActionAuditedEvent;
import com.lingxi.kernel.*;
import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 将本地状态推进收敛为短事务，避免支付渠道网络调用占用数据库事务。 */
@Service
public class CommerceTransactionService {
  private final CommerceRepository repository;
  private final IdGenerator ids;
  private final DomainEventPublisher events;

  public CommerceTransactionService(
      CommerceRepository repository, IdGenerator ids, DomainEventPublisher events) {
    this.repository = repository;
    this.ids = ids;
    this.events = events;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Order createOrder(Order order, SellablePrice priceSnapshot) {
    repository.insertOrder(order, priceSnapshot);
    return order;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Order markPaying(long orderId, String paymentReference) {
    Order order =
        repository.findOrder(orderId).orElseThrow(() -> error("PAY_ORDER_NOT_FOUND", "订单不存在"));
    if (order.getStatus() != Order.Status.CREATED) {
      return order;
    }
    long previousVersion = order.getVersion();
    order.paying(paymentReference, now());
    updated(repository.updateOrder(order, previousVersion));
    return order;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Subscription completeCancellation(CancelSubscriptionCommand command) {
    Subscription subscription =
        repository
            .findSubscription(command.subscriptionId())
            .orElseThrow(() -> error("PAY_SUBSCRIPTION_NOT_FOUND", "订阅不存在"));
    subscription.assertCancellable(
        command.userId(), command.cancelMode(), command.expectedVersion());
    long previousVersion = subscription.getVersion();
    subscription.cancel(command.userId(), command.cancelMode(), command.expectedVersion(), now());
    updated(repository.updateSubscription(subscription, previousVersion));
    return subscription;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Order applyVerifiedPayment(String channel, PaymentChannelAdapter.VerifiedPayment paid) {
    Order order =
        repository
            .findOrderByNo(paid.orderNo())
            .orElseThrow(() -> error("PAY_ORDER_NOT_FOUND", "商户订单不存在"));
    if (!order.getChannel().equals(channel)) {
      throw error("PAY_CHANNEL_MISMATCH", "回调渠道与订单不匹配");
    }
    var callback = repository.findCallback(channel, paid.callbackId());
    if (callback.isPresent()) {
      if (!Digests.constantTimeEquals(callback.get().rawDigest(), paid.rawDigest())) {
        throw error("PAY_CALLBACK_CONFLICT", "重复回调摘要不一致");
      }
      if ("PROCESSED".equals(callback.get().status())) return order;
    }
    var transaction = repository.findTransaction(channel, paid.transactionId());
    if (transaction.isPresent()) {
      var existing = transaction.get();
      if (existing.orderId() != order.getId()
          || existing.amountMinor() != paid.amountMinor()
          || !existing.currency().equals(paid.currency()))
        throw error("PAY_TRANSACTION_CONFLICT", "重复渠道交易与原交易不一致");
      if (callback.isEmpty())
        repository.insertCallback(
            ids.nextId(), channel, paid.callbackId(), paid.rawDigest(), "PROCESSED", now());
      else repository.updateCallback(channel, paid.callbackId(), "PROCESSED");
      return order;
    }
    LocalDateTime now = now();
    if (callback.isEmpty())
      repository.insertCallback(
          ids.nextId(), channel, paid.callbackId(), paid.rawDigest(), "VERIFIED", now);
    long previousVersion = order.getVersion();
    if (order.pay(paid.transactionId(), paid.amountMinor(), paid.currency(), now)) {
      updated(repository.updateOrder(order, previousVersion));
      repository.insertTransaction(
          ids.nextId(),
          order.getId(),
          channel,
          paid.transactionId(),
          paid.amountMinor(),
          paid.currency(),
          paid.rawDigest(),
          paid.paidAt());
      SellablePrice price =
          repository
              .findOrderPriceSnapshot(order.getId())
              .orElseThrow(() -> error("PAY_PRICE_SNAPSHOT_NOT_FOUND", "订单价格快照不存在"));
      repository.grantEntitlement(
          ids.nextId(),
          order.getUserId(),
          price.entitlementKey(),
          price.entitlementAmount(),
          paid.periodEnd(),
          "ORDER",
          String.valueOf(order.getId()),
          "ORDER:" + order.getId(),
          now);
      if (!"ONE_TIME".equals(price.billingPeriod())
          && paid.subscriptionId() != null
          && repository.findActiveSubscription(order.getUserId(), order.getProductId()).isEmpty()) {
        repository.insertSubscription(
            Subscription.activate(
                ids.nextId(),
                order.getUserId(),
                order.getProductId(),
                channel,
                paid.subscriptionId(),
                paid.periodEnd(),
                now));
      }
    }
    repository.updateCallback(channel, paid.callbackId(), "PROCESSED");
    return order;
  }

  @Transactional
  public Order applyRefund(ConfirmRefundCommand command, long entitlementToReclaim) {
    return applyRefundInternal(command, entitlementToReclaim);
  }

  @Transactional
  public Order applyAdminRefund(
      long adminId,
      ConfirmRefundCommand command,
      long entitlementToReclaim,
      AdminCommerceManagementFacade.OperationContext context) {
    Order order = applyRefundInternal(command, entitlementToReclaim);
    events.publish(
        new AdminActionAuditedEvent(
            UUID.randomUUID().toString(),
            adminId,
            "REFUND_CONFIRM",
            "ORDER",
            String.valueOf(order.getId()),
            order.getVersion(),
            context.reason(),
            context.ticketNo(),
            Instant.now()));
    return order;
  }

  private Order applyRefundInternal(ConfirmRefundCommand command, long entitlementToReclaim) {
    Order order =
        repository
            .findOrderByNo(command.orderNo())
            .orElseThrow(() -> error("PAY_ORDER_NOT_FOUND", "订单不存在"));
    if (!order.getChannel().equals(command.channel()))
      throw error("PAY_CHANNEL_MISMATCH", "退款渠道与订单不匹配");
    var existing = repository.findRefund(command.channel(), command.refundTransactionId());
    if (existing.isPresent()) {
      if (existing.get().orderId() != order.getId()
          || existing.get().amountMinor() != command.amountMinor())
        throw error("PAY_REFUND_CONFLICT", "重复退款交易与原退款不一致");
      return order;
    }
    SellablePrice price =
        repository
            .findOrderPriceSnapshot(order.getId())
            .orElseThrow(() -> error("PAY_PRICE_SNAPSHOT_NOT_FOUND", "订单价格快照不存在"));
    LocalDateTime now = now();
    if (entitlementToReclaim > 0) {
      repository.consumeEntitlement(
          ids.nextId(),
          order.getUserId(),
          price.entitlementKey(),
          entitlementToReclaim,
          "REFUND",
          command.refundTransactionId(),
          "REFUND:" + command.channel() + ":" + command.refundTransactionId(),
          now);
    }
    long previousVersion = order.getVersion();
    order.refund(command.amountMinor(), now);
    updated(repository.updateOrder(order, previousVersion));
    repository.insertRefund(
        ids.nextId(),
        order.getId(),
        command.channel(),
        command.refundTransactionId(),
        command.amountMinor(),
        command.reason(),
        now);
    return order;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Order closeExpiredOrder(long orderId) {
    Order order =
        repository.findOrder(orderId).orElseThrow(() -> error("PAY_ORDER_NOT_FOUND", "订单不存在"));
    if (order.getStatus() == Order.Status.CLOSED) return order;
    long previousVersion = order.getVersion();
    if (order.close(now())) updated(repository.updateOrder(order, previousVersion));
    return order;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Subscription reconcileSubscription(
      long subscriptionId, PaymentChannelAdapter.SubscriptionSnapshot snapshot) {
    Subscription subscription =
        repository
            .findSubscription(subscriptionId)
            .orElseThrow(() -> error("PAY_SUBSCRIPTION_NOT_FOUND", "订阅不存在"));
    long previousVersion = subscription.getVersion();
    if (subscription.reconcile(snapshot.status(), snapshot.periodEnd(), now())) {
      updated(repository.updateSubscription(subscription, previousVersion));
    }
    return subscription;
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private void updated(boolean updated) {
    if (!updated) throw error("PAY_CONCURRENT_UPDATE", "数据已被并发修改");
  }

  private BusinessException error(String code, String message) {
    return new BusinessException(code, message);
  }

  private static final class Digests {
    private static boolean constantTimeEquals(String left, String right) {
      if (left == null || right == null) return false;
      return java.security.MessageDigest.isEqual(
          left.getBytes(java.nio.charset.StandardCharsets.UTF_8),
          right.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
  }
}
