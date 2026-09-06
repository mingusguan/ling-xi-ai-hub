package com.lingxi.companion.api;

/** 高风险提案确认命令，摘要必须来自提案响应。 */
public record ConfirmProposalCommand(
    long userId,
    long runId,
    long proposalId,
    String decision,
    String digest,
    long expectedVersion,
    boolean recentAuthentication) {}
