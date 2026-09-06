package com.lingxi.relationship.api;

import java.time.Instant;
import java.util.Set;

/** 分享链接创建或访问视图。 */
public record ShareLinkResult(
    long shareId,
    String rawToken,
    String resourceType,
    String resourceId,
    Set<String> fields,
    String snapshotJson,
    String status,
    int visitCount,
    Integer visitLimit,
    Instant expiresAt,
    long version) {}
