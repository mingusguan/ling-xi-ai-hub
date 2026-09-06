package com.lingxi.relationship.api;

import java.time.Instant;
import java.util.Set;

/** 监护关系对外视图。 */
public record GuardianRelationResult(
    long relationId,
    long teenUserId,
    Long guardianUserId,
    String status,
    Set<GuardianPermissionType> permissions,
    Instant effectiveAt) {}
