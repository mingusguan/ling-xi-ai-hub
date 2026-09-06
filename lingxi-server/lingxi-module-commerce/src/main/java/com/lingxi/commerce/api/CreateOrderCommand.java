package com.lingxi.commerce.api;

public record CreateOrderCommand(
    String businessOrderKey,
    long userId,
    long productId,
    int priceVersion,
    String channel,
    String guardianApprovalReference) {}
