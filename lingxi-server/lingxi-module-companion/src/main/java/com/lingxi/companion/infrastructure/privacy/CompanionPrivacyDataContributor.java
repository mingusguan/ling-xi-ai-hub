package com.lingxi.companion.infrastructure.privacy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.companion.application.MemoryProjectionAdapter;
import com.lingxi.companion.infrastructure.persistence.*;
import com.lingxi.identity.api.PrivacyContribution;
import com.lingxi.identity.api.PrivacyDataContributor;
import com.lingxi.identity.api.PrivacyProcessingContext;
import com.lingxi.identity.api.PrivacyRequestType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 统一导出或逻辑删除用户的对话、智能体运行、记忆和安全事件。 */
@Component
public class CompanionPrivacyDataContributor implements PrivacyDataContributor {
  private final ConversationMapper conversationMapper;
  private final MessageMapper messageMapper;
  private final AgentRunMapper runMapper;
  private final RunEventMapper runEventMapper;
  private final ActionProposalMapper proposalMapper;
  private final MemoryMapper memoryMapper;
  private final SafetyEventMapper safetyMapper;
  private final ObjectMapper objectMapper;
  private final List<MemoryProjectionAdapter> projections;

  public CompanionPrivacyDataContributor(
      ConversationMapper conversationMapper,
      MessageMapper messageMapper,
      AgentRunMapper runMapper,
      RunEventMapper runEventMapper,
      ActionProposalMapper proposalMapper,
      MemoryMapper memoryMapper,
      SafetyEventMapper safetyMapper,
      ObjectMapper objectMapper,
      List<MemoryProjectionAdapter> projections) {
    this.conversationMapper = conversationMapper;
    this.messageMapper = messageMapper;
    this.runMapper = runMapper;
    this.runEventMapper = runEventMapper;
    this.proposalMapper = proposalMapper;
    this.memoryMapper = memoryMapper;
    this.safetyMapper = safetyMapper;
    this.objectMapper = objectMapper;
    this.projections = List.copyOf(projections);
  }

  @Override
  public String moduleName() {
    return "companion";
  }

  @Override
  @Transactional
  public PrivacyContribution process(PrivacyProcessingContext context) {
    long userId = context.userId();
    PrivacyRequestType type = context.type();
    if (type == PrivacyRequestType.EXPORT) {
      return PrivacyContribution.exported(export(userId));
    }
    if (type != PrivacyRequestType.DELETE_DATA && type != PrivacyRequestType.CLOSE_ACCOUNT) {
      return PrivacyContribution.unchanged();
    }
    for (MemoryProjectionAdapter projection : projections) {
      String receipt = projection.logicallyDeleteUserData(context.requestId(), userId);
      if (receipt == null || receipt.isBlank()) {
        throw new IllegalStateException("记忆外部投影未返回逻辑隔离凭证");
      }
    }
    return PrivacyContribution.deleted(logicallyDelete(userId));
  }

  private int logicallyDelete(long userId) {
    List<Long> conversationIds =
        conversationMapper.selectList(
                Wrappers.<ConversationEntity>lambdaQuery()
                    .select(ConversationEntity::getId)
                    .eq(ConversationEntity::getUserId, userId))
            .stream().map(ConversationEntity::getId).toList();
    List<Long> runIds =
        runMapper.selectList(
                Wrappers.<AgentRunEntity>lambdaQuery()
                    .select(AgentRunEntity::getId)
                    .eq(AgentRunEntity::getUserId, userId))
            .stream().map(AgentRunEntity::getId).toList();
    int affectedRows = 0;
    if (!runIds.isEmpty()) {
      affectedRows += runEventMapper.delete(
          Wrappers.<RunEventEntity>lambdaQuery().in(RunEventEntity::getRunId, runIds));
      affectedRows += proposalMapper.delete(
          Wrappers.<ActionProposalEntity>lambdaQuery().in(ActionProposalEntity::getRunId, runIds));
      affectedRows += runMapper.deleteByIds(runIds);
    }
    if (!conversationIds.isEmpty()) {
      affectedRows += messageMapper.delete(
          Wrappers.<MessageEntity>lambdaQuery()
              .in(MessageEntity::getConversationId, conversationIds));
      affectedRows += conversationMapper.deleteByIds(conversationIds);
    }
    affectedRows += memoryMapper.delete(
        Wrappers.<MemoryEntity>lambdaQuery().eq(MemoryEntity::getUserId, userId));
    affectedRows += safetyMapper.delete(
        Wrappers.<SafetyEventEntity>lambdaQuery().eq(SafetyEventEntity::getUserId, userId));
    return affectedRows;
  }

