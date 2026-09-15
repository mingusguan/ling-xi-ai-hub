package com.lingxi.goal.api;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 快速记录视图。
 *
 * @param moodLevel 情绪自评；可为空。仅用于趋势观察，不产出诊断结论。
 */
public record QuickNoteResult(
    long noteId,
    long userId,
    String content,
    MoodLevel moodLevel,
    LocalDate localDate,
    Instant createdAt) {}
