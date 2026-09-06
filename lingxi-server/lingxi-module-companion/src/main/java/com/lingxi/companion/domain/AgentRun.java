package com.lingxi.companion.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;

/** 可恢复 Agent 运行聚合，所有状态只能按受控路径推进。 */
public class AgentRun {
  public enum Status {
    QUEUED,
    RUNNING,
    WAITING_CONFIRMATION,
    SUCCEEDED,
    FAILED_RETRYABLE,
    FAILED_FINAL,
    CANCELLED
  }

  private final long id;
  private final String publicId;
  private final String requestKey;
  private final String requestDigest;
  private final long conversationId;
  private final long userId;
  private final String scene;
  private final long authorizationVersion;
  private Status status;
  private String modelVersion;
  private String promptVersion;
  private String resultText;
  private String errorCode;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private AgentRun(
      long id,
      String publicId,
      String requestKey,
      String requestDigest,
      long conversationId,
      long userId,
      String scene,
      long authorizationVersion,
      Status status,
      String modelVersion,
      String promptVersion,
      String resultText,
      String errorCode,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.publicId = publicId;
    this.requestKey = requestKey;
    this.requestDigest = requestDigest;
    this.conversationId = conversationId;
    this.userId = userId;
    this.scene = scene;
    this.authorizationVersion = authorizationVersion;
    this.status = status;
    this.modelVersion = modelVersion;
    this.promptVersion = promptVersion;
    this.resultText = resultText;
    this.errorCode = errorCode;
    this.version = version;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static AgentRun create(
      long id,
      String publicId,
      String requestKey,
      String requestDigest,
      long conversationId,
      long userId,
      String scene,
      long authorizationVersion,
      LocalDateTime now) {
    if (id <= 0
        || conversationId <= 0
        || userId <= 0
        || blank(publicId)
        || blank(requestKey)
        || blank(requestDigest)
        || blank(scene)) {
      throw new BusinessException("AGENT_RUN_INVALID", "Agent 运行参数不完整");
    }
    return new AgentRun(
        id,
        publicId,
        requestKey,
        requestDigest,
        conversationId,
        userId,
        scene,
        authorizationVersion,
        Status.QUEUED,
        null,
        null,
        null,
        null,
        0,
        now,
        now);
  }

  public static AgentRun rehydrate(
      long id,
      String publicId,
      String requestKey,
      String requestDigest,
      long conversationId,
      long userId,
      String scene,
      long authorizationVersion,
      Status status,
      String modelVersion,
      String promptVersion,
      String resultText,
      String errorCode,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    return new AgentRun(
        id,
        publicId,
        requestKey,
        requestDigest,
        conversationId,
        userId,
        scene,
        authorizationVersion,
        status,
        modelVersion,
        promptVersion,
        resultText,
        errorCode,
        version,
        createdAt,
        updatedAt);
  }

  public void start(long expectedVersion, LocalDateTime now) {
    check(expectedVersion);
    if (status == Status.RUNNING) return;
    if (status != Status.QUEUED && status != Status.FAILED_RETRYABLE) invalid();
    status = Status.RUNNING;
    errorCode = null;
    advance(now);
  }

  public void waitForConfirmation(
      String modelVersion, String promptVersion, long expectedVersion, LocalDateTime now) {
    check(expectedVersion);
    if (status != Status.RUNNING) invalid();
    this.modelVersion = modelVersion;
    this.promptVersion = promptVersion;
    status = Status.WAITING_CONFIRMATION;
    advance(now);
  }

  public void resumeAfterConfirmation(long expectedVersion, LocalDateTime now) {
    check(expectedVersion);
    if (status != Status.WAITING_CONFIRMATION) invalid();
    status = Status.RUNNING;
    advance(now);
  }

  public void succeed(
      String resultText,
      String modelVersion,
      String promptVersion,
      long expectedVersion,
      LocalDateTime now) {
    check(expectedVersion);
    if (status != Status.RUNNING) invalid();
    this.resultText = resultText;
    this.modelVersion = modelVersion;
    this.promptVersion = promptVersion;
    status = Status.SUCCEEDED;
    advance(now);
  }

  public void fail(String code, boolean retryable, long expectedVersion, LocalDateTime now) {
    check(expectedVersion);
    if (status == Status.SUCCEEDED || status == Status.CANCELLED) invalid();
    errorCode = code;
    status = retryable ? Status.FAILED_RETRYABLE : Status.FAILED_FINAL;
    advance(now);
  }

  public void assertOwner(long userId) {
    if (this.userId != userId) {
      throw new BusinessException("AGENT_RUN_NOT_FOUND", "Agent 运行不存在");
    }
  }

  private void check(long expectedVersion) {
    if (version != expectedVersion) {
      throw new BusinessException("AGENT_RUN_CONFLICT", "Agent 运行状态已变化");
    }
  }

  private void advance(LocalDateTime now) {
    version++;
    updatedAt = now;
  }

  private void invalid() {
    throw new BusinessException("AGENT_RUN_STATE_INVALID", "Agent 运行状态不允许当前操作");
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  public long getId() {
    return id;
  }

  public String getPublicId() {
    return publicId;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public String getRequestDigest() {
    return requestDigest;
  }

  public long getConversationId() {
    return conversationId;
  }

  public long getUserId() {
    return userId;
  }

  public String getScene() {
    return scene;
  }

  public long getAuthorizationVersion() {
    return authorizationVersion;
  }

  public Status getStatus() {
    return status;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public String getPromptVersion() {
    return promptVersion;
  }

  public String getResultText() {
    return resultText;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public long getVersion() {
    return version;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
