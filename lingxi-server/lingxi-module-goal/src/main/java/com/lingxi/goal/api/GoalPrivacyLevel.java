package com.lingxi.goal.api;

/**
 * 目标隐私级别，默认「仅自己」。
 *
 * <p>该字段只能收紧可见范围，不能单独授予可见性：即使取 {@link #PARTNER_VISIBLE}，
 * 同行伙伴仍然必须存在按目标的最小化授权记录才能看到该目标，避免出现第二套授权事实来源。
 */
public enum GoalPrivacyLevel {
  /** 仅自己可见，为默认值。 */
  PRIVATE,
  /** 允许在存在按目标授权的前提下对同行伙伴可见。 */
  PARTNER_VISIBLE
}
