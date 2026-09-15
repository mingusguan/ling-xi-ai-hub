package com.lingxi.goal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingxi.goal.api.ActionDifficulty;
import com.lingxi.goal.api.MoodLevel;
import com.lingxi.goal.api.PriorityLevel;
import com.lingxi.goal.api.TodayItem;
import com.lingxi.kernel.BusinessException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 今日工作台的排序规则、专注计时与快速记录。
 *
 * <p>这三块共同决定「今天先做什么」，此前一个都没有实现：工作台只按接口返回顺序渲染，
 * 逾期行动取不到，也没有专注计时与随手记录。
 */
class TodayWorkbenchTest {
  private static final Instant T0 = Instant.parse("2026-09-14T01:00:00Z");

  // ---- 专注会话：计时不依赖心跳，暂停区间必须结算 ----

  @Test
  void focusAccumulatesOnlyRunningIntervals() {
    FocusSession session = FocusSession.start(1, 9, null, null, 25, T0);
    // 运行 10 分钟
    session.pause(T0.plusSeconds(600));
    assertThat(session.getAccumulatedSeconds()).isEqualTo(600);
    assertThat(session.currentSeconds(T0.plusSeconds(1200))).isEqualTo(600); // 暂停期间不增长

    session.resume(T0.plusSeconds(1200));
    // 再运行 5 分钟
    int finished = session.finish("done", T0.plusSeconds(1500));
    assertThat(finished).isEqualTo(900);
    assertThat(session.getStatus()).isEqualTo(FocusSessionStatus.FINISHED);
    assertThat(session.activeKey()).isNull();
  }

  /** 重复暂停/恢复是安全空操作，避免多端连点把累计时长算错。 */
  @Test
  void repeatedPauseAndResumeAreIdempotent() {
    FocusSession session = FocusSession.start(1, 9, null, null, null, T0);
    session.pause(T0.plusSeconds(300));
    session.pause(T0.plusSeconds(900));
    assertThat(session.getAccumulatedSeconds()).isEqualTo(300);

    session.resume(T0.plusSeconds(1000));
    session.resume(T0.plusSeconds(2000));
    assertThat(session.getStatus()).isEqualTo(FocusSessionStatus.RUNNING);
    // 只从第一次恢复的时刻起算，第二次恢复不重置起点
    assertThat(session.currentSeconds(T0.plusSeconds(1300))).isEqualTo(600);
  }

  @Test
  void abandonedSessionCannotBeFinished() {
    FocusSession session = FocusSession.start(1, 9, null, null, null, T0);
    session.abandon(T0.plusSeconds(60));
    assertThat(session.activeKey()).isNull();
    assertThatThrownBy(() -> session.finish(null, T0.plusSeconds(120)))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_FOCUS_NOT_ACTIVE");
  }

  @Test
  void focusPlanOutOfRangeRejected() {
    assertThatThrownBy(() -> FocusSession.start(1, 9, null, null, 0, T0))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_INVALID_FOCUS_PLAN");
  }

  // ---- 快速记录 ----

  @Test
  void quickNoteRejectsEmptyContent() {
    assertThatThrownBy(() -> new QuickNote(1, 9, "   ", null, LocalDate.of(2026, 9, 14), null))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GOAL_EMPTY_QUICK_NOTE");
  }

  @Test
  void quickNoteTrimsContentAndKeepsMood() {
    QuickNote note =
        new QuickNote(
            1, 9, "  今天状态不好  ", MoodLevel.LOW, LocalDate.of(2026, 9, 14), null);
    assertThat(note.content()).isEqualTo("今天状态不好");
    assertThat(note.moodLevel()).isEqualTo(MoodLevel.LOW);
  }

  // ---- 工作台排序 ----

  /** 逾期排在最前面：欠账不能被藏起来。 */
  @Test
  void overdueComesBeforeTodayItems() {
    TodayItem overdue = item(1, true, 0, LocalTime.of(23, 0), 10);
    TodayItem today = item(2, false, 0, LocalTime.of(8, 0), 5);
    List<TodayItem> items = sorted(today, overdue);
    assertThat(items).extracting(TodayItem::occurrenceId).containsExactly(1L, 2L);
  }

  /** 同一天内，能解锁更多后续行动的先做。 */
  @Test
  void unlockingMoreComesFirstWithinSameDay() {
    TodayItem blocker = item(1, false, 3, LocalTime.of(20, 0), 60);
    TodayItem leaf = item(2, false, 0, LocalTime.of(8, 0), 5);
    List<TodayItem> items = sorted(leaf, blocker);
    assertThat(items).extracting(TodayItem::occurrenceId).containsExactly(1L, 2L);
  }

  /** 同等条件下按用户设定的行动时刻升序。 */
  @Test
  void earlierLocalTimeComesFirstWhenNothingUnlocksOthers() {
    TodayItem evening = item(1, false, 0, LocalTime.of(21, 0), 5);
    TodayItem morning = item(2, false, 0, LocalTime.of(7, 0), 30);
    List<TodayItem> items = sorted(evening, morning);
    assertThat(items).extracting(TodayItem::occurrenceId).containsExactly(2L, 1L);
  }

  /** 同一时刻时短的先做；未填预计时长排最后，不能当成 0。 */
  @Test
  void shorterEstimatedMinutesFirstAndUnsetGoesLast() {
    TodayItem longOne = item(1, false, 0, LocalTime.of(9, 0), 60);
    TodayItem unset = item(2, false, 0, LocalTime.of(9, 0), null);
    TodayItem shortOne = item(3, false, 0, LocalTime.of(9, 0), 10);
    List<TodayItem> items = sorted(longOne, unset, shortOne);
    assertThat(items).extracting(TodayItem::occurrenceId).containsExactly(3L, 1L, 2L);
  }

  private static List<TodayItem> sorted(TodayItem... items) {
    List<TodayItem> list = new ArrayList<>(List.of(items));
    list.sort(TodayWorkbenchOrder.COMPARATOR);
    return list;
  }

  private static TodayItem item(
      long occurrenceId, boolean overdue, int unlocks, LocalTime localTime, Integer estimated) {
    return new TodayItem(
        occurrenceId,
        occurrenceId * 10,
        occurrenceId * 100,
        "目标",
        "行动 " + occurrenceId,
        null,
        LocalDate.of(2026, 9, 14),
        T0,
        localTime,
        "Asia/Shanghai",
        OccurrenceStatus.SCHEDULED,
        overdue,
        estimated,
        PriorityLevel.NORMAL,
        ActionDifficulty.NORMAL,
        null,
        unlocks,
        null);
  }
}
