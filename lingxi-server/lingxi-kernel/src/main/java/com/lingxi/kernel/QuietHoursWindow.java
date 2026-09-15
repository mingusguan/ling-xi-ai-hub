package com.lingxi.kernel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 免打扰时段值对象，支持跨天区间。
 *
 * <p>放在 kernel 是因为同一套「是不是在免打扰时段内、时段什么时候结束」的判定
 * 同时被账号级通知偏好与引导画像使用。两处各写一遍会漂移：跨天边界的判断
 * 很容易其中一处写错，而错误表现是「有时打扰了用户、有时又漏发提醒」，
 * 很难从现象反推到具体那一处。
 *
 * @param start 免打扰开始时刻（本地时间）
 * @param end 免打扰结束时刻（本地时间）；早于或等于 {@code start} 时表示跨天
 * @param zone 判定所用时区
 */
public record QuietHoursWindow(LocalTime start, LocalTime end, ZoneId zone) {

  public QuietHoursWindow {
    if (start == null || end == null || zone == null) {
      throw new BusinessException("KERNEL_INVALID_QUIET_HOURS", "免打扰时段参数不完整");
    }
    if (start.equals(end)) {
      throw new BusinessException("KERNEL_INVALID_QUIET_HOURS", "免打扰时段的开始与结束不能相同");
    }
  }

  /** 给定时刻是否落在免打扰时段内。 */
  public boolean contains(Instant instant) {
    return containsLocalTime(instant.atZone(zone).toLocalTime());
  }

  /**
   * 给定的当地时间是否落在免打扰时段内。
   *
   * <p>单独暴露这个方法，是因为引导画像只保存「当地时间」这一层语义
   * （用户填写的就是晚上 11 点到早上 7 点），不需要也没有时区上下文；
   * 硬造一个 Instant 去做判定会把「本地时间」和「绝对时刻」两件事混在一起。
   */
  public boolean containsLocalTime(LocalTime localTime) {
    if (localTime == null) {
      return false;
    }
    return start.isBefore(end)
        ? !localTime.isBefore(start) && localTime.isBefore(end)
        : !localTime.isBefore(start) || localTime.isBefore(end);
  }

  /**
   * 免打扰时段结束、可以再次打扰用户的时刻。
   *
   * <p>不在时段内时返回 null。
   *
   * <p>结束时刻落在哪一天取决于当前位于区间的哪一半：跨天区间（22:00 到次日 07:00）里，
   * 深夜 23:30 的顺延目标是「次日 07:00」，而清晨 06:30 的顺延目标就是「当天 07:00」。
   * 只看「结束是否晚于开始」会把清晨那一半整整推后一天，让提醒凭空晚到 24 小时。
   */
  public Instant endsAt(Instant instant) {
    if (!contains(instant)) {
      return null;
    }
    LocalTime local = instant.atZone(zone).toLocalTime();
    LocalDate localDate = instant.atZone(zone).toLocalDate();
    boolean crossesMidnight = !end.isAfter(start);
    // 跨天时，只有已经越过开始时刻（深夜那一段）才需要顺延到次日。
    LocalDate endDate = crossesMidnight && !local.isBefore(start) ? localDate.plusDays(1) : localDate;
    return endDate.atTime(end).atZone(zone).toInstant();
  }
}
