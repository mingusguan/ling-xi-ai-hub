package com.lingxi.commerce.api;

import com.lingxi.kernel.PageResult;
import java.time.Instant;
import java.time.LocalDateTime;

/** 商业化后台写入与对账处置门面。 */
public interface AdminCommerceManagementFacade {
  ManagedResult saveProduct(long adminId, ProductCommand command);
  ManagedResult savePrice(long adminId, PriceCommand command);
  ManagedResult savePromotion(long adminId, PromotionCommand command);
  OrderResult confirmRefund(long adminId, RefundCommand command);
  EntitlementResult adjustEntitlement(long adminId, EntitlementAdjustmentCommand command);
  PageResult<PromotionSummary> promotions(long adminId, String status, int page, int pageSize);
  PageResult<ReconciliationSummary> reconciliationCases(long adminId, String status, int page, int pageSize);
  ReconciliationSummary resolveReconciliation(long adminId, ReconciliationCommand command);

  record OperationContext(String reason,String ticketNo,String requestId,boolean recentAuthentication){}
  record ProductCommand(long id,String productKey,String name,String scene,String billingPeriod,
      String agePolicy,String entitlementKey,long entitlementAmount,String status,
      long expectedVersion,OperationContext context){}
  record PriceCommand(long id,long productId,int versionNo,long amountMinor,String currency,
      String status,long expectedVersion,OperationContext context){}
  record PromotionCommand(long id,String promotionKey,String name,String promotionType,
      String ruleJson,String audienceRule,LocalDateTime startsAt,LocalDateTime endsAt,String status,
      long expectedVersion,OperationContext context){}
  record RefundCommand(String channel,String refundTransactionId,String orderNo,long amountMinor,
      String refundReason,OperationContext context){}
  record EntitlementAdjustmentCommand(long userId,String resourceKey,long delta,Instant expiresAt,
      String commandId,OperationContext context){}
  record ReconciliationCommand(long id,String resolution,long expectedVersion,OperationContext context){}
  record ManagedResult(long id,String resourceType,String resourceKey,String status,long version,
      LocalDateTime updatedAt){}
  record PromotionSummary(long id,String promotionKey,String name,String promotionType,
      String ruleJson,String audienceRule,LocalDateTime startsAt,LocalDateTime endsAt,String status,
      long version,LocalDateTime updatedAt){}
  record ReconciliationSummary(long id,String caseNo,String channel,String businessType,
      String businessId,long expectedMinor,long actualMinor,String currency,String differenceReason,
      String status,Long reviewerAdminId,String resolution,long version,LocalDateTime createdAt,
      LocalDateTime updatedAt){}
}
