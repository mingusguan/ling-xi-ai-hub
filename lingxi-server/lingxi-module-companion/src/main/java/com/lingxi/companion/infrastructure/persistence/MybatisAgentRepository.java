package com.lingxi.companion.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.companion.domain.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisAgentRepository implements AgentRepository {
  private final ConversationMapper conversations;
  private final MessageMapper messages;
  private final AgentRunMapper runs;
  private final ActionProposalMapper proposals;
  private final RunEventMapper events;
  private final SafetyEventMapper safetyEvents;
  private final MemoryMapper memories;

  public MybatisAgentRepository(
      ConversationMapper conversations,
      MessageMapper messages,
      AgentRunMapper runs,
      ActionProposalMapper proposals,
      RunEventMapper events,
      SafetyEventMapper safetyEvents,
      MemoryMapper memories) {
    this.conversations = conversations;
    this.messages = messages;
    this.runs = runs;
    this.proposals = proposals;
    this.events = events;
    this.safetyEvents = safetyEvents;
    this.memories = memories;
  }

  public Optional<ConversationSnapshot> findConversation(long id) {
    return Optional.ofNullable(conversations.selectById(id)).map(this::conversation);
  }

  public Optional<ConversationSnapshot> findConversationByRequestKey(String key) {
    return Optional.ofNullable(
            conversations.selectOne(
                Wrappers.<ConversationEntity>lambdaQuery()
                    .eq(ConversationEntity::getRequestKey, key)
                    .last("LIMIT 1")))
        .map(this::conversation);
  }

  public void insertConversation(ConversationSnapshot c) {
    ConversationEntity e = new ConversationEntity();
    e.setId(c.id());
    e.setPublicId(c.publicId());
    e.setRequestKey(c.requestKey());
    e.setUserId(c.userId());
    e.setScene(c.scene());
    e.setTitle(c.title());
    e.setStatus(c.status());
    e.setCreatedAt(c.createdAt());
    e.setUpdatedAt(c.updatedAt());
    conversations.insert(e);
  }

  public Optional<AgentRun> findRun(long id) {
    return Optional.ofNullable(runs.selectById(id)).map(this::run);
  }

  public Optional<AgentRun> findRunByRequestKey(String key) {
    return Optional.ofNullable(
            runs.selectOne(
                Wrappers.<AgentRunEntity>lambdaQuery()
                    .eq(AgentRunEntity::getRequestKey, key)
                    .last("LIMIT 1")))
        .map(this::run);
  }

  public void insertRun(AgentRun run) {
    AgentRunEntity e = new AgentRunEntity();
    fill(e, run);
    runs.insert(e);
  }

  public boolean updateRun(AgentRun run, long previous) {
    return runs.update(
            null,
            Wrappers.<AgentRunEntity>lambdaUpdate()
                .eq(AgentRunEntity::getId, run.getId())
                .eq(AgentRunEntity::getVersion, previous)
                .set(AgentRunEntity::getStatus, run.getStatus().name())
                .set(AgentRunEntity::getModelVersion, run.getModelVersion())
                .set(AgentRunEntity::getPromptVersion, run.getPromptVersion())
                .set(AgentRunEntity::getResultText, run.getResultText())
                .set(AgentRunEntity::getErrorCode, run.getErrorCode())
                .set(AgentRunEntity::getVersion, run.getVersion())
                .set(AgentRunEntity::getUpdatedAt, run.getUpdatedAt()))
        == 1;
  }

  public void insertMessage(
      long id,
      long conversationId,
      Long runId,
      String role,
      String text,
      String digest,
      boolean aiGenerated,
      LocalDateTime now) {
    MessageEntity e = new MessageEntity();
    e.setId(id);
    e.setConversationId(conversationId);
    e.setRunId(runId);
    e.setRole(role);
    e.setContentText(text);
    e.setContentDigest(digest);
    e.setAiGenerated(aiGenerated);
    e.setCreatedAt(now);
    messages.insert(e);
  }

  public String findRunInput(long runId) {
    MessageEntity e =
        messages.selectOne(
            Wrappers.<MessageEntity>lambdaQuery()
                .eq(MessageEntity::getRunId, runId)
                .eq(MessageEntity::getRole, "USER")
                .orderByAsc(MessageEntity::getId)
                .last("LIMIT 1"));
    return e == null ? null : e.getContentText();
  }

  public String findRunAttachmentContext(long runId) {
    MessageEntity e =
        messages.selectOne(
            Wrappers.<MessageEntity>lambdaQuery()
                .eq(MessageEntity::getRunId, runId)
                .eq(MessageEntity::getRole, "CONTEXT")
                .orderByAsc(MessageEntity::getId)
                .last("LIMIT 1"));
    return e == null ? "[]" : e.getContentText();
  }

  public Optional<ActionProposal> findProposal(long id) {
    return Optional.ofNullable(proposals.selectById(id)).map(this::proposal);
  }

  public Optional<ActionProposal> findPendingProposal(long runId) {
    return Optional.ofNullable(
            proposals.selectOne(
                Wrappers.<ActionProposalEntity>lambdaQuery()
                    .eq(ActionProposalEntity::getRunId, runId)
                    .in(ActionProposalEntity::getStatus, "PENDING", "EXECUTING", "EXECUTED")
                    .orderByDesc(ActionProposalEntity::getId)
                    .last("LIMIT 1")))
        .map(this::proposal);
  }

  public void insertProposal(ActionProposal p) {
    ActionProposalEntity e = new ActionProposalEntity();
    fill(e, p);
    proposals.insert(e);
  }

  public boolean updateProposal(ActionProposal p, long previous) {
    return proposals.update(
            null,
            Wrappers.<ActionProposalEntity>lambdaUpdate()
                .eq(ActionProposalEntity::getId, p.getId())
                .eq(ActionProposalEntity::getVersion, previous)
                .set(ActionProposalEntity::getStatus, p.getStatus().name())
                .set(ActionProposalEntity::getDecision, p.getDecision())
                .set(ActionProposalEntity::getResultJson, p.getResultJson())
                .set(ActionProposalEntity::getVersion, p.getVersion())
                .set(ActionProposalEntity::getUpdatedAt, p.getUpdatedAt()))
        == 1;
  }

  public void insertEvent(long id, long runId, String type, String payload, LocalDateTime now) {
    RunEventEntity e = new RunEventEntity();
    e.setId(id);
    e.setRunId(runId);
    e.setEventType(type);
    e.setSafePayloadJson(payload);
    e.setCreatedAt(now);
    events.insert(e);
  }

  public List<EventSnapshot> listEvents(long runId, long after, int limit) {
    return events
        .selectList(
            Wrappers.<RunEventEntity>lambdaQuery()
                .eq(RunEventEntity::getRunId, runId)
                .gt(RunEventEntity::getId, after)
                .orderByAsc(RunEventEntity::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 500))))
        .stream()
        .map(
            e ->
                new EventSnapshot(
                    e.getId(), e.getEventType(), e.getSafePayloadJson(), e.getCreatedAt()))
        .toList();
  }

  public void insertSafetyEvent(
      long id,
      long runId,
      long userId,
      String riskType,
      String riskLevel,
      String disclosureScope,
      LocalDateTime now) {
    SafetyEventEntity e = new SafetyEventEntity();
    e.setId(id);
    e.setRunId(runId);
    e.setUserId(userId);
    e.setRiskType(riskType);
    e.setRiskLevel(riskLevel);
    e.setDisclosureScope(disclosureScope);
    e.setStatus("OPEN");
    e.setCreatedAt(now);
    safetyEvents.insert(e);
  }

  public Optional<Memory> findMemory(long id) {
    return Optional.ofNullable(memories.selectById(id)).map(this::memory);
  }

  public List<Memory> listActiveMemories(long userId) {
    return memories
        .selectList(
            Wrappers.<MemoryEntity>lambdaQuery()
                .eq(MemoryEntity::getUserId, userId)
                .eq(MemoryEntity::getStatus, "ACTIVE")
                .orderByDesc(MemoryEntity::getUpdatedAt))
        .stream()
        .map(this::memory)
        .toList();
  }

  public boolean updateMemory(Memory memory, long previous) {
    return memories.update(
            null,
            Wrappers.<MemoryEntity>lambdaUpdate()
                .eq(MemoryEntity::getId, memory.getId())
                .eq(MemoryEntity::getVersion, previous)
                .set(MemoryEntity::getContentText, memory.getContentText())
                .set(MemoryEntity::getStatus, memory.getStatus().name())
                .set(MemoryEntity::getVersion, memory.getVersion())
                .set(MemoryEntity::getUpdatedAt, memory.getUpdatedAt()))
        == 1;
  }

  private ConversationSnapshot conversation(ConversationEntity e) {
    return new ConversationSnapshot(
        e.getId(),
        e.getPublicId(),
        e.getRequestKey(),
        e.getUserId(),
        e.getScene(),
        e.getTitle(),
        e.getStatus(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private AgentRun run(AgentRunEntity e) {
    return AgentRun.rehydrate(
        e.getId(),
        e.getPublicId(),
        e.getRequestKey(),
        e.getRequestDigest(),
        e.getConversationId(),
        e.getUserId(),
        e.getScene(),
        e.getAuthorizationVersion(),
        AgentRun.Status.valueOf(e.getStatus()),
        e.getModelVersion(),
        e.getPromptVersion(),
        e.getResultText(),
        e.getErrorCode(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private ActionProposal proposal(ActionProposalEntity e) {
    return ActionProposal.rehydrate(
        e.getId(),
        e.getPublicId(),
        e.getRunId(),
        e.getUserId(),
        e.getToolName(),
        e.getRiskLevel(),
        e.getArgumentsJson(),
        e.getRequestDigest(),
        e.getAuthorizationVersion(),
        e.getExpiresAt(),
        ActionProposal.Status.valueOf(e.getStatus()),
        e.getDecision(),
        e.getResultJson(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private Memory memory(MemoryEntity e) {
    return Memory.rehydrate(
        e.getId(),
        e.getUserId(),
        e.getPurpose(),
        e.getContentText(),
        e.getSourceRef(),
        e.getSensitivity(),
        Memory.Status.valueOf(e.getStatus()),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private void fill(AgentRunEntity e, AgentRun r) {
    e.setId(r.getId());
    e.setPublicId(r.getPublicId());
    e.setRequestKey(r.getRequestKey());
    e.setRequestDigest(r.getRequestDigest());
    e.setConversationId(r.getConversationId());
    e.setUserId(r.getUserId());
    e.setScene(r.getScene());
    e.setStatus(r.getStatus().name());
    e.setAuthorizationVersion(r.getAuthorizationVersion());
    e.setModelVersion(r.getModelVersion());
    e.setPromptVersion(r.getPromptVersion());
    e.setResultText(r.getResultText());
    e.setErrorCode(r.getErrorCode());
    e.setVersion(r.getVersion());
    e.setCreatedAt(r.getCreatedAt());
    e.setUpdatedAt(r.getUpdatedAt());
  }

  private void fill(ActionProposalEntity e, ActionProposal p) {
    e.setId(p.getId());
    e.setPublicId(p.getPublicId());
    e.setRunId(p.getRunId());
    e.setUserId(p.getUserId());
    e.setToolName(p.getToolName());
    e.setRiskLevel(p.getRiskLevel());
    e.setArgumentsJson(p.getArgumentsJson());
    e.setRequestDigest(p.getRequestDigest());
    e.setAuthorizationVersion(p.getAuthorizationVersion());
    e.setExpiresAt(p.getExpiresAt());
    e.setStatus(p.getStatus().name());
    e.setDecision(p.getDecision());
    e.setResultJson(p.getResultJson());
    e.setVersion(p.getVersion());
    e.setCreatedAt(p.getCreatedAt());
    e.setUpdatedAt(p.getUpdatedAt());
  }
}
