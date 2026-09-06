package com.lingxi.identity.api;

import java.time.Instant;

/** 本人鉴权后返回的隐私导出内容。 */
public record PrivacyExportResult(
    long requestId, String contentType, String content, Instant expiresAt) {}
