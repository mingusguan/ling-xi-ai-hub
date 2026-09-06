package com.lingxi.relationship.api;

public record CreatePartnerInteractionCommand(
    long actorUserId, long goalId, String interactionType, String resourceId, String contentJson) {}
