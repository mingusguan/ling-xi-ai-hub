package com.lingxi.commerce.api;

import java.time.Instant;

public record EntitlementResult(
    long userId, String resourceKey, long balance, Instant expiresAt, long version) {}
