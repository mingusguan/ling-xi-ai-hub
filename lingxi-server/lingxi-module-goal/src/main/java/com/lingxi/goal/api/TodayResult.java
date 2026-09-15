package com.lingxi.goal.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 今日工作台聚合结果。
 *
 * <p>把「今天要做」「已经逾期」「接下来几天」明确分开：早先的实现只有一个今天起算的
 * 未来七天窗口，逾期行动根本取不到，用户看到的是一份漏掉了欠账的清单。
 *
 * @param today 今天的行动，已按工作台排序规则排好
 * @param overdue 逾期行动：未执行且计划日期早于今天
 * @param upcoming 接下来几天（不含今天）的行动
 * @param suggestions 规则化提示；接入模型后由同一结构承载模型结论
 * @param activeFocus 当前进行中的专注会话；没有则为空
 * @param quickNotes 今天最近若干条快速记录
 * @param todayTotal 今天行动总数
 * @param todayFinished 今天已完成（完成/部分完成/跳过/失败）的数量
 * @param overdueTotal 逾期总数
 * @param windowStart 逾期窗口起点，用于向用户说明逾期统计范围
 */
public record TodayResult(
    LocalDate localDate,
    List<TodayItem> today,
    List<TodayItem> overdue,
    List<TodayItem> upcoming,
    List<TodaySuggestion> suggestions,
    FocusSessionResult activeFocus,
    List<QuickNoteResult> quickNotes,
    int todayTotal,
    int todayFinished,
    int overdueTotal,
    LocalDate windowStart,
    Instant generatedAt) {

  public TodayResult {
    today = today == null ? List.of() : List.copyOf(today);
    overdue = overdue == null ? List.of() : List.copyOf(overdue);
    upcoming = upcoming == null ? List.of() : List.copyOf(upcoming);
    suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    quickNotes = quickNotes == null ? List.of() : List.copyOf(quickNotes);
  }
}
