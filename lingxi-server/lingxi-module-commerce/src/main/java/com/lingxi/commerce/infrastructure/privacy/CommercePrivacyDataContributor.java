package com.lingxi.commerce.infrastructure.privacy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.commerce.infrastructure.persistence.*;
import com.lingxi.identity.api.*;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 负责交易导出及注销前交易终态校验，避免在途支付或订阅被静默丢弃。 */
@Component
public class CommercePrivacyDataContributor implements PrivacyDataContributor {
  private final OrderMapper orderMapper;
  private final OrderItemMapper itemMapper;
  private final PaymentTransactionMapper transactionMapper;
  private final RefundMapper refundMapper;
  private final SubscriptionMapper subscriptionMapper;
  private final EntitlementMapper entitlementMapper;
  private final EntitlementLedgerMapper ledgerMapper;
  private final ObjectMapper objectMapper;

  public CommercePrivacyDataContributor(
      OrderMapper orderMapper, OrderItemMapper itemMapper,
      PaymentTransactionMapper transactionMapper, RefundMapper refundMapper,
      SubscriptionMapper subscriptionMapper, EntitlementMapper entitlementMapper,
      EntitlementLedgerMapper ledgerMapper, ObjectMapper objectMapper) {
    this.orderMapper = orderMapper;
    this.itemMapper = itemMapper;
    this.transactionMapper = transactionMapper;
    this.refundMapper = refundMapper;
    this.subscriptionMapper = subscriptionMapper;
    this.entitlementMapper = entitlementMapper;
    this.ledgerMapper = ledgerMapper;
    this.objectMapper = objectMapper;
  }

  @Override public String moduleName() { return "commerce"; }

  @Override
  @Transactional
  public PrivacyContribution process(PrivacyProcessingContext context) {
    long userId = context.userId();
    PrivacyRequestType type = context.type();
    if (type == PrivacyRequestType.EXPORT) {
      return PrivacyContribution.exported(export(userId));
    }
    // 普通数据清理不能删除支付凭证和有效权益，仅账户注销处理交易数据。
    if (type != PrivacyRequestType.CLOSE_ACCOUNT) {
      return PrivacyContribution.unchanged();
    }
    assertClosable(userId);
    return PrivacyContribution.deleted(logicallyDelete(userId));
  }

