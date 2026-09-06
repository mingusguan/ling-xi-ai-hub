package com.lingxi.companion.api;

import java.time.LocalDateTime;

public record MemoryResult(
    long id,
    long userId,
    String purpose,
    String contentText,
    String sourceRef,
    String sensitivity,
    String status,
    long version,
    LocalDateTime updatedAt) {}
