package com.lingxi.engagement.api;

import java.time.Instant;

/** 一个服务端增量变更。 */
public record SyncChangeResult(
    long sequence,
    String domain,
    String resourceType,
    String resourceId,
    long resourceVersion,
    String operation,
    String snapshotJson,
    Instant occurredAt) {}
