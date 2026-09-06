package com.lingxi.goal.domain;

import com.lingxi.goal.api.CheckInResultType;
import com.lingxi.kernel.BusinessException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** 一次行动发生实例。 */
public class ActionOccurrence {
  private final long id;
  private final long actionId;
  private final Instant scheduledAt;
  private final LocalDate localDate;
  private final String timezone;
  private OccurrenceStatus status;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private ActionOccurrence(
      long id,
      long actionId,
      Instant scheduledAt,
      LocalDate localDate,
      String timezone,
      OccurrenceStatus status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.actionId = actionId;
    this.scheduledAt = Objects.requireNonNull(scheduledAt);
    this.localDate = Objects.requireNonNull(localDate);
    this.timezone = Objects.requireNonNull(timezone);
    this.status = Objects.requireNonNull(status);
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static ActionOccurrence schedule(
      long id, long actionId, Instant at, LocalDate date, String zone, LocalDateTime now) {
    return new ActionOccurrence(id, actionId, at, date, zone, OccurrenceStatus.SCHEDULED, now, now);
  }

  public static ActionOccurrence rehydrate(
      long id,
      long actionId,
      Instant at,
      LocalDate date,
      String zone,
      OccurrenceStatus status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    return new ActionOccurrence(id, actionId, at, date, zone, status, createdAt, updatedAt);
  }

  public void checkIn(CheckInResultType result, LocalDateTime now) {
    if (result == null) {
      throw new BusinessException("GOAL_INVALID_CHECK_IN", "打卡结果不能为空");
    }
    status =
        switch (result) {
          case COMPLETED -> OccurrenceStatus.COMPLETED;
          case PARTIAL -> OccurrenceStatus.PARTIAL;
          case SKIPPED -> OccurrenceStatus.SKIPPED;
        };
    updatedAt = now;
  }

  public long getId() {
    return id;
  }

  public long getActionId() {
    return actionId;
  }

  public Instant getScheduledAt() {
    return scheduledAt;
  }

  public LocalDate getLocalDate() {
    return localDate;
  }

  public String getTimezone() {
    return timezone;
  }

  public OccurrenceStatus getStatus() {
    return status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
