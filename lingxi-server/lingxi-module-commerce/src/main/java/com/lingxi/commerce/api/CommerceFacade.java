package com.lingxi.commerce.api;

import com.lingxi.kernel.PageResult;
import java.util.List;

public interface CommerceFacade {
  List<ProductResult> listProducts(long userId, String scene);

  /** 按创建时间倒序分页查询本人订单。 */
  PageResult<OrderResult> listOrders(long userId, int page, int pageSize);

  /** 分页查询本人订阅（含已取消与已过期，由客户端按状态展示）。 */
  PageResult<SubscriptionResult> listSubscriptions(long userId, int page, int pageSize);

  OrderResult createOrder(CreateOrderCommand command);

  OrderResult handlePaymentCallback(PaymentCallbackCommand command);

  SubscriptionResult cancelSubscription(CancelSubscriptionCommand command);

  OrderResult confirmRefund(ConfirmRefundCommand command);
}
