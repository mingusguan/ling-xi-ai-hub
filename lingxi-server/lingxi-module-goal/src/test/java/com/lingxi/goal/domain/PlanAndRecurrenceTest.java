package com.lingxi.goal.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.goal.api.ActionDifficulty;
import com.lingxi.goal.api.ActionExceptionType;
import com.lingxi.goal.api.CheckInResultType;
import com.lingxi.goal.api.EnergyLevel;
import com.lingxi.goal.api.MoodLevel;
import com.lingxi.goal.api.RecurrenceType;
import com.lingxi.kernel.BusinessException;
import java.time.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanAndRecurrenceTest {
  @Test
  void draftActivationCreatesNewImmutableValue() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    PlanVersion draft = PlanVersion.draft(1, 2, 1, "key", "digest", "{}", null, "AGENT", now);
    PlanVersion active = draft.activate(now.plusMinutes(1));
    assertThat(draft.status()).isEqualTo(PlanVersionStatus.PENDING_CONFIRMATION);
    assertThat(draft.activatedAt()).isNull();
    assertThat(active.status()).isEqualTo(PlanVersionStatus.ACTIVE);
  }

  @Test
  void daylightSavingGapResolvesToSingleValidInstant() {
    Action action =
        new Action(
            1,
            2,
            3,
            null,
            "client",
            "晨间行动",
            new ActionSchedule(
                RecurrenceType.ONCE,
                Set.of(),
                null,
                LocalDate.of(2026, 3, 8),
                null,
                LocalTime.of(2, 30),
                null,
                "America/New_York"),
            ActionDetail.defaults(),
            ActionStatus.ACTIVE,
            0,
            LocalDateTime.now(),
            LocalDateTime.now());
    ZonedDateTime resolved =
        action.startDate().atTime(action.localTime()).atZone(ZoneId.of(action.timezone()));
    assertThat(resolved.toLocalTime()).isEqualTo(LocalTime.of(3, 30));
    assertThat(resolved.toInstant()).isEqualTo(Instant.parse("2026-03-08T07:30:00Z"));
  }

  @Test
  void correctionKeepsHistoricalCheckInAndChangesOccurrenceResult() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    ActionOccurrence occurrence =
        ActionOccurrence.schedule(
            1,
            2,
            Instant.parse("2026-08-05T02:00:00Z"),
            LocalDate.of(2026, 8, 5),
            "Asia/Shanghai",
            now);
    CheckIn original =
        CheckIn.record(
            3,
            1,
            9,
            "first",
            "d1",
            CheckInResultType.PARTIAL,
            CheckInDetail.minimal(null, null),
            now);
    original.supersede();
    occurrence.checkIn(CheckInResultType.COMPLETED, now.plusMinutes(1));
    CheckIn correction =
        CheckIn.record(
            4,
            1,
            9,
            "second",
            "d2",
            CheckInResultType.COMPLETED,
            CheckInDetail.minimal(null, null),
            now.plusMinutes(1));
    assertThat(original.isEffective()).isFalse();
    assertThat(correction.isEffective()).isTrue();
    assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.COMPLETED);
  }

  @Test
  void completedReviewIsIdempotentButCannotBeOverwritten() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    Review review = Review.schedule(1, 2, "2026-W32", "{}", now);
    review.complete("key", "digest", "{\"done\":true}", now);
    review.complete("key", "digest", "{\"done\":true}", now);
    assertThat(review.getVersion()).isEqualTo(1);
    assertThatThrownBy(() -> review.complete("other", "other", "{}", now))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_REVIEW_ALREADY_COMPLETED");
  }

  @Test
  void weeklyActionRequiresWeekdays() {
    assertThatThrownBy(() -> action(RecurrenceType.WEEKLY, Set.of(), null, LocalDate.now()))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_WEEKDAYS_REQUIRED");
  }

  /** 工作日规则只需落在周一至周五，不依赖星期集合。 */
  @Test
  void weekdaysRuleOccursFromMondayToFridayOnly() {
    Action action = action(RecurrenceType.WEEKDAYS, Set.of(), null, LocalDate.of(2026, 9, 1));
    assertThat(action.occursOn(LocalDate.of(2026, 9, 1))).isTrue(); // 周二
    assertThat(action.occursOn(LocalDate.of(2026, 9, 4))).isTrue(); // 周五
    assertThat(action.occursOn(LocalDate.of(2026, 9, 5))).isFalse(); // 周六
    assertThat(action.occursOn(LocalDate.of(2026, 9, 6))).isFalse(); // 周日
    assertThat(action.occursOn(LocalDate.of(2026, 8, 31))).isFalse(); // 早于开始日期
  }

  /** 间隔重复以开始日期为锚点，按自然日等差落点。 */
  @Test
  void intervalRuleAnchorsOnStartDate() {
    Action action = action(RecurrenceType.INTERVAL, Set.of(), 3, LocalDate.of(2026, 9, 1));
    assertThat(action.occursOn(LocalDate.of(2026, 9, 1))).isTrue();
    assertThat(action.occursOn(LocalDate.of(2026, 9, 2))).isFalse();
    assertThat(action.occursOn(LocalDate.of(2026, 9, 4))).isTrue();
    assertThat(action.occursOn(LocalDate.of(2026, 9, 7))).isTrue();
  }

  /** 间隔重复缺少间隔天数时必须拒绝，避免退化成不可预期的默认规则。 */
  @Test
  void intervalRuleRequiresIntervalDays() {
    assertThatThrownBy(() -> action(RecurrenceType.INTERVAL, Set.of(), null, LocalDate.now()))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_INTERVAL_REQUIRED");
  }

  /** 非间隔重复不允许残留间隔天数，防止仓储与规则判定出现两套事实。 */
  @Test
  void nonIntervalRuleDropsIntervalDays() {
    Action action = action(RecurrenceType.DAILY, Set.of(), 5, LocalDate.of(2026, 9, 1));
    assertThat(action.intervalDays()).isNull();
  }

  private static Action action(
      RecurrenceType recurrenceType, Set<DayOfWeek> weekdays, Integer intervalDays, LocalDate startDate) {
    return new Action(
        1,
        2,
        3,
        null,
        "client",
        "行动",
        new ActionSchedule(
            recurrenceType, weekdays, intervalDays, startDate, null, LocalTime.NOON, null, "Asia/Shanghai"),
        ActionDetail.defaults(),
        ActionStatus.ACTIVE,
        0,
        LocalDateTime.now(),
        LocalDateTime.now());
  }

  /** 单次调整不改写行动定义：改期只登记例外，重复规则本身保持不变。 */
  @Test
  void rescheduleExceptionKeepsOriginalRecurrenceRule() {
    ActionException exception =
        ActionException.reschedule(
            9, 7, LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 19), "临时出差", LocalDateTime.now());
    assertThat(exception.type()).isEqualTo(ActionExceptionType.RESCHEDULE);
    assertThat(exception.localDate()).isEqualTo(LocalDate.of(2026, 9, 17));
    assertThat(exception.rescheduledDate()).isEqualTo(LocalDate.of(2026, 9, 19));

    Action original = action(RecurrenceType.WEEKDAYS, Set.of(), null, LocalDate.of(2026, 9, 14));
    // 规则本身没有被改动，被改期的那一天仍按规则成立；由生成器叠加例外后跳过原日期。
    assertThat(original.occursOn(LocalDate.of(2026, 9, 17))).isTrue();
  }

  /** 跳过例外不允许携带改期目标日期，否则同一条例外会有两种互相矛盾的语义。 */
  @Test
  void skipExceptionRejectsTargetDate() {
    assertThatThrownBy(
            () ->
                new ActionException(
                    9,
                    7,
                    LocalDate.of(2026, 9, 17),
                    ActionExceptionType.SKIP,
                    LocalDate.of(2026, 9, 19),
                    null,
                    LocalDateTime.now()))
        .isInstanceOf(BusinessException.class);
  }

  /** 改期目标不能与原日期相同，否则等于没改但语义上又占了一条例外。 */
  @Test
  void rescheduleExceptionRejectsSameDate() {
    assertThatThrownBy(
            () -> ActionException.reschedule(9, 7, LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 17), null, LocalDateTime.now()))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_INVALID_ACTION_EXCEPTION");
  }

  /** 编辑行动以「本次及未来」整体替换定义，版本号递增。 */
  @Test
  void editActionReplacesDefinitionAndAdvancesVersion() {
    Action original = action(RecurrenceType.DAILY, Set.of(), null, LocalDate.of(2026, 9, 14));
    Action edited =
        original.edited(
            "改名后的行动",
            new ActionSchedule(
                RecurrenceType.WEEKDAYS,
                Set.of(),
                null,
                LocalDate.of(2026, 9, 14),
                null,
                LocalTime.of(7, 0),
                LocalTime.of(7, 30),
                "Asia/Shanghai"),
            new ActionDetail(null, null, null, "读完一章", 30, null, null, false),
            LocalDateTime.now());
    assertThat(edited.title()).isEqualTo("改名后的行动");
    assertThat(edited.recurrenceType()).isEqualTo(RecurrenceType.WEEKDAYS);
    assertThat(edited.occursOn(LocalDate.of(2026, 9, 19))).isFalse(); // 周六
    assertThat(edited.version()).isEqualTo(original.version() + 1);
    assertThat(edited.detail().completionCriteria()).isEqualTo("读完一章");
  }

  /** 已取消的行动不能再编辑，避免取消后又被改回可执行状态。 */
  @Test
  void cancelledActionCannotBeEdited() {
    Action cancelled = action(RecurrenceType.DAILY, Set.of(), null, LocalDate.of(2026, 9, 14)).cancelled(LocalDateTime.now());
    assertThatThrownBy(
            () ->
                cancelled.edited(
                    "改名", cancelled.schedule(), ActionDetail.defaults(), LocalDateTime.now()))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_ACTION_NOT_EDITABLE");
  }

  /** 记录失败必须给出原因，否则复盘拿不到可用的阻塞信息。 */
  @Test
  void failedCheckInRequiresReason() {
    assertThatThrownBy(
            () ->
                CheckIn.record(
                    1,
                    2,
                    3,
                    "key",
                    "digest",
                    CheckInResultType.FAILED,
                    CheckInDetail.minimal("尝试了但没完成", null),
                    LocalDateTime.now()))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_FAILURE_REASON_REQUIRED");
  }

  /** 非失败结果不允许携带失败原因，否则同一条记录会有两种互相矛盾的含义。 */
  @Test
  void nonFailedCheckInRejectsFailureReason() {
    assertThatThrownBy(
            () ->
                CheckIn.record(
                    1,
                    2,
                    3,
                    "key",
                    "digest",
                    CheckInResultType.COMPLETED,
                    new CheckInDetail(null, null, null, null, null, null, "不该出现的原因"),
                    LocalDateTime.now()))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_UNEXPECTED_FAILURE_REASON");
  }

  /** 打卡维度按填写内容完整保留，并映射为对应的实例状态。 */
  @Test
  void checkInKeepsRecordedDimensionsAndMapsStatus() {
    CheckInDetail detail =
        new CheckInDetail(
            "读得比预期慢",
            "file-123",
            45,
            ActionDifficulty.HARD,
            EnergyLevel.LOW,
            MoodLevel.NEUTRAL,
            null);
    CheckIn checkIn =
        CheckIn.record(
            1, 2, 3, "key", "digest", CheckInResultType.PARTIAL, detail, LocalDateTime.now());
    assertThat(checkIn.getDetail().actualMinutes()).isEqualTo(45);
    assertThat(checkIn.getDetail().perceivedDifficulty())
        .isEqualTo(ActionDifficulty.HARD);
    assertThat(checkIn.getDetail().energyLevel()).isEqualTo(EnergyLevel.LOW);
    assertThat(checkIn.getDetail().moodLevel()).isEqualTo(MoodLevel.NEUTRAL);
    assertThat(checkIn.getEvidenceReference()).isEqualTo("file-123");

    ActionOccurrence occurrence =
        ActionOccurrence.schedule(
            9, 2, java.time.Instant.parse("2026-09-14T13:00:00Z"), LocalDate.of(2026, 9, 14), "Asia/Shanghai", LocalDateTime.now());
    occurrence.checkIn(CheckInResultType.FAILED, LocalDateTime.now());
    assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.FAILED);
  }

  /** 实际耗时越界必须拒绝，避免把误填的小时当成分钟写进复盘输入。 */
  @Test
  void actualMinutesOutOfRangeRejected() {
    assertThatThrownBy(
            () -> new CheckInDetail(null, null, 0, null, null, null, null))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_INVALID_ACTUAL_MINUTES");
  }
}
