package com.lingxi.companion.api;

import java.util.List;

/** Companion Agent 对用户端公开的稳定应用门面。 */
public interface CompanionFacade {
  ConversationResult createConversation(CreateConversationCommand command);

  AgentRunResult startRun(StartAgentRunCommand command);

  AgentRunResult getRun(long userId, long runId);

  AgentRunResult confirm(ConfirmProposalCommand command);

  List<AgentEventResult> listEvents(long userId, long runId, long afterEventId);

  List<MemoryResult> listMemories(long userId);

  MemoryResult updateMemory(UpdateMemoryCommand command);

  MemoryResult deleteMemory(long userId, long memoryId, long expectedVersion);
}
