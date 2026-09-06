package com.lingxi.relationship.api;

import java.time.Instant;
import java.util.Set;

/** 监护邀请结果，原始令牌只在创建时返回一次。 */
public record GuardianInvitationResult(
    long relationId,
    String invitationToken,
    String status,
    Instant expiresAt,
    Set<GuardianPermissionType> permissions) {}
