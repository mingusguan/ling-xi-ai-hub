package com.lingxi.commerce.api;

import com.lingxi.kernel.PageResult;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/** 商业化后台的脱敏只读门面；写命令继续走领域状态机。 */
public interface AdminCommerceFacade {
  CommerceOverview overview(long adminId);
  List<ProductSummary> products(long adminId);
  PageResult<OrderSummary> orders(long adminId, String keyword, String status, int page, int pageSize);
  PageResult<SubscriptionSummary> subscriptions(long adminId, String status, int page, int pageSize);
  PageResult<EntitlementSummary> entitlements(long adminId, Long userId, int page, int pageSize);

  record CommerceOverview(long orderCount, long paidOrderCount, long revenueMinor,
      long activeSubscriptions, long refundCount) {}
  record ProductSummary(long id, String productKey, String name, String scene,
      String billingPeriod, String agePolicy, String entitlementKey, long entitlementAmount,
      String status, List<PriceSummary> prices) {}
  record PriceSummary(long id, int versionNo, long amountMinor, String currency, String status) {}
  record OrderSummary(long id, String orderNo, long userId, long productId, long amountMinor,
      String currency, String channel, String status, long refundedMinor, long version,
      LocalDateTime createdAt, LocalDateTime updatedAt) {}
  record SubscriptionSummary(long id, long userId, long productId, String channel,
      String status, Instant periodEnd, String cancelMode, long version, LocalDateTime updatedAt) {}
  record EntitlementSummary(long id, long userId, String resourceKey, long balance,
      Instant expiresAt, long version, LocalDateTime updatedAt) {}
}
