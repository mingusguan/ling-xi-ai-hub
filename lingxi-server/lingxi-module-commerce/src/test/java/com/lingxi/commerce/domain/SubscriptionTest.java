package com.lingxi.commerce.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.kernel.BusinessException;
import java.time.*;
import org.junit.jupiter.api.Test;

class SubscriptionTest {
  @Test
  void cancellationChecksOwnerAndModeBeforeChannelCall() {
    Subscription subscription =
        Subscription.activate(1, 10, 20, "HUAWEI", "sub-1", Instant.now(), LocalDateTime.now());

    assertThatThrownBy(() -> subscription.assertCancellable(11, "IMMEDIATE", 0))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> subscription.assertCancellable(10, "UNKNOWN", 0))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void duplicateCancellationReturnsTheCompletedState() {
    Subscription subscription =
        Subscription.activate(1, 10, 20, "HUAWEI", "sub-1", Instant.now(), LocalDateTime.now());
    subscription.cancel(10, "PERIOD_END", 0, LocalDateTime.now());

    assertThat(subscription.isCompletedCancellation(10, "PERIOD_END", 0)).isTrue();
    assertThat(subscription.isCompletedCancellation(10, "IMMEDIATE", 0)).isFalse();
  }

  @Test
  void reconciliationUsesChannelFactAndIsIdempotent() {
    Instant oldEnd = Instant.parse("2026-08-01T00:00:00Z");
    Instant renewedEnd = Instant.parse("2026-09-01T00:00:00Z");
    Subscription subscription =
        Subscription.activate(1, 10, 20, "HUAWEI", "sub-1", oldEnd, LocalDateTime.now());

    assertThat(subscription.reconcile(Subscription.Status.ACTIVE, renewedEnd, LocalDateTime.now()))
        .isTrue();
    assertThat(subscription.getPeriodEnd()).isEqualTo(renewedEnd);
    assertThat(subscription.reconcile(Subscription.Status.ACTIVE, renewedEnd, LocalDateTime.now()))
        .isFalse();
  }
}
