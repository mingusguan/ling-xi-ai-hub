package com.lingxi.companion.domain;

import java.time.LocalDateTime;
import java.util.*;

public interface AgentRepository {
  Optional<ConversationSnapshot> findConversation(long id);

  Optional<ConversationSnapshot> findConversationByRequestKey(String requestKey);

  void insertConversation(ConversationSnapshot conversation);

  Optional<AgentRun> findRun(long id);

  Optional<AgentRun> findRunByRequestKey(String requestKey);

  void insertRun(AgentRun run);

  boolean updateRun(AgentRun run, long previousVersion);

  void insertMessage(
      long id,
      long conversationId,
      Long runId,
      String role,
      String contentText,
      String contentDigest,
      boolean aiGenerated,
      LocalDateTime now);

  String findRunInput(long runId);

  String findRunAttachmentContext(long runId);

  Optional<ActionProposal> findProposal(long id);

  Optional<ActionProposal> findPendingProposal(long runId);

  void insertProposal(ActionProposal proposal);

  boolean updateProposal(ActionProposal proposal, long previousVersion);

  void insertEvent(
      long id, long runId, String eventType, String safePayloadJson, LocalDateTime now);

  List<EventSnapshot> listEvents(long runId, long afterEventId, int limit);

  void insertSafetyEvent(
      long id,
      long runId,
      long userId,
      String riskType,
      String riskLevel,
      String disclosureScope,
      LocalDateTime now);

  Optional<Memory> findMemory(long id);

  List<Memory> listActiveMemories(long userId);

  boolean updateMemory(Memory memory, long previousVersion);

  record ConversationSnapshot(
      long id,
      String publicId,
      String requestKey,
      long userId,
      String scene,
      String title,
      String status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}

  record EventSnapshot(
      long id, String eventType, String safePayloadJson, LocalDateTime createdAt) {}
}
