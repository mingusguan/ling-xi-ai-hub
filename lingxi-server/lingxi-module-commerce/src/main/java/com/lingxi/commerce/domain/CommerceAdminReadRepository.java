package com.lingxi.commerce.domain;

import com.lingxi.commerce.api.AdminCommerceFacade.*;
import com.lingxi.kernel.PageResult;
import java.util.List;

/** commerce 自有表的后台读取端口。 */
public interface CommerceAdminReadRepository {
  CommerceOverview overview();
  List<ProductSummary> products();
  PageResult<OrderSummary> orders(String keyword, String status, int page, int pageSize);
  PageResult<SubscriptionSummary> subscriptions(String status, int page, int pageSize);
  PageResult<EntitlementSummary> entitlements(Long userId, int page, int pageSize);
}