  private String export(long userId) {
    Map<String, Object> data = new LinkedHashMap<>();
    List<ConversationEntity> conversations = conversationMapper.selectList(
        Wrappers.<ConversationEntity>lambdaQuery()
            .select(
                ConversationEntity::getId, ConversationEntity::getPublicId,
                ConversationEntity::getUserId, ConversationEntity::getScene,
                ConversationEntity::getTitle, ConversationEntity::getStatus,
                ConversationEntity::getCreatedAt, ConversationEntity::getUpdatedAt)
            .eq(ConversationEntity::getUserId, userId));
    List<AgentRunEntity> runs = runMapper.selectList(
        Wrappers.<AgentRunEntity>lambdaQuery()
            .select(
                AgentRunEntity::getId, AgentRunEntity::getPublicId,
                AgentRunEntity::getConversationId, AgentRunEntity::getUserId,
                AgentRunEntity::getScene, AgentRunEntity::getStatus,
                AgentRunEntity::getModelVersion, AgentRunEntity::getPromptVersion,
                AgentRunEntity::getResultText, AgentRunEntity::getCreatedAt,
                AgentRunEntity::getUpdatedAt)
            .eq(AgentRunEntity::getUserId, userId));
    List<Long> conversationIds = conversations.stream().map(ConversationEntity::getId).toList();
    List<Long> runIds = runs.stream().map(AgentRunEntity::getId).toList();
    data.put("conversations", conversations);
    data.put("messages", conversationIds.isEmpty() ? List.of() : messageMapper.selectList(
        Wrappers.<MessageEntity>lambdaQuery()
            .select(
                MessageEntity::getId, MessageEntity::getConversationId, MessageEntity::getRunId,
                MessageEntity::getRole, MessageEntity::getContentText,
                MessageEntity::getAiGenerated, MessageEntity::getCreatedAt)
            .in(MessageEntity::getConversationId, conversationIds)));
    data.put("runs", runs);
    data.put("runEvents", runIds.isEmpty() ? List.of() : runEventMapper.selectList(
        Wrappers.<RunEventEntity>lambdaQuery()
            .select(
                RunEventEntity::getId, RunEventEntity::getRunId,
                RunEventEntity::getEventType, RunEventEntity::getSafePayloadJson,
                RunEventEntity::getCreatedAt)
            .in(RunEventEntity::getRunId, runIds)));
    data.put("proposals", proposalMapper.selectList(
        Wrappers.<ActionProposalEntity>lambdaQuery()
            .select(
                ActionProposalEntity::getId, ActionProposalEntity::getPublicId,
                ActionProposalEntity::getRunId, ActionProposalEntity::getUserId,
                ActionProposalEntity::getToolName, ActionProposalEntity::getRiskLevel,
                ActionProposalEntity::getArgumentsJson, ActionProposalEntity::getExpiresAt,
                ActionProposalEntity::getStatus, ActionProposalEntity::getDecision,
                ActionProposalEntity::getResultJson, ActionProposalEntity::getCreatedAt,
                ActionProposalEntity::getUpdatedAt)
            .eq(ActionProposalEntity::getUserId, userId)));
    data.put("memories", memoryMapper.selectList(
        Wrappers.<MemoryEntity>lambdaQuery()
            .select(
                MemoryEntity::getId, MemoryEntity::getUserId, MemoryEntity::getPurpose,
                MemoryEntity::getContentText, MemoryEntity::getSourceRef,
                MemoryEntity::getSensitivity, MemoryEntity::getStatus,
                MemoryEntity::getCreatedAt, MemoryEntity::getUpdatedAt)
            .eq(MemoryEntity::getUserId, userId)));
    data.put("safetyEvents", safetyMapper.selectList(
        Wrappers.<SafetyEventEntity>lambdaQuery()
            .select(
                SafetyEventEntity::getId, SafetyEventEntity::getRunId,
                SafetyEventEntity::getUserId, SafetyEventEntity::getRiskType,
                SafetyEventEntity::getRiskLevel, SafetyEventEntity::getDisclosureScope,
                SafetyEventEntity::getStatus, SafetyEventEntity::getCreatedAt,
                SafetyEventEntity::getHandledAt)
            .eq(SafetyEventEntity::getUserId, userId)));
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("陪伴数据导出失败", e);
    }
  }
}
