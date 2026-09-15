package com.lingxi.goal.domain;

import com.lingxi.goal.api.ActionDifficulty;
import com.lingxi.goal.api.PriorityLevel;
import com.lingxi.kernel.BusinessException;

/**
 * 行动的定义性细节。
 *
 * <p>这些字段不参与「行动在哪天发生」的判定，但决定今日工作台的排序、打卡的对比基线
 * 与复盘的偏差分析，因此与时间规则分开建模，避免行动聚合继续膨胀成字段袋。
 */
public record ActionDetail(
    String description,
    PriorityLevel priority,
    ActionDifficulty difficulty,
    String completionCriteria,
    Integer estimatedMinutes,
    Long prerequisiteActionId,
    String reminderPolicy,
    /**
     * 是否为新手引导生成的首行动（PRD ONB-02）。
     *
     * <p>首行动是降低启动门槛的一次性动作，必须能在 5—30 分钟内做完；标记它而不是
     * 另建一张表，是因为它参与今日工作台排序与打卡统计的方式和普通行动完全一致。
     */
    boolean first) {
  /** 预计时长上界：一天的总分钟数，超过该值视为填写错误。 */
  private static final int MAX_ESTIMATED_MINUTES = 24 * 60;
  /** 首行动时长区间（PRD ONB-02）：低于 5 分钟没有推进感，超过 30 分钟又变成新的门槛。 */
  private static final int FIRST_ACTION_MIN_MINUTES = 5;
  private static final int FIRST_ACTION_MAX_MINUTES = 30;

  public ActionDetail {
    description = trimToNull(description);
    completionCriteria = trimToNull(completionCriteria);
    reminderPolicy = trimToNull(reminderPolicy);
    priority = priority == null ? PriorityLevel.NORMAL : priority;
    difficulty = difficulty == null ? ActionDifficulty.NORMAL : difficulty;
    if (estimatedMinutes != null
        && (estimatedMinutes <= 0 || estimatedMinutes > MAX_ESTIMATED_MINUTES)) {
      throw new BusinessException("GOAL_INVALID_ESTIMATED_MINUTES", "行动预计时长超出合理范围");
    }
    if (prerequisiteActionId != null && prerequisiteActionId <= 0) {
      throw new BusinessException("GOAL_INVALID_PREREQUISITE", "前置行动标识不合法");
    }
    if (first && (estimatedMinutes == null
        || estimatedMinutes < FIRST_ACTION_MIN_MINUTES
        || estimatedMinutes > FIRST_ACTION_MAX_MINUTES)) {
      // 首行动必须给出可核对时长：缺了它「5—30 分钟就能做完」这条产品承诺无法验证。
      throw new BusinessException(
          "GOAL_INVALID_FIRST_ACTION",
          "首行动必须填写 5—30 分钟的预计时长");
    }
  }

  /** 只有标题时的最小细节，供既有入口与历史数据兼容使用。 */
  public static ActionDetail defaults() {
    return new ActionDetail(
        null, PriorityLevel.NORMAL, ActionDifficulty.NORMAL, null, null, null, null, false);
  }

  /** 复制细节并替换首行动标记，供引导生成与普通行动共存。 */
  public ActionDetail asFirst(boolean value) {
    return new ActionDetail(
        description,
        priority,
        difficulty,
        completionCriteria,
        estimatedMinutes,
        prerequisiteActionId,
        reminderPolicy,
        value);
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
