package com.lingxi.content.api;

public record FileResult(
    long fileId,
    String publicId,
    long ownerUserId,
    String purpose,
    String originalName,
    long sizeBytes,
    String mimeType,
    String contentHash,
    String sensitivity,
    String status,
    String scanResult,
    long version) {}
