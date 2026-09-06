package com.lingxi.commerce.api;

public record ConsumeEntitlementCommand(
    String commandId,
    long userId,
    String resourceKey,
    long amount,
    String sourceType,
    String sourceId) {}
