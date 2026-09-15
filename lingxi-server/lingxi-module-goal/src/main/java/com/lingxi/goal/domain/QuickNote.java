package com.lingxi.goal.domain;

import com.lingxi.goal.api.MoodLevel;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 快速记录。
 *
 * <p>不绑定任何行动：用户想随手写一句「今天状态不好」时不应该被要求先选一个行动，
 * 否则随手记录就变成了负担。情绪自评可选，仅用于趋势观察，不产出诊断结论。
 *
 * <p>时间戳沿用领域层统一口径：UTC 的 {@link LocalDateTime}，与目标、行动、打卡一致。
 */
public record QuickNote(
    long id,
    long userId,
    String content,
    MoodLevel moodLevel,
    LocalDate localDate,
    LocalDateTime createdAt) {
  /** 单条快速记录的长度上界，避免把长文误投到这里。 */
  private static final int MAX_CONTENT_LENGTH = 2000;

  public QuickNote {
    if (id <= 0 || userId <= 0) {
      throw new BusinessException("GOAL_INVALID_QUICK_NOTE", "快速记录标识不合法");
    }
    if (content == null || content.isBlank()) {
      throw new BusinessException("GOAL_EMPTY_QUICK_NOTE", "快速记录内容不能为空");
    }
    content = content.trim();
    if (content.length() > MAX_CONTENT_LENGTH) {
      throw new BusinessException("GOAL_QUICK_NOTE_TOO_LONG", "快速记录内容过长");
    }
    if (localDate == null) {
      throw new BusinessException("GOAL_INVALID_QUICK_NOTE", "快速记录缺少记录日期");
    }
  }
}
