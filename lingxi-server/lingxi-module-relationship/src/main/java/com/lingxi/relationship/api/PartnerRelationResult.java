package com.lingxi.relationship.api;

/** 伙伴关系视图。 */
public record PartnerRelationResult(
    long relationId, long inviterUserId, long inviteeUserId, String status, long version) {}
