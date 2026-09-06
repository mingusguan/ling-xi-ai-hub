package com.lingxi.goal.domain;

import com.lingxi.goal.api.CheckInResultType;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import java.util.Objects;

/** 可修正但保留历史的打卡事实。 */
public class CheckIn {
  private final long id;
  private final long occurrenceId;
  private final long userId;
  private final String requestKey;
  private final String requestDigest;
  private final CheckInResultType result;
  private final String note;
  private final String evidenceReference;
  private boolean effective;
  private final LocalDateTime recordedAt;
  private final LocalDateTime createdAt;

  private CheckIn(
      long id,
      long occurrenceId,
      long userId,
      String requestKey,
      String digest,
      CheckInResultType result,
      String note,
      String evidence,
      boolean effective,
      LocalDateTime recordedAt,
      LocalDateTime createdAt) {
    this.id = id;
    this.occurrenceId = occurrenceId;
    this.userId = userId;
    this.requestKey = Objects.requireNonNull(requestKey);
    this.requestDigest = Objects.requireNonNull(digest);
    this.result = Objects.requireNonNull(result);
    this.note = note;
    this.evidenceReference = evidence;
    this.effective = effective;
    this.recordedAt = recordedAt;
    this.createdAt = createdAt;
  }

  public static CheckIn record(
      long id,
      long occurrenceId,
      long userId,
      String requestKey,
      String digest,
      CheckInResultType result,
      String note,
      String evidence,
      LocalDateTime now) {
    if (id <= 0 || occurrenceId <= 0 || userId <= 0) {
      throw new BusinessException("GOAL_INVALID_CHECK_IN", "打卡标识不合法");
    }
    return new CheckIn(
        id, occurrenceId, userId, requestKey, digest, result, note, evidence, true, now, now);
  }

  public static CheckIn rehydrate(
      long id,
      long occurrenceId,
      long userId,
      String requestKey,
      String digest,
      CheckInResultType result,
      String note,
      String evidence,
      boolean effective,
      LocalDateTime recordedAt,
      LocalDateTime createdAt) {
    return new CheckIn(
        id,
        occurrenceId,
        userId,
        requestKey,
        digest,
        result,
        note,
        evidence,
        effective,
        recordedAt,
        createdAt);
  }

  public void supersede() {
    effective = false;
  }

  public long getId() {
    return id;
  }

  public long getOccurrenceId() {
    return occurrenceId;
  }

  public long getUserId() {
    return userId;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public String getRequestDigest() {
    return requestDigest;
  }

  public CheckInResultType getResult() {
    return result;
  }

  public String getNote() {
    return note;
  }

  public String getEvidenceReference() {
    return evidenceReference;
  }

  public boolean isEffective() {
    return effective;
  }

  public LocalDateTime getRecordedAt() {
    return recordedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
