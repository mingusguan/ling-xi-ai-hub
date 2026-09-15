package com.lingxi.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * 免打扰时段判定与顺延时刻计算。
 *
 * <p>这类边界最容易写错又最难从现象反推：跨天区间（22:00 到次日 07:00）如果判定写反，
 * 表现是「该安静的时候反而打扰用户」或「该发的提醒不发」，两者都不会报错。
 *
 * <p>kernel 模块只有 JUnit、没有 AssertJ，因此本用例使用 JUnit 原生断言。
 */
class QuietHoursWindowTest {
  private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

  /** 上海时间 2026-09-15 23:30。 */
  private static final Instant LATE_NIGHT = Instant.parse("2026-09-15T15:30:00Z");
  /** 上海时间 2026-09-15 06:30。 */
  private static final Instant EARLY_MORNING = Instant.parse("2026-09-14T22:30:00Z");
  /** 上海时间 2026-09-15 12:00。 */
  private static final Instant NOON = Instant.parse("2026-09-15T04:00:00Z");

  private QuietHoursWindow overnight() {
    return new QuietHoursWindow(LocalTime.of(22, 0), LocalTime.of(7, 0), SHANGHAI);
  }

  private QuietHoursWindow daytime() {
    return new QuietHoursWindow(LocalTime.of(13, 0), LocalTime.of(14, 0), SHANGHAI);
  }

  @Test
  void overnightWindowCoversBothSidesOfMidnight() {
    assertTrue(overnight().contains(LATE_NIGHT));
    assertTrue(overnight().contains(EARLY_MORNING));
    assertFalse(overnight().contains(NOON));
  }

  @Test
  void daytimeWindowEndsOnTheSameDay() {
    Instant inside = Instant.parse("2026-09-15T05:30:00Z");
    assertTrue(daytime().contains(inside));
    assertEquals(Instant.parse("2026-09-15T06:00:00Z"), daytime().endsAt(inside));
  }

  @Test
  void overnightWindowEndsOnTheNextDay() {
    // 深夜 23:30 命中跨天区间，顺延目标是「次日 07:00」，不是当天 07:00（那已经过去了）。
    assertEquals(Instant.parse("2026-09-15T23:00:00Z"), overnight().endsAt(LATE_NIGHT));
  }

  @Test
  void morningSideOfOvernightWindowEndsOnTheSameDay() {
    // 06:30 仍在区间内，顺延目标就是当天 07:00（上海 07:00 = UTC 前一天 23:00）。
    assertEquals(Instant.parse("2026-09-14T23:00:00Z"), overnight().endsAt(EARLY_MORNING));
  }

  @Test
  void outsideTheWindowThereIsNothingToDeferTo() {
    assertNull(overnight().endsAt(NOON));
  }

  @Test
  void boundaryMinutesBelongToTheWindow() {
    // 开始时刻算在区间内、结束时刻算在区间外，否则会出现「最后一个静默分钟被打破」。
    assertTrue(overnight().containsLocalTime(LocalTime.of(22, 0)));
    assertFalse(overnight().containsLocalTime(LocalTime.of(7, 0)));
    assertTrue(overnight().containsLocalTime(LocalTime.of(6, 59)));
  }

  @Test
  void identicalStartAndEndIsRejected() {
    BusinessException thrown =
        assertThrows(
            BusinessException.class,
            () -> new QuietHoursWindow(LocalTime.of(8, 0), LocalTime.of(8, 0), SHANGHAI));
    assertEquals("KERNEL_INVALID_QUIET_HOURS", thrown.getCode());
  }
}
