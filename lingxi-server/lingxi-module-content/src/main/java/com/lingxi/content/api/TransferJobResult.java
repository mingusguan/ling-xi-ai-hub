package com.lingxi.content.api;

import java.time.Instant;

public record TransferJobResult(
    long jobId,
    String type,
    long userId,
    String status,
    String previewJson,
    String errorJson,
    Long resultFileId,
    Instant expiresAt,
    long version) {}
