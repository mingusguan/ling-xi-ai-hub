package com.lingxi.companion.domain;

import com.lingxi.kernel.BusinessException;
import java.time.*;

/** 高风险工具调用确认凭证，绑定请求摘要、用户和授权版本。 */
public class ActionProposal {
  public enum Status {
    PENDING,
    EXECUTING,
    EXECUTED,
    REJECTED,
    EXPIRED
  }

  private final long id, runId, userId, authorizationVersion;
  private final String publicId, toolName, riskLevel, argumentsJson, requestDigest;
  private final Instant expiresAt;
  private Status status;
  private String decision, resultJson;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private ActionProposal(
      long id,
      String publicId,
      long runId,
      long userId,
      String toolName,
      String riskLevel,
      String argumentsJson,
      String requestDigest,
      long authorizationVersion,
      Instant expiresAt,
      Status status,
      String decision,
      String resultJson,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.publicId = publicId;
    this.runId = runId;
    this.userId = userId;
    this.toolName = toolName;
    this.riskLevel = riskLevel;
    this.argumentsJson = argumentsJson;
    this.requestDigest = requestDigest;
    this.authorizationVersion = authorizationVersion;
    this.expiresAt = expiresAt;
    this.status = status;
    this.decision = decision;
    this.resultJson = resultJson;
    this.version = version;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static ActionProposal create(
      long id,
      String publicId,
      long runId,
      long userId,
      String toolName,
      String riskLevel,
      String argumentsJson,
      String requestDigest,
      long authorizationVersion,
      Instant expiresAt,
      LocalDateTime now) {
    if (!"T2".equals(riskLevel) && !"T3".equals(riskLevel)) {
      throw new BusinessException("AGENT_PROPOSAL_RISK_INVALID", "只有 T2/T3 工具需要确认提案");
    }
    return new ActionProposal(
        id,
        publicId,
        runId,
        userId,
        toolName,
        riskLevel,
        argumentsJson,
        requestDigest,
        authorizationVersion,
        expiresAt,
        Status.PENDING,
        null,
        null,
        0,
        now,
        now);
  }

  public static ActionProposal rehydrate(
      long id,
      String publicId,
      long runId,
      long userId,
      String toolName,
      String riskLevel,
      String argumentsJson,
      String requestDigest,
      long authorizationVersion,
      Instant expiresAt,
      Status status,
      String decision,
      String resultJson,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    return new ActionProposal(
        id,
        publicId,
        runId,
        userId,
        toolName,
        riskLevel,
        argumentsJson,
        requestDigest,
        authorizationVersion,
        expiresAt,
        status,
        decision,
        resultJson,
        version,
        createdAt,
        updatedAt);
  }

  public void decide(
      long userId,
      long currentAuthorizationVersion,
      String digest,
      boolean accepted,
      long expectedVersion,
      Instant now,
      LocalDateTime localNow) {
    if (this.userId != userId) notFound();
    if (version != expectedVersion) conflict();
    if (status == Status.EXECUTED || status == Status.REJECTED) return;
    if (status != Status.PENDING) conflict();
    if (!requestDigest.equals(digest)) {
      throw new BusinessException("AGENT_PROPOSAL_DIGEST_MISMATCH", "确认内容与提案不一致");
    }
    if (authorizationVersion != currentAuthorizationVersion) {
      throw new BusinessException("AGENT_PROPOSAL_AUTH_CHANGED", "授权已变化，请重新生成提案");
    }
    if (!expiresAt.isAfter(now)) {
      status = Status.EXPIRED;
      decision = "EXPIRED";
      advance(localNow);
      return;
    }
    decision = accepted ? "ACCEPTED" : "REJECTED";
    status = accepted ? Status.EXECUTING : Status.REJECTED;
    advance(localNow);
  }

  public void complete(String resultJson, long expectedVersion, LocalDateTime now) {
    if (version != expectedVersion) conflict();
    if (status == Status.EXECUTED) return;
    if (status != Status.EXECUTING) conflict();
    this.resultJson = resultJson;
    status = Status.EXECUTED;
    advance(now);
  }

  public void assertExecutionClaim(long userId, long currentAuthorizationVersion, String digest) {
    if (this.userId != userId) notFound();
    if (!requestDigest.equals(digest)) {
      throw new BusinessException("AGENT_PROPOSAL_DIGEST_MISMATCH", "确认内容与提案不一致");
    }
    if (authorizationVersion != currentAuthorizationVersion) {
      throw new BusinessException("AGENT_PROPOSAL_AUTH_CHANGED", "授权已变化，请重新生成提案");
    }
    if (status != Status.EXECUTING && status != Status.EXECUTED) conflict();
  }

  private void advance(LocalDateTime now) {
    version++;
    updatedAt = now;
  }

  private void conflict() {
    throw new BusinessException("AGENT_PROPOSAL_CONFLICT", "提案状态已变化");
  }

  private void notFound() {
    throw new BusinessException("AGENT_PROPOSAL_NOT_FOUND", "提案不存在");
  }

  public long getId() {
    return id;
  }

  public String getPublicId() {
    return publicId;
  }

  public long getRunId() {
    return runId;
  }

  public long getUserId() {
    return userId;
  }

  public String getToolName() {
    return toolName;
  }

  public String getRiskLevel() {
    return riskLevel;
  }

  public String getArgumentsJson() {
    return argumentsJson;
  }

  public String getRequestDigest() {
    return requestDigest;
  }

  public long getAuthorizationVersion() {
    return authorizationVersion;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Status getStatus() {
    return status;
  }

  public String getDecision() {
    return decision;
  }

  public String getResultJson() {
    return resultJson;
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
