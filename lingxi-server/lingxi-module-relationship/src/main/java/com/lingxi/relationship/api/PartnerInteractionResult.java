package com.lingxi.relationship.api;

import java.time.LocalDateTime;

public record PartnerInteractionResult(
    long interactionId,
    long relationId,
    long grantId,
    long actorUserId,
    String interactionType,
    String resourceId,
    LocalDateTime createdAt) {}
