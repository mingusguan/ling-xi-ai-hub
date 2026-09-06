package com.lingxi.commerce.api;

public record ConfirmRefundCommand(
    String channel, String refundTransactionId, String orderNo, long amountMinor, String reason) {}
