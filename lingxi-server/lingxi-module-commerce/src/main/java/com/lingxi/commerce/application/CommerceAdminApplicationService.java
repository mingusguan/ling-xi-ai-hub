package com.lingxi.commerce.application;

import com.lingxi.commerce.api.AdminCommerceFacade;
import com.lingxi.commerce.domain.CommerceAdminReadRepository;
import com.lingxi.identity.api.AdminAuthorizationFacade;
import com.lingxi.kernel.*;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CommerceAdminApplicationService implements AdminCommerceFacade {
  private final CommerceAdminReadRepository reads;
  private final AdminAuthorizationFacade admins;
  public CommerceAdminApplicationService(CommerceAdminReadRepository reads, AdminAuthorizationFacade admins) {
    this.reads = reads; this.admins = admins;
  }
  public CommerceOverview overview(long adminId) { require(adminId); return reads.overview(); }
  public List<ProductSummary> products(long adminId) { require(adminId); return reads.products(); }
  public PageResult<OrderSummary> orders(long adminId, String keyword, String status, int page, int size) {
    require(adminId); return reads.orders(keyword, status, page(page), size(size));
  }
  public PageResult<SubscriptionSummary> subscriptions(long adminId, String status, int page, int size) {
    require(adminId); return reads.subscriptions(status, page(page), size(size));
  }
  public PageResult<EntitlementSummary> entitlements(long adminId, Long userId, int page, int size) {
    require(adminId); return reads.entitlements(userId, page(page), size(size));
  }
  private void require(long id) { if (!admins.allowed(id, "commerce:read")) throw new BusinessException("ADMIN_FORBIDDEN", "管理员权限不足"); }
  private int page(int value) { return Math.max(1, value); }
  private int size(int value) { return Math.min(100, Math.max(1, value)); }
}
