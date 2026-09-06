package com.lingxi.commerce.api;

public record CancelSubscriptionCommand(
    String requestKey,
    long userId,
    long subscriptionId,
    String cancelMode,
    long expectedVersion,
    boolean recentAuthentication) {}
