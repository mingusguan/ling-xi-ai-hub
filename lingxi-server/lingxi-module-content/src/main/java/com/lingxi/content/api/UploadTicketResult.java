package com.lingxi.content.api;

import java.time.Instant;

public record UploadTicketResult(
    long fileId,
    String publicId,
    String objectKey,
    String uploadReference,
    Instant expiresAt,
    String status,
    long version) {}
