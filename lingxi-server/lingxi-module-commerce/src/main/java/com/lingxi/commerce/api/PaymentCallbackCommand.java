package com.lingxi.commerce.api;

import java.util.Map;

public record PaymentCallbackCommand(
    String channel, String rawPayload, Map<String, String> headers) {}
