package com.lingxi.identity.api;

import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.QuietHoursWindow;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * 新手引导得到的基础画像值对象。
 *
 * <p>承载 PRD「ONB-01 基础画像」全部字段：称呼、时区之外可选的作息与每周可用时间、
 * 提醒时段与免打扰时段、AI 沟通风格、主动程度、常见阻塞原因。
 *
 * <p>所有字段都可为空：PRD 明确要求该步骤可跳过，且禁止强制收集真实姓名、身份证、
 * 职业单位、精确位置等完成核心功能非必需的信息。称呼是用户自定义昵称，不做实名校验。
 */
public record OnboardingProfile(
    String nickname,
    LocalTime sleepTime,
    LocalTime wakeTime,
    Integer weeklyAvailableMinutes,
    LocalTime remindWindowStart,
    LocalTime remindWindowEnd,
    LocalTime quietHoursStart,
    LocalTime quietHoursEnd,
    CommunicationStyle communicationStyle,
    ProactivityLevel proactivityLevel,
    Set<CommonBlocker> commonBlockers) {
  /** 称呼长度上界；超出视为误填，避免异常长文本进入画像与提示词。 */
  private static final int MAX_NICKNAME_LENGTH = 24;
  /** 每周可用时间上界：一周总分钟数。与目标级同名字段保持一致的量纲。 */
  private static final int MAX_WEEKLY_MINUTES = 7 * 24 * 60;

  public OnboardingProfile {
    nickname = normalizeNickname(nickname);
    commonBlockers =
        commonBlockers == null
            ? Set.of()
            : Set.copyOf(new LinkedHashSet<>(commonBlockers));
    if (weeklyAvailableMinutes != null
        && (weeklyAvailableMinutes <= 0 || weeklyAvailableMinutes > MAX_WEEKLY_MINUTES)) {
      throw new BusinessException("IDENTITY_INVALID_AVAILABLE_TIME", "每周可用时间超出合理范围");
    }
    if (remindWindowStart != null ^ remindWindowEnd != null) {
      throw new BusinessException(
          "IDENTITY_INVALID_REMIND_WINDOW", "提醒时段的开始与结束必须同时填写");
    }
    if (remindWindowStart != null && !remindWindowEnd.isAfter(remindWindowStart)) {
      throw new BusinessException("IDENTITY_INVALID_REMIND_WINDOW", "提醒时段结束时间必须晚于开始时间");
    }
    if (quietHoursStart != null ^ quietHoursEnd != null) {
      throw new BusinessException(
          "IDENTITY_INVALID_QUIET_HOURS", "免打扰时段的开始与结束必须同时填写");
    }
    // 免打扰允许跨天（例如 23:00 到次日 07:00），因此这里只拒绝起止完全相同。
    if (quietHoursStart != null && quietHoursStart.equals(quietHoursEnd)) {
      throw new BusinessException("IDENTITY_INVALID_QUIET_HOURS", "免打扰时段的开始与结束不能相同");
    }
  }

  /** 尚未完成引导时的空画像；读取接口据此返回 null 而非报错。 */
  public static OnboardingProfile empty() {
    return new OnboardingProfile(
        null, null, null, null, null, null, null, null, null, null, Set.of());
  }

  /** 引导是否已经产生过任何用户输入，用于前端判断是否首次进入。 */
  public boolean isEmpty() {
    return nickname == null
        && sleepTime == null
        && wakeTime == null
        && weeklyAvailableMinutes == null
        && remindWindowStart == null
        && quietHoursStart == null
        && communicationStyle == null
        && proactivityLevel == null
        && commonBlockers.isEmpty();
  }

  /** 生效的沟通风格：用户未选择时按简洁处理。 */
  public CommunicationStyle effectiveCommunicationStyle() {
    return communicationStyle == null ? CommunicationStyle.CONCISE : communicationStyle;
  }

  /** 生效的主动程度：用户未选择时按「中」处理。 */
  public ProactivityLevel effectiveProactivityLevel() {
    return proactivityLevel == null ? ProactivityLevel.MEDIUM : proactivityLevel;
  }

  /**
   * 判断某个时刻（按给定时区换算成当地时间）是否落在免打扰时段内。
   *
   * <p>支持跨天区间；未设置免打扰时永远返回 false。
   */
  public boolean isWithinQuietHours(LocalTime time) {
    if (quietHoursStart == null || quietHoursEnd == null || time == null) {
      return false;
    }
    // 判定规则与通知投递共用 QuietHoursWindow，避免两处各写一份跨天边界判断。
    return new QuietHoursWindow(quietHoursStart, quietHoursEnd, ZoneOffset.UTC)
        .containsLocalTime(time);
  }

  /**
   * 免打扰时段结束、可以再次打扰用户的时刻。
   *
   * <p>通知投递在「用户没有单独配置通知偏好」时回退到这份画像，因此需要能算出
   * 顺延到什么时候。返回 null 表示当前不在免打扰时段内。
   */
  public Instant quietHoursEndAt(Instant instant, ZoneId zone) {
    if (quietHoursStart == null || quietHoursEnd == null) {
      return null;
    }
    return new QuietHoursWindow(quietHoursStart, quietHoursEnd, zone).endsAt(instant);
  }

  /** 阻塞原因按枚举声明顺序输出，保证相同集合的序列化结果稳定。 */
  public Set<CommonBlocker> orderedBlockers() {
    return new TreeSet<>(commonBlockers);
  }

  private static String normalizeNickname(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.length() > MAX_NICKNAME_LENGTH) {
      throw new BusinessException("IDENTITY_INVALID_NICKNAME", "称呼长度超出限制");
    }
    return trimmed;
  }
}
