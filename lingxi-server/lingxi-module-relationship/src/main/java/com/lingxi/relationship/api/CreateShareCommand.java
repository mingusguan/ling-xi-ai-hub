package com.lingxi.relationship.api;

import java.time.Instant;
import java.util.Set;

/** 创建字段受控分享链接。 */
public record CreateShareCommand(
    String requestKey,
    long ownerUserId,
    String resourceType,
    String resourceId,
    Set<String> fields,
    Instant expiresAt,
    Integer visitLimit,
    String password) {}
