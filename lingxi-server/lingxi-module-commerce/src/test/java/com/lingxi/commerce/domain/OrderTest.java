package com.lingxi.commerce.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.kernel.BusinessException;
import java.time.*;
import org.junit.jupiter.api.Test;

class OrderTest {
  private final SellablePrice price =
      new SellablePrice(
          1, "PRO", "Pro", "ALL", 2, 3, 9900, "CNY", "MONTH", "ALL", "AI_CALL", 100, true);

  @Test
  void amountMustMatchServerSnapshot() {
    var o = Order.create(1, "LX1", "key", 2, price, "HUAWEI", LocalDateTime.now());
    o.paying("ref", LocalDateTime.now());
    assertThatThrownBy(() -> o.pay("tx", 1, "CNY", LocalDateTime.now()))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void refundsPreserveOriginalOrderAndAmountInvariant() {
    var o = Order.create(1, "LX1", "key", 2, price, "HUAWEI", LocalDateTime.now());
    o.pay("tx", 9900, "CNY", LocalDateTime.now());
    o.refund(1000, LocalDateTime.now());
    assertThat(o.getStatus()).isEqualTo(Order.Status.PARTIALLY_REFUNDED);
    assertThatThrownBy(() -> o.refund(9000, LocalDateTime.now()))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void onlyUnpaidOrderCanBeClosed() {
    Order unpaid = Order.create(1, "LX1", "key", 2, price, "HUAWEI", LocalDateTime.now());
    assertThat(unpaid.close(LocalDateTime.now())).isTrue();
    assertThat(unpaid.getStatus()).isEqualTo(Order.Status.CLOSED);
    assertThat(unpaid.close(LocalDateTime.now())).isFalse();

    Order paid = Order.create(2, "LX2", "key2", 2, price, "HUAWEI", LocalDateTime.now());
    paid.pay("tx", 9900, "CNY", LocalDateTime.now());
    assertThatThrownBy(() -> paid.close(LocalDateTime.now())).isInstanceOf(BusinessException.class);
  }
}
