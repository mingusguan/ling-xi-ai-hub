package com.lingxi.commerce.infrastructure.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.commerce.infrastructure.persistence.*;
import com.lingxi.identity.api.PrivacyProcessingContext;
import com.lingxi.identity.api.PrivacyRequestType;
import com.lingxi.identity.api.PrivacyScope;
import com.lingxi.kernel.BusinessException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommercePrivacyDataContributorTest {
  private OrderMapper orderMapper;
  private SubscriptionMapper subscriptionMapper;
  private RefundMapper refundMapper;
  private CommercePrivacyDataContributor contributor;

  @BeforeEach
  void setUp() {
    orderMapper = mock(OrderMapper.class);
    subscriptionMapper = mock(SubscriptionMapper.class);
    refundMapper = mock(RefundMapper.class);
    contributor =
        new CommercePrivacyDataContributor(
            orderMapper,
            mock(OrderItemMapper.class),
            mock(PaymentTransactionMapper.class),
            refundMapper,
            subscriptionMapper,
            mock(EntitlementMapper.class),
            mock(EntitlementLedgerMapper.class),
            new ObjectMapper());
  }

  @Test
  void payingOrderBlocksAccountClosure() {
    when(orderMapper.selectCount(any())).thenReturn(1L);

    assertThatThrownBy(
            () ->
                contributor.process(
                    new PrivacyProcessingContext(
                        10L, 20L, PrivacyRequestType.CLOSE_ACCOUNT, new PrivacyScope(Set.of()))))
        .isInstanceOfSatisfying(
            BusinessException.class,
            error -> assertThat(error.getCode()).isEqualTo("PRIVACY_COMMERCE_PENDING"));

    verifyNoInteractions(subscriptionMapper);
    verify(orderMapper, never()).update(any(), any());
  }

  @Test
  void ordinaryDataDeletionPreservesCommerceEvidenceAndEntitlements() {
    assertThat(
            contributor
                .process(
                    new PrivacyProcessingContext(
                        10L, 20L, PrivacyRequestType.DELETE_DATA, new PrivacyScope(Set.of())))
                .affectedRows())
        .isZero();
    verifyNoInteractions(orderMapper, subscriptionMapper);
  }

  @Test
  void nonTerminalRefundBlocksAccountClosure() {
    OrderEntity order = new OrderEntity();
    order.setId(100L);
    when(orderMapper.selectCount(any())).thenReturn(0L);
    when(orderMapper.selectList(any())).thenReturn(List.of(order));
    when(refundMapper.selectCount(any())).thenReturn(1L);

    assertThatThrownBy(
            () ->
                contributor.process(
                    new PrivacyProcessingContext(
                        11L, 20L, PrivacyRequestType.CLOSE_ACCOUNT, new PrivacyScope(Set.of()))))
        .isInstanceOfSatisfying(
            BusinessException.class,
            error -> assertThat(error.getCode()).isEqualTo("PRIVACY_REFUND_PENDING"));
  }
}
