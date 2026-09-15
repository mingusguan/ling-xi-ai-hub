package com.lingxi.engagement.application;

import java.time.Instant;

/**
 * 通知投递判定结果。
 *
 * <p>需要区分「现在不能发」和「现在不能发、但之后要补发」：命中免打扰时段属于后者。
 * 早先的实现只有布尔值，于是命中免打扰被当成策略拒绝永久取消，用户设置的提醒
 * 到点后直接消失，而不是顺延到免打扰结束。
 *
 * @param allowed 当前时刻是否允许投递
 * @param deferredUntil 被判定为「稍后再发」时的下一个可投递时刻；不允许且无法补发时为 null
 * @param reason 记录在投递流水里的原因码，便于后台区分延后与拒绝
 */
public record DeliveryDecision(boolean allowed, Instant deferredUntil, String reason) {
  /** 渠道未开启等不可补发的场景：该场景本来就不该通过这个渠道触达。 */
  public static final String DENIED_BY_POLICY = "LATEST_POLICY_DENIED";
  /** 命中账号级免打扰时段。 */
  public static final String DEFERRED_ACCOUNT_QUIET_HOURS = "DEFERRED_ACCOUNT_QUIET_HOURS";
  /** 命中引导画像里的免打扰时段。 */
  public static final String DEFERRED_PROFILE_QUIET_HOURS = "DEFERRED_PROFILE_QUIET_HOURS";

  public static DeliveryDecision allow() {
    return new DeliveryDecision(true, null, null);
  }

  /** 现在不能发，也没有可补发的时刻：按策略拒绝处理。 */
  public static DeliveryDecision denied() {
    return new DeliveryDecision(false, null, DENIED_BY_POLICY);
  }

  /** 现在不能发，但稍后可以补发。 */
  public static DeliveryDecision deferredUntil(Instant nextAttempt, String reason) {
    return new DeliveryDecision(false, nextAttempt, reason);
  }

  /** 是否属于「稍后补发」而不是「永久拒绝」。 */
  public boolean isDeferrable() {
    return !allowed && deferredUntil != null;
  }
}
