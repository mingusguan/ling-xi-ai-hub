package com.lingxi.commerce.domain;

public record SellablePrice(
    long productId,
    String productKey,
    String productName,
    String scene,
    long priceId,
    int priceVersion,
    long amountMinor,
    String currency,
    String billingPeriod,
    String agePolicy,
    String entitlementKey,
    long entitlementAmount,
    boolean active) {}
