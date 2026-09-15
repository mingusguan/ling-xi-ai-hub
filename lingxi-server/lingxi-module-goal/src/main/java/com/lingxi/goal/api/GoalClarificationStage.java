package com.lingxi.goal.api;

/**
 * 新手引导首目标引导的澄清阶段（PRD 8.2 ONB-02）。
 *
 * <p>PRD 要求「AI 每次最多提出一个关键澄清问题，避免一次展示长问卷」，
 * 并用「用户可随时切换为手动创建」保底。阶段因此显式落库而不是只存在客户端：
 * 换设备或中途退出后，用户不必从第一个问题重新回答。
 *
 * <p>该字段只描述引导进度，不影响目标状态机：目标始终是 DRAFT，
 * 用户走完引导或直接手动编排计划都不改变这个事实。
 */
public enum GoalClarificationStage {
  /** 等待用户用自己的话说想达成什么。 */
  AWAITING_GOAL,
  /** 已拿到目标，等待用户确认「怎样算达成」。 */
  AWAITING_CRITERIA,
  /** 已确认达成标准，等待确认首行动。 */
  AWAITING_FIRST_ACTION,
  /** 引导阶段结束；后续走目标详情页的正式计划编排。 */
  COMPLETE
}
