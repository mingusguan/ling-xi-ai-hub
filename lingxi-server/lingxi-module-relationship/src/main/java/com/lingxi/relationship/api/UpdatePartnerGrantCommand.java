package com.lingxi.relationship.api;

import java.time.Instant;
import java.util.Set;

/** 为单个目标授予或更新伙伴权限。 */
public record UpdatePartnerGrantCommand(
    long ownerUserId,
    long relationId,
    long goalId,
    Set<PartnerPermission> permissions,
    Instant expiresAt,
    long expectedVersion) {}
