package com.lingxi.commerce.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lingxi.commerce.api.ConfirmRefundCommand;
import com.lingxi.commerce.domain.CommerceRepository;
import com.lingxi.commerce.domain.Order;
import com.lingxi.commerce.domain.SellablePrice;
import com.lingxi.kernel.DomainEventPublisher;
import com.lingxi.kernel.IdGenerator;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CommerceTransactionServiceTest {
  @Mock private CommerceRepository repository;
  @Mock private IdGenerator ids;
  @Mock private DomainEventPublisher events;
  private CommerceTransactionService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new CommerceTransactionService(repository, ids, events);
  }

  @Test
  void shouldFulfilPaidOrderFromImmutableOrderSnapshot() {
    Order order = order(Order.Status.PAYING, 3, 0);
    SellablePrice snapshot = price("SNAPSHOT_CREDITS", 9);
    PaymentChannelAdapter.VerifiedPayment payment = payment("cb-1", "tx-1", "digest-1");
    when(repository.findOrderByNo("LX-1")).thenReturn(Optional.of(order));
    when(repository.findCallback("WECHAT", "cb-1")).thenReturn(Optional.empty());
    when(repository.findTransaction("WECHAT", "tx-1")).thenReturn(Optional.empty());
    when(repository.updateOrder(order, 3)).thenReturn(true);
    when(repository.findOrderPriceSnapshot(1)).thenReturn(Optional.of(snapshot));

    service.applyVerifiedPayment("WECHAT", payment);

    verify(repository).findOrderPriceSnapshot(1);
    verify(repository, never()).findPrice(anyLong(), anyInt());
    verify(repository)
        .grantEntitlement(
            anyLong(),
            org.mockito.ArgumentMatchers.eq(8L),
            org.mockito.ArgumentMatchers.eq("SNAPSHOT_CREDITS"),
            org.mockito.ArgumentMatchers.eq(9L),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq("ORDER"),
            org.mockito.ArgumentMatchers.eq("1"),
            org.mockito.ArgumentMatchers.eq("ORDER:1"),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldRejectReusedChannelTransactionWithDifferentFacts() {
    Order order = order(Order.Status.PAID, 4, 0);
    PaymentChannelAdapter.VerifiedPayment payment = payment("cb-2", "tx-1", "digest-new");
    when(repository.findOrderByNo("LX-1")).thenReturn(Optional.of(order));
    when(repository.findCallback("WECHAT", "cb-2")).thenReturn(Optional.empty());
    when(repository.findTransaction("WECHAT", "tx-1"))
        .thenReturn(Optional.of(new CommerceRepository.TransactionSnapshot(1, 1888, "CNY")));

    assertThatThrownBy(() -> service.applyVerifiedPayment("WECHAT", payment))
        .hasMessageContaining("原交易不一致");
  }

  @Test
  void shouldAcceptReusedChannelTransactionWhenBusinessFactsMatch() {
    Order order = order(Order.Status.PAID, 4, 0);
    PaymentChannelAdapter.VerifiedPayment payment = payment("cb-3", "tx-1", "digest-new");
    when(repository.findOrderByNo("LX-1")).thenReturn(Optional.of(order));
    when(repository.findCallback("WECHAT", "cb-3")).thenReturn(Optional.empty());
    when(repository.findTransaction("WECHAT", "tx-1"))
        .thenReturn(Optional.of(new CommerceRepository.TransactionSnapshot(1, 1999, "CNY")));

    service.applyVerifiedPayment("WECHAT", payment);

    verify(repository)
        .insertCallback(
            anyLong(),
            org.mockito.ArgumentMatchers.eq("WECHAT"),
            org.mockito.ArgumentMatchers.eq("cb-3"),
            org.mockito.ArgumentMatchers.eq("digest-new"),
            org.mockito.ArgumentMatchers.eq("PROCESSED"),
            org.mockito.ArgumentMatchers.any());
    verify(repository, never())
        .insertTransaction(
            anyLong(),
            anyLong(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            anyLong(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldRejectRefundTransactionReusedForAnotherOrder() {
    Order order = order(Order.Status.PAID, 4, 0);
    ConfirmRefundCommand command =
        new ConfirmRefundCommand("WECHAT", "refund-1", "LX-1", 500, "用户申请");
    when(repository.findOrderByNo("LX-1")).thenReturn(Optional.of(order));
    when(repository.findRefund("WECHAT", "refund-1"))
        .thenReturn(Optional.of(new CommerceRepository.RefundSnapshot(99, 500)));

    assertThatThrownBy(() -> service.applyRefund(command, 1)).hasMessageContaining("原退款不一致");
  }

  private Order order(Order.Status status, long version, long refunded) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    return Order.rehydrate(
        1, "LX-1", "biz-1", 8, 10, 20, 2, 1999, "CNY", "WECHAT", status, "pay-ref", refunded,
        version, now, now);
  }

  private SellablePrice price(String entitlementKey, long entitlementAmount) {
    return new SellablePrice(
        10,
        "PROD",
        "商品",
        "ALL",
        20,
        2,
        1999,
        "CNY",
        "ONE_TIME",
        "ALL",
        entitlementKey,
        entitlementAmount,
        true);
  }

  private PaymentChannelAdapter.VerifiedPayment payment(
      String callbackId, String transactionId, String digest) {
    return new PaymentChannelAdapter.VerifiedPayment(
        callbackId, transactionId, "LX-1", 1999, "CNY", Instant.now(), digest, null, null);
  }
}
