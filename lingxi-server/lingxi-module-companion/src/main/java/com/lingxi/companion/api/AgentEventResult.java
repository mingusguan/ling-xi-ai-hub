package com.lingxi.companion.api;

import java.time.LocalDateTime;

public record AgentEventResult(
    long eventId, String eventType, String safePayloadJson, LocalDateTime createdAt) {}
