package com.lingxi.commerce.domain;

import com.lingxi.commerce.api.AdminCommerceManagementFacade.*;
import com.lingxi.kernel.PageResult;
import java.time.LocalDateTime;

/** 商业化后台资源仓储。 */
public interface CommerceAdminWriteRepository {
  ManagedResult saveProduct(long id,ProductCommand command,LocalDateTime now);
  ManagedResult savePrice(long id,PriceCommand command,LocalDateTime now);
  ManagedResult savePromotion(long id,PromotionCommand command,LocalDateTime now);
  PageResult<PromotionSummary> promotions(String status,int page,int size);
  PageResult<ReconciliationSummary> reconciliationCases(String status,int page,int size);
  ReconciliationSummary resolveReconciliation(long adminId,ReconciliationCommand command,LocalDateTime now);
}
