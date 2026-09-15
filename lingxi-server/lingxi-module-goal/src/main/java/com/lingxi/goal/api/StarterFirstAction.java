package com.lingxi.goal.api;

import com.lingxi.kernel.BusinessException;

/**
 * 入门模板里的首行动候选。
 *
 * <p>PRD ONB-02 要求「创建成功后直接生成一个 5～30 分钟的首行动」。模板给出的候选
 * 也受同一约束，因此在这里校验一次：模板数据来自版本迁移，写错的话只会在用户
 * 真正采用模板时才暴露，越早拦住越好。
 *
 * @param title 行动标题
 * @param estimatedMinutes 预计时长（分钟），必须落在 5—30 之间
 * @param priority 优先级；为空按常规处理
 * @param difficulty 主观难度；为空按一般处理
 * @param completionCriteria 完成标准（怎样算做完了）
 */
public record StarterFirstAction(
    String title,
    Integer estimatedMinutes,
    PriorityLevel priority,
    ActionDifficulty difficulty,
    String completionCriteria) {
  /** 首行动时长区间，与 {@code ActionDetail} 的校验保持一致。 */
  private static final int MIN_MINUTES = 5;
  private static final int MAX_MINUTES = 30;

  public StarterFirstAction {
    if (title == null || title.isBlank()) {
      throw new BusinessException("GOAL_INVALID_STARTER_TEMPLATE", "模板首行动缺少标题");
    }
    title = title.trim();
    if (estimatedMinutes == null
        || estimatedMinutes < MIN_MINUTES
        || estimatedMinutes > MAX_MINUTES) {
      throw new BusinessException("GOAL_INVALID_STARTER_TEMPLATE", "模板首行动时长必须在 5—30 分钟内");
    }
    priority = priority == null ? PriorityLevel.NORMAL : priority;
    difficulty = difficulty == null ? ActionDifficulty.NORMAL : difficulty;
  }
}
