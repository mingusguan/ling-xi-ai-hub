package com.lingxi.goal.api;

/**
 * 行动难度三档。
 *
 * <p>用于复盘计算「主观难度」偏差：用户实际感受与计划难度长期偏离时，
 * 计划本身需要调整，而不是归因于用户不努力。
 */
public enum ActionDifficulty {
  /** 轻松。 */
  EASY,
  /** 一般，未填写时的默认档。 */
  NORMAL,
  /** 困难。 */
  HARD
}
