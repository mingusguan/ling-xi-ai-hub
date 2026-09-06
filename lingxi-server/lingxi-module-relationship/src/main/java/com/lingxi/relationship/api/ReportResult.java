package com.lingxi.relationship.api;

import java.time.LocalDateTime;

public record ReportResult(long reportId, String status, LocalDateTime createdAt) {}
