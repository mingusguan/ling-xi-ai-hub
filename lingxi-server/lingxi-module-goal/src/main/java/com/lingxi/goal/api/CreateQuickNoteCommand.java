package com.lingxi.goal.api;

/**
 * 新建快速记录命令。
 *
 * @param content 记录内容
 * @param moodLevel 情绪自评；可为空
 * @param timezone 调用方时区；服务端据此把记录归到正确的本地日期
 */
public record CreateQuickNoteCommand(
    String requestKey, long userId, String content, MoodLevel moodLevel, String timezone) {}
