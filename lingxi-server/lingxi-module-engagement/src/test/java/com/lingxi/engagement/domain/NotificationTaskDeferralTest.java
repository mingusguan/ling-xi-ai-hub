package com.lingxi.engagement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingxi.engagement.api.NotificationChannel;
import com.lingxi.engagement.api.NotificationTaskStatus;
import com.lingxi.engagement.api.ScheduleNotificationCommand;
import com.lingxi.kernel.BusinessException;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * 通知任务因免打扰顺延的行为。
 *
 * <p>回归背景：早先命中免打扰时任务被直接取消，用户设置的提醒到点后凭空消失。
 * 产品语义应当是「换个时间再发」，因此这里固定住三件事：顺延后回到待投递、
 * 记录顺延目标时刻、顺延次数有上限。
 */
class NotificationTaskDeferralTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 15, 16, 0);

  private NotificationTask sendingTask() {
    NotificationTask task =
        NotificationTask.schedule(
            1L,
            new ScheduleNotificationCommand(
                "dedupe-1",
                1L,
                NotificationChannel.PUSH,
                "ACTION_REMINDER",
                "goal_occurrence",
                "99",
                "{}",
                Instant.parse("2026-09-15T14:00:00Z")),
            NOW);
    task.start(NOW);
    return task;
  }

  @Test
  void deferralReturnsTheTaskToPendingAndRecordsTheTargetInstant() {
    NotificationTask task = sendingTask();
    Instant until = Instant.parse("2026-09-15T23:00:00Z");

    assertThat(task.deferredTo(until, 3, NOW)).isTrue();

    assertThat(task.getStatus()).isEqualTo(NotificationTaskStatus.PENDING);
    assertThat(task.getDeferredUntil()).isEqualTo(until);
    assertThat(task.getLastError()).isEqualTo(NotificationTask.DEFERRED_REASON);
    assertThat(task.getAttemptCount()).isEqualTo(1);
  }

  @Test
  void deferralIsRefusedOnceTheLimitIsReached() {
    NotificationTask task = sendingTask();
    Instant until = Instant.parse("2026-09-15T23:00:00Z");

    assertThat(task.deferredTo(until, 1, NOW)).isTrue();
    task.start(NOW);

    assertThat(task.deferredTo(until, 1, NOW)).isFalse();
  }

  @Test
  void aTargetInstantInThePastIsNotAccepted() {
    NotificationTask task = sendingTask();

    assertThat(task.deferredTo(Instant.parse("2020-01-01T00:00:00Z"), 3, NOW)).isFalse();
    assertThat(task.deferredTo(null, 3, NOW)).isFalse();
    // 未接受顺延就不应改变状态，否则任务会卡在待投递却永远不会被再次领取。
    assertThat(task.getStatus()).isEqualTo(NotificationTaskStatus.SENDING);
  }

  @Test
  void onlyASendingTaskCanBeDeferred() {
    NotificationTask task = sendingTask();
    task.sent(NOW);

    assertThatThrownBy(() -> task.deferredTo(Instant.parse("2026-09-15T23:00:00Z"), 3, NOW))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "ENG_TASK_NOT_SENDING");
  }

  @Test
  void newTasksHaveNoDeferralRecord() {
    NotificationTask task = sendingTask();
    assertThat(task.getDeferredUntil()).isNull();
  }
}
