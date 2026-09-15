package com.lingxi.kernel;

import java.util.Set;

/**
 * 与伙伴对话有关的用户偏好（来自新手引导画像）。
 *
 * <p>放在 kernel 而不是某个业务模块：画像由身份模块持有，消费方是对话与触达，
 * 直接互相依赖会形成模块循环，因此用 kernel 端口承载这份只读视图。
 *
 * <p>所有字段都保留「用户没选过」的可能：{@code communicationStyle} 为空表示没选，
 * 由消费方决定兜底，而不是在这里替用户假造一个选择。
 *
 * @param nickname 用户自定义称呼；null 表示没填，此时不应使用任何称呼
 * @param communicationStyle 沟通风格；null 表示没选
 * @param proactivityLevel 主动程度；null 表示没选
 * @param commonBlockers 常见阻塞原因；空集合表示没选
 */
public record CompanionPreference(
    String nickname,
    CommunicationPreferenceStyle communicationStyle,
    ProactivityPreference proactivityLevel,
    Set<CommonBlockerReason> commonBlockers) {

  public CompanionPreference {
    commonBlockers = commonBlockers == null ? Set.of() : Set.copyOf(commonBlockers);
  }

  /** 用户什么都没填时的空偏好。 */
  public static CompanionPreference empty() {
    return new CompanionPreference(null, null, null, Set.of());
  }

  /** 沟通风格（与身份模块的取值一致，避免消费方再依赖身份模块的枚举）。 */
  public enum CommunicationPreferenceStyle {
    // 简洁：直接给结论与下一步，少铺垫
    CONCISE,
    // 温和：先接住情绪，再给建议
    GENTLE,
    // 直接：就事论事，不做情绪缓冲
    DIRECT,
    // 教练式：用提问帮用户自己得出结论
    COACHING
  }

  /** 主动程度，决定伙伴发起陪伴与跟进的强度。 */
  public enum ProactivityPreference {
    // 低：只在用户主动开口时响应
    LOW,
    // 中：按行动提醒主动跟进
    MEDIUM,
    // 高：在免打扰时段之外更主动地陪伴与复盘
    HIGH
  }

  /** 常见阻塞原因。 */
  public enum CommonBlockerReason {
    // 时间不够
    TIME,
    // 精力不足
    ENERGY,
    // 不知道具体该做什么
    CLARITY,
    // 知道怎么做但不想开始
    MOTIVATION,
    // 缺少场地或设备
    ENVIRONMENT,
    // 情绪状态影响执行
    MOOD,
    // 其他
    OTHER
  }
}
