package com.lingxi.content.api;

public record CreateUploadTicketCommand(
    String requestKey,
    long ownerUserId,
    String purpose,
    String originalName,
    long sizeBytes,
    String mimeType,
    String contentHash,
    String sensitivity) {}
