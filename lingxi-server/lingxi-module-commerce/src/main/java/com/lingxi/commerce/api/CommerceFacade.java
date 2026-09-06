package com.lingxi.commerce.api;

import java.util.List;

public interface CommerceFacade {
  List<ProductResult> listProducts(long userId, String scene);

  OrderResult createOrder(CreateOrderCommand command);

  OrderResult handlePaymentCallback(PaymentCallbackCommand command);

  SubscriptionResult cancelSubscription(CancelSubscriptionCommand command);

  OrderResult confirmRefund(ConfirmRefundCommand command);
}
