package com.lingxi.goal.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import java.util.Objects;

/** 周期复盘聚合，完成后结论不可覆盖。 */
public class Review {
  private final long id;
  private final long goalId;
  private final String periodKey;
  private final String inputSnapshotJson;
  private ReviewStatus status;
  private String conclusionJson;
  private String completionRequestKey;
  private String completionRequestDigest;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime completedAt;

  private Review(
      long id,
      long goalId,
      String periodKey,
      String input,
      ReviewStatus status,
      String conclusion,
      String requestKey,
      String digest,
      long version,
      LocalDateTime createdAt,
      LocalDateTime completedAt) {
    this.id = id;
    this.goalId = goalId;
    this.periodKey = Objects.requireNonNull(periodKey);
    this.inputSnapshotJson = Objects.requireNonNull(input);
    this.status = Objects.requireNonNull(status);
    this.conclusionJson = conclusion;
    this.completionRequestKey = requestKey;
    this.completionRequestDigest = digest;
    this.version = version;
    this.createdAt = createdAt;
    this.completedAt = completedAt;
  }

  public static Review schedule(
      long id, long goalId, String periodKey, String input, LocalDateTime now) {
    return new Review(
        id, goalId, periodKey, input, ReviewStatus.PENDING, null, null, null, 0, now, null);
  }

  public static Review rehydrate(
      long id,
      long goalId,
      String periodKey,
      String input,
      ReviewStatus status,
      String conclusion,
      String requestKey,
      String digest,
      long version,
      LocalDateTime createdAt,
      LocalDateTime completedAt) {
    return new Review(
        id,
        goalId,
        periodKey,
        input,
        status,
        conclusion,
        requestKey,
        digest,
        version,
        createdAt,
        completedAt);
  }

  public void complete(String requestKey, String digest, String conclusion, LocalDateTime now) {
    if (status == ReviewStatus.COMPLETED) {
      if (Objects.equals(completionRequestKey, requestKey)
          && Objects.equals(completionRequestDigest, digest)) {
        return;
      }
      throw new BusinessException("GOAL_REVIEW_ALREADY_COMPLETED", "复盘已完成，不能静默覆盖");
    }
    if (conclusion == null || conclusion.isBlank()) {
      throw new BusinessException("GOAL_EMPTY_REVIEW", "复盘结论不能为空");
    }
    status = ReviewStatus.COMPLETED;
    conclusionJson = conclusion;
    completionRequestKey = requestKey;
    completionRequestDigest = digest;
    completedAt = now;
    version++;
  }

  public long getId() {
    return id;
  }

  public long getGoalId() {
    return goalId;
  }

  public String getPeriodKey() {
    return periodKey;
  }

  public String getInputSnapshotJson() {
    return inputSnapshotJson;
  }

  public ReviewStatus getStatus() {
    return status;
  }

  public String getConclusionJson() {
    return conclusionJson;
  }

  public String getCompletionRequestKey() {
    return completionRequestKey;
  }

  public String getCompletionRequestDigest() {
    return completionRequestDigest;
  }

  public long getVersion() {
    return version;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }
}
