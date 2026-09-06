package com.lingxi.commerce.api;

public record ProductResult(
    long productId,
    String productKey,
    String name,
    String scene,
    long priceId,
    int priceVersion,
    long amountMinor,
    String currency,
    String billingPeriod,
    String agePolicy) {}
