package com.lingxi.commerce.api;

import java.time.LocalDateTime;

public record OrderResult(
    long orderId,
    String orderNo,
    long userId,
    long productId,
    long amountMinor,
    String currency,
    String status,
    String paymentReference,
    long refundedMinor,
    long version,
    LocalDateTime createdAt) {}
