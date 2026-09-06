package com.lingxi.identity.api;

import java.time.Instant;

/** 隐私请求进度视图。 */
public record PrivacyRequestResult(
    long requestId,
    PrivacyRequestType type,
    PrivacyRequestStatus status,
    int progress,
    Instant deadline,
    String resultReference) {}
