package com.lingxi.identity.api;

/**
 * AI 主动程度偏好，决定伙伴主动发起提醒与问候的强度。
 *
 * <p>PRD「新手引导 ONB-01 基础画像」规定默认值为「中」。默认值只在服务端兜底，
 * 不写回用户数据：用户没选过就是没选过。
 */
public enum ProactivityLevel {
  /** 低：只在用户主动开口时响应。 */
  LOW,
  /** 中：按行动提醒设置主动跟进，默认值。 */
  MEDIUM,
  /** 高：在免打扰时段之外更积极地发起陪伴与复盘。 */
  HIGH
}
