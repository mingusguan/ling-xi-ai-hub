package com.lingxi.companion.api;

import java.time.LocalDateTime;

public record ConversationResult(
    long id,
    String publicId,
    long userId,
    String scene,
    String title,
    String status,
    LocalDateTime createdAt) {}
