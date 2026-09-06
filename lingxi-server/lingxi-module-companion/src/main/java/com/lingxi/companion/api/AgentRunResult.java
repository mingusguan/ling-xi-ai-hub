package com.lingxi.companion.api;

import java.time.*;

public record AgentRunResult(
    long id,
    String publicId,
    long conversationId,
    long userId,
    String scene,
    String status,
    String resultText,
    String errorCode,
    long version,
    ProposalResult proposal,
    LocalDateTime createdAt) {
  public record ProposalResult(
      long id,
      String publicId,
      String toolName,
      String riskLevel,
      String argumentsJson,
      String digest,
      Instant expiresAt,
      String status,
      long version) {}
}
