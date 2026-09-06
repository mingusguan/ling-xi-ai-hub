package com.lingxi.relationship.api;

import java.time.Instant;
import java.util.Set;

/** 伙伴目标授权视图。 */
public record PartnerGrantResult(
    long grantId,
    long relationId,
    long ownerUserId,
    long goalId,
    Set<PartnerPermission> permissions,
    Instant expiresAt,
    String status,
    long version) {}
