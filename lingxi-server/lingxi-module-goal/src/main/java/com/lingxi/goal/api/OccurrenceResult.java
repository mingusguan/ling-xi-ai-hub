package com.lingxi.goal.api;

import java.time.Instant;
import java.time.LocalDate;

/** 行动发生实例视图。 */
public record OccurrenceResult(
    long occurrenceId,
    long actionId,
    String actionTitle,
    Instant scheduledAt,
    LocalDate localDate,
    String timezone,
    String status) {}
