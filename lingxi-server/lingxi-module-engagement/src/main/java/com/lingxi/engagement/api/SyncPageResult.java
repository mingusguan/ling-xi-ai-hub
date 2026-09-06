package com.lingxi.engagement.api;

import java.util.List;

/** 游标增量同步页。 */
public record SyncPageResult(
    List<SyncChangeResult> changes, long nextCursor, boolean fullResyncRequired) {}
