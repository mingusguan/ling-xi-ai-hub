package com.lingxi.goal.domain;

import com.lingxi.kernel.BusinessException;
import java.time.Duration;
import java.time.Instant;

/**
 * 专注会话聚合。
 *
 * <p>计时只记录「每个运行区间的起点」与「已累计秒数」两个量，暂停时把当前区间结算进累计值，
 * 因此累计时长不依赖任何后台心跳：即使进程重启，累计值也不会丢，恢复后从新的起点继续。
 *
 * <p>一个用户同时只允许一个进行中的会话，由 {@code uk_goal_focus_active} 唯一键兜底。
 */
public class FocusSession {
  /** 单次专注的时长上界：24 小时，超过视为忘记结束，避免污染统计。 */
  private static final long MAX_ACCUMULATED_SECONDS = 24 * 3600L;

  private final long id;
  private final long userId;
  private final Long occurrenceId;
  private final Long actionId;
  private final Integer plannedMinutes;
  private FocusSessionStatus status;
  private int accumulatedSeconds;
  private Instant lastResumedAt;
  private final Instant startedAt;
  private Instant endedAt;
  private String note;
  private long version;
  private final Instant createdAt;
  private Instant updatedAt;

  private FocusSession(
      long id,
      long userId,
      Long occurrenceId,
      Long actionId,
      Integer plannedMinutes,
      FocusSessionStatus status,
      int accumulatedSeconds,
      Instant lastResumedAt,
      Instant startedAt,
      Instant endedAt,
      String note,
      long version,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.userId = userId;
    this.occurrenceId = occurrenceId;
    this.actionId = actionId;
    this.plannedMinutes = plannedMinutes;
    this.status = status;
    this.accumulatedSeconds = accumulatedSeconds;
    this.lastResumedAt = lastResumedAt;
    this.startedAt = startedAt;
    this.endedAt = endedAt;
    this.note = note;
    this.version = version;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** 开始一次专注；可选关联到某个行动实例。 */
  public static FocusSession start(
      long id, long userId, Long occurrenceId, Long actionId, Integer plannedMinutes, Instant now) {
    if (id <= 0 || userId <= 0) {
      throw new BusinessException("GOAL_INVALID_FOCUS_SESSION", "专注会话标识不合法");
    }
    if (plannedMinutes != null && (plannedMinutes <= 0 || plannedMinutes > 24 * 60)) {
      throw new BusinessException("GOAL_INVALID_FOCUS_PLAN", "计划专注时长超出合理范围");
    }
    return new FocusSession(
        id,
        userId,
        occurrenceId,
        actionId,
        plannedMinutes,
        FocusSessionStatus.RUNNING,
        0,
        now,
        now,
        null,
        null,
        0,
        now,
        now);
  }

  public static FocusSession rehydrate(
      long id,
      long userId,
      Long occurrenceId,
      Long actionId,
      Integer plannedMinutes,
      FocusSessionStatus status,
      int accumulatedSeconds,
      Instant lastResumedAt,
      Instant startedAt,
      Instant endedAt,
      String note,
      long version,
      Instant createdAt,
      Instant updatedAt) {
    return new FocusSession(
        id,
        userId,
        occurrenceId,
        actionId,
        plannedMinutes,
        status,
        accumulatedSeconds,
        lastResumedAt,
        startedAt,
        endedAt,
        note,
        version,
        createdAt,
        updatedAt);
  }

  /** 暂停计时，把当前运行区间结算进累计秒数。已暂停时是安全的空操作。 */
  public void pause(Instant now) {
    if (status == FocusSessionStatus.PAUSED) {
      return;
    }
    requireRunning("GOAL_FOCUS_NOT_RUNNING", "只有计时中的专注会话可以暂停");
    accumulatedSeconds = clamp(accumulatedSeconds + elapsedSeconds(now));
    lastResumedAt = null;
    status = FocusSessionStatus.PAUSED;
    version++;
    updatedAt = now;
  }

  /** 恢复计时。计时中时是安全的空操作。 */
  public void resume(Instant now) {
    if (status == FocusSessionStatus.RUNNING) {
      return;
    }
    if (status != FocusSessionStatus.PAUSED) {
      throw new BusinessException("GOAL_FOCUS_NOT_PAUSED", "只有已暂停的专注会话可以恢复");
    }
    lastResumedAt = now;
    status = FocusSessionStatus.RUNNING;
    version++;
    updatedAt = now;
  }

  /** 结束并保留为一次专注记录；返回本次的实际专注秒数。 */
  public int finish(String note, Instant now) {
    if (status == FocusSessionStatus.FINISHED) {
      return accumulatedSeconds;
    }
    if (status != FocusSessionStatus.RUNNING && status != FocusSessionStatus.PAUSED) {
      throw new BusinessException("GOAL_FOCUS_NOT_ACTIVE", "只有进行中的专注会话可以结束");
    }
    if (status == FocusSessionStatus.RUNNING) {
      accumulatedSeconds = clamp(accumulatedSeconds + elapsedSeconds(now));
    }
    lastResumedAt = null;
    status = FocusSessionStatus.FINISHED;
    endedAt = now;
    this.note = note == null || note.isBlank() ? null : note.trim();
    version++;
    updatedAt = now;
    return accumulatedSeconds;
  }

  /** 作废本次专注；已产生的时长不计入统计。 */
  public void abandon(Instant now) {
    if (status == FocusSessionStatus.ABANDONED) {
      return;
    }
    status = FocusSessionStatus.ABANDONED;
    lastResumedAt = null;
    endedAt = now;
    version++;
    updatedAt = now;
  }

  /** 当前累计秒数；计时中时把正在进行的区间也算进去，供界面实时展示。 */
  public int currentSeconds(Instant now) {
    return status == FocusSessionStatus.RUNNING
        ? clamp(accumulatedSeconds + elapsedSeconds(now))
        : accumulatedSeconds;
  }

  public boolean active() {
    return status == FocusSessionStatus.RUNNING || status == FocusSessionStatus.PAUSED;
  }

  private void requireRunning(String code, String message) {
    if (status != FocusSessionStatus.RUNNING) {
      throw new BusinessException(code, message);
    }
  }

  private long elapsedSeconds(Instant now) {
    if (lastResumedAt == null) {
      return 0;
    }
    long seconds = Duration.between(lastResumedAt, now).getSeconds();
    return Math.max(seconds, 0);
  }

  private static int clamp(long seconds) {
    return (int) Math.min(seconds, MAX_ACCUMULATED_SECONDS);
  }

  public long getId() {
    return id;
  }

  public long getUserId() {
    return userId;
  }

  public Long getOccurrenceId() {
    return occurrenceId;
  }

  public Long getActionId() {
    return actionId;
  }

  public Integer getPlannedMinutes() {
    return plannedMinutes;
  }

  public FocusSessionStatus getStatus() {
    return status;
  }

  public int getAccumulatedSeconds() {
    return accumulatedSeconds;
  }

  public Instant getLastResumedAt() {
    return lastResumedAt;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public String getNote() {
    return note;
  }

  public long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /** 进行中标记；结束或作废后置空，配合唯一键保证同时只有一个进行中会话。 */
  public Integer activeKey() {
    return active() ? 1 : null;
  }
}