  private void assertClosable(long userId) {
    long unsettledOrders = orderMapper.selectCount(Wrappers.<OrderEntity>lambdaQuery()
        .eq(OrderEntity::getUserId, userId).in(OrderEntity::getStatus, "PAYING", "DISPUTED"));
    if (unsettledOrders > 0) {
      throw new BusinessException("PRIVACY_COMMERCE_PENDING", "存在支付中的订单，暂不能完成注销");
    }
    List<Long> orderIds = orderMapper.selectList(
            Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getUserId, userId))
        .stream().map(OrderEntity::getId).toList();
    if (!orderIds.isEmpty()) {
      long unsettledRefunds = refundMapper.selectCount(
          Wrappers.<RefundEntity>lambdaQuery()
              .in(RefundEntity::getOrderId, orderIds)
              .notIn(RefundEntity::getStatus, "CONFIRMED", "REJECTED", "FAILED"));
      if (unsettledRefunds > 0) {
        throw new BusinessException("PRIVACY_REFUND_PENDING", "存在未终态退款，暂不能完成注销");
      }
    }
    long activeSubscriptions = subscriptionMapper.selectCount(
        Wrappers.<SubscriptionEntity>lambdaQuery()
            .eq(SubscriptionEntity::getUserId, userId)
            .notIn(SubscriptionEntity::getStatus, "EXPIRED", "CANCELLED", "REFUNDED"));
    long unreconciledCancellations = subscriptionMapper.selectCount(
        Wrappers.<SubscriptionEntity>lambdaQuery()
            .eq(SubscriptionEntity::getUserId, userId)
            .in(SubscriptionEntity::getStatus, "CANCELLED", "REFUNDED")
            .isNull(SubscriptionEntity::getLastReconciledAt));
    if (activeSubscriptions > 0 || unreconciledCancellations > 0) {
      throw new BusinessException(
          "PRIVACY_SUBSCRIPTION_CANCEL_PENDING", "自动续费尚未确认取消，暂不能完成注销");
    }
    orderMapper.update(null, Wrappers.<OrderEntity>lambdaUpdate()
        .eq(OrderEntity::getUserId, userId).eq(OrderEntity::getStatus, "CREATED")
        .set(OrderEntity::getStatus, "CLOSED")
        .set(OrderEntity::getUpdatedAt, LocalDateTime.now(ZoneOffset.UTC))
        .setSql("version = version + 1"));
  }

  private int logicallyDelete(long userId) {
    List<Long> orderIds = orderMapper.selectList(Wrappers.<OrderEntity>lambdaQuery()
            .select(OrderEntity::getId).eq(OrderEntity::getUserId, userId))
        .stream().map(OrderEntity::getId).toList();
    int affectedRows = 0;
    if (!orderIds.isEmpty()) {
      affectedRows += itemMapper.delete(Wrappers.<OrderItemEntity>lambdaQuery()
          .in(OrderItemEntity::getOrderId, orderIds));
      affectedRows += transactionMapper.delete(Wrappers.<PaymentTransactionEntity>lambdaQuery()
          .in(PaymentTransactionEntity::getOrderId, orderIds));
      affectedRows += refundMapper.delete(Wrappers.<RefundEntity>lambdaQuery()
          .in(RefundEntity::getOrderId, orderIds));
      affectedRows += orderMapper.deleteByIds(orderIds);
    }
    affectedRows += subscriptionMapper.delete(Wrappers.<SubscriptionEntity>lambdaQuery()
        .eq(SubscriptionEntity::getUserId, userId));
    affectedRows += entitlementMapper.delete(Wrappers.<EntitlementEntity>lambdaQuery()
        .eq(EntitlementEntity::getUserId, userId));
    affectedRows += ledgerMapper.delete(Wrappers.<EntitlementLedgerEntity>lambdaQuery()
        .eq(EntitlementLedgerEntity::getUserId, userId));
    return affectedRows;
  }

  private String export(long userId) {
    List<OrderEntity> orders = orderMapper.selectList(Wrappers.<OrderEntity>lambdaQuery()
        .eq(OrderEntity::getUserId, userId));
    List<Long> orderIds = orders.stream().map(OrderEntity::getId).toList();
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("orders", orders);
    data.put("orderItems", orderIds.isEmpty() ? List.of() : itemMapper.selectList(
        Wrappers.<OrderItemEntity>lambdaQuery().in(OrderItemEntity::getOrderId, orderIds)));
    data.put("transactions", orderIds.isEmpty() ? List.of() : transactionMapper.selectList(
        Wrappers.<PaymentTransactionEntity>lambdaQuery()
            .select(
                PaymentTransactionEntity::getId,
                PaymentTransactionEntity::getOrderId,
                PaymentTransactionEntity::getChannel,
                PaymentTransactionEntity::getTransactionId,
                PaymentTransactionEntity::getAmountMinor,
                PaymentTransactionEntity::getCurrency,
                PaymentTransactionEntity::getVerifiedAt)
            .in(PaymentTransactionEntity::getOrderId, orderIds)));
    data.put("refunds", orderIds.isEmpty() ? List.of() : refundMapper.selectList(
        Wrappers.<RefundEntity>lambdaQuery().in(RefundEntity::getOrderId, orderIds)));
    data.put("subscriptions", subscriptionMapper.selectList(Wrappers.<SubscriptionEntity>lambdaQuery()
        .eq(SubscriptionEntity::getUserId, userId)));
    data.put("entitlements", entitlementMapper.selectList(Wrappers.<EntitlementEntity>lambdaQuery()
        .eq(EntitlementEntity::getUserId, userId)));
    data.put("entitlementLedger", ledgerMapper.selectList(
        Wrappers.<EntitlementLedgerEntity>lambdaQuery()
            .eq(EntitlementLedgerEntity::getUserId, userId)));
    try { return objectMapper.writeValueAsString(data); }
    catch (JsonProcessingException e) { throw new IllegalStateException("交易数据导出失败", e); }
  }
}
