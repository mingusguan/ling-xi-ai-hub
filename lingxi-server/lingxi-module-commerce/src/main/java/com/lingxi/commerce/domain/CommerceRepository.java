package com.lingxi.commerce.domain;

import java.time.*;
import java.util.*;

public interface CommerceRepository {
  List<SellablePrice> listPrices();

  Optional<SellablePrice> findPrice(long productId, int version);

  Optional<SellablePrice> findPriceByProduct(long productId);

  Optional<Order> findOrder(long id);

  Optional<Order> findOrderByNo(String no);

  Optional<Order> findOrderByBusinessKey(String key);

  List<Order> findClosableOrders(LocalDateTime updatedBefore, int limit);

  /** 统计指定用户的订单总数，用于客户端“我的订单”分页。 */
  long countOrdersByUser(long userId);

  /** 按创建时间倒序分页查询指定用户的订单。 */
  List<Order> findOrdersByUser(long userId, int page, int pageSize);

  void insertOrder(Order order, SellablePrice priceSnapshot);

  Optional<SellablePrice> findOrderPriceSnapshot(long orderId);

  boolean updateOrder(Order order, long previous);

  Optional<CallbackSnapshot> findCallback(String channel, String callbackId);

  void insertCallback(
      long id, String channel, String callbackId, String digest, String status, LocalDateTime now);

  void updateCallback(String channel, String callbackId, String status);

  Optional<TransactionSnapshot> findTransaction(String channel, String transactionId);

  void insertTransaction(
      long id,
      long orderId,
      String channel,
      String transactionId,
      long amount,
      String currency,
      String rawDigest,
      Instant verifiedAt);

  Optional<RefundSnapshot> findRefund(String channel, String refundId);

  void insertRefund(
      long id,
      long orderId,
      String channel,
      String refundId,
      long amount,
      String reason,
      LocalDateTime now);

  Optional<Subscription> findSubscription(long id);

  Optional<Subscription> findActiveSubscription(long userId, long productId);

  List<Subscription> findSubscriptionsForReconciliation(int limit);

  /** 统计指定用户的订阅总数，用于客户端“我的订阅”分页。 */
  long countSubscriptionsByUser(long userId);

  /** 按创建时间倒序分页查询指定用户的订阅（含已取消与已过期，由客户端按状态展示）。 */
  List<Subscription> findSubscriptionsByUser(long userId, int page, int pageSize);

  void markSubscriptionReconciled(long subscriptionId, LocalDateTime reconciledAt);

  void insertSubscription(Subscription subscription);

  boolean updateSubscription(Subscription subscription, long previous);

  long grantEntitlement(
      long id,
      long userId,
      String resource,
      long delta,
      Instant expires,
      String sourceType,
      String sourceId,
      String commandId,
      LocalDateTime now);

  long consumeEntitlement(
      long id,
      long userId,
      String resource,
      long amount,
      String sourceType,
      String sourceId,
      String commandId,
      LocalDateTime now);

  EntitlementSnapshot getEntitlement(long userId, String resource);

  /** 查询指定用户当前全部权益余额（含已过期记录，由客户端按到期时间展示）。 */
  List<EntitlementSnapshot> findEntitlementsByUser(long userId);

  record EntitlementSnapshot(
      long id, long userId, String resourceKey, long balance, Instant expiresAt, long version) {}

  record CallbackSnapshot(String rawDigest, String status) {}

  record TransactionSnapshot(long orderId, long amountMinor, String currency) {}

  record RefundSnapshot(long orderId, long amountMinor) {}
}
