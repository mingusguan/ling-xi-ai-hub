package com.lingxi.commerce.application;

public interface PurchaseApprovalAdapter {
  boolean approved(long teenUserId, long productId, String approvalReference);
}
