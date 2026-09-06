package com.lingxi.commerce.api;

import java.time.Instant;

public record SubscriptionResult(
    long subscriptionId,
    long userId,
    long productId,
    String status,
    Instant periodEnd,
    String cancelMode,
    long version) {}
