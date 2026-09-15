package com.lingxi.goal.domain;

import com.lingxi.goal.api.CheckInResultType;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 可修正但保留历史的打卡事实。
 *
 * <p>修正不覆盖原记录：原记录被标记为失效，新记录成为当前有效记录，
 * 因此「用户改过什么」始终可追溯；对外只暴露当前有效记录。
 */
public class CheckIn {
  private final long id;
  private final long occurrenceId;
  private final long userId;
  private final String requestKey;
  private final String requestDigest;
  private final CheckInResultType result;
  private final CheckInDetail detail;
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
      CheckInDetail detail,
      boolean effective,
      LocalDateTime recordedAt,
      LocalDateTime createdAt) {
    this.id = id;
    this.occurrenceId = occurrenceId;
    this.userId = userId;
    this.requestKey = Objects.requireNonNull(requestKey);
    this.requestDigest = Objects.requireNonNull(digest);
    this.result = Objects.requireNonNull(result);
    this.detail = detail == null ? CheckInDetail.minimal(null, null) : detail;
    this.effective = effective;
    this.recordedAt = recordedAt;
    this.createdAt = createdAt;
    // 结果与内容必须自洽，否则复盘会拿到互相矛盾的输入。
    this.detail.assertConsistentWith(this.result);
  }

  public static CheckIn record(
      long id,
      long occurrenceId,
      long userId,
      String requestKey,
      String digest,
      CheckInResultType result,
      CheckInDetail detail,
      LocalDateTime now) {
    if (id <= 0 || occurrenceId <= 0 || userId <= 0) {
      throw new BusinessException("GOAL_INVALID_CHECK_IN", "打卡标识不合法");
    }
    return new CheckIn(id, occurrenceId, userId, requestKey, digest, result, detail, true, now, now);
  }

  public static CheckIn rehydrate(
      long id,
      long occurrenceId,
      long userId,
      String requestKey,
      String digest,
      CheckInResultType result,
      CheckInDetail detail,
      boolean effective,
      LocalDateTime recordedAt,
      LocalDateTime createdAt) {
    return new CheckIn(
        id, occurrenceId, userId, requestKey, digest, result, detail, effective, recordedAt, createdAt);
  }

  /** 标记为已被修正替代；历史记录保留，只是不再生效。 */
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

  public CheckInDetail getDetail() {
    return detail;
  }

  public String getNote() {
    return detail.note();
  }

  public String getEvidenceReference() {
    return detail.evidenceReference();
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
