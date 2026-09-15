package com.lingxi.identity.api;

/**
 * AI 沟通风格偏好。
 *
 * <p>PRD「新手引导 ONB-01 基础画像」规定的四种风格；只描述表达方式，不代表模型能力差异。
 * 未设置时按 {@link #CONCISE} 处理，属于服务端默认值而非用户显式选择。
 */
public enum CommunicationStyle {
  /** 简洁：直接给结论与下一步，少铺垫。 */
  CONCISE,
  /** 温和：先共情再给建议，措辞柔和。 */
  GENTLE,
  /** 直接：就事论事，不做情绪缓冲。 */
  DIRECT,
  /** 教练式：以提问推动用户自己得出结论。 */
  COACHING
}
