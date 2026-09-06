package com.lingxi.companion.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.companion.api.*;
import com.lingxi.companion.domain.*;
import com.lingxi.content.api.ContentFacade;
import com.lingxi.identity.api.*;
import com.lingxi.kernel.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 对话、运行查询和高风险确认的应用入口。 */
@Service
public class CompanionApplicationService implements CompanionFacade {
  public static final String RUN_JOB = "companion.agent.run";
  private final AgentRepository repository;
  private final IdentityFacade identities;
  private final ContentFacade contents;
  private final AgentTransactionService transactions;
  private final AsyncJobScheduler jobs;
  private final Map<String, AgentToolAdapter> tools = new HashMap<>();
  private final ObjectMapper json;

  public CompanionApplicationService(
      AgentRepository repository,
      IdentityFacade identities,
      ContentFacade contents,
      AgentTransactionService transactions,
      AsyncJobScheduler jobs,
      List<AgentToolAdapter> toolAdapters,
      ObjectMapper json) {
    this.repository = repository;
    this.identities = identities;
    this.contents = contents;
    this.transactions = transactions;
    this.jobs = jobs;
    this.json = json;
    toolAdapters.forEach(tool -> tools.put(tool.toolName(), tool));
  }

  public ConversationResult createConversation(CreateConversationCommand command) {
    requireProfile(command.userId());
    if (blank(command.requestKey()) || blank(command.scene()) || blank(command.title())) {
      throw error("AGENT_CONVERSATION_INVALID", "会话参数不完整");
    }
    return conversation(
        transactions.createConversation(
            command.requestKey(), command.userId(), command.scene(), command.title()));
  }

  public AgentRunResult startRun(StartAgentRunCommand command) {
    AccessProfile profile = requireProfile(command.userId());
    AgentRepository.ConversationSnapshot conversation =
        repository
            .findConversation(command.conversationId())
            .orElseThrow(() -> error("AGENT_CONVERSATION_NOT_FOUND", "会话不存在"));
    if (conversation.userId() != command.userId()) {
      throw error("AGENT_CONVERSATION_NOT_FOUND", "会话不存在");
    }
    if (blank(command.requestKey()) || blank(command.input()) || command.input().length() > 20000) {
      throw error("AGENT_INPUT_INVALID", "Agent 输入不合法");
    }
    List<Long> attachments =
        command.attachmentFileIds() == null
            ? List.of()
            : command.attachmentFileIds().stream().distinct().sorted().toList();
    if (attachments.size() > 10) throw error("AGENT_ATTACHMENTS_LIMIT", "附件数量超过限制");
    attachments.forEach(fileId -> contents.getReadyFile(command.userId(), fileId));
    String attachmentContext = writeJson(attachments);
    String digest =
        AgentTransactionService.Digests.sha256(
            command.conversationId()
                + "\n"
                + command.scene()
                + "\n"
                + command.input()
                + "\n"
                + attachmentContext);
    AgentRun run =
        transactions.createRun(
            command.requestKey(),
            digest,
            command.conversationId(),
            command.userId(),
            command.scene(),
            profile.authorizationVersion(),
            command.input(),
            attachmentContext);
    if (run.getStatus() == AgentRun.Status.QUEUED
        || run.getStatus() == AgentRun.Status.FAILED_RETRYABLE) {
      jobs.schedule(RUN_JOB, String.valueOf(run.getId()), "{\"runId\":" + run.getId() + "}", 5);
    }
    return result(run);
  }

  @Transactional(readOnly = true)
  public AgentRunResult getRun(long userId, long runId) {
    AgentRun run = run(runId);
    run.assertOwner(userId);
    return result(run);
  }

  public AgentRunResult confirm(ConfirmProposalCommand command) {
    AccessProfile profile = requireProfile(command.userId());
    ActionProposal requestedProposal =
        repository
            .findProposal(command.proposalId())
            .orElseThrow(() -> error("AGENT_PROPOSAL_NOT_FOUND", "提案不存在"));
    if (requestedProposal.getRunId() != command.runId()) {
      throw error("AGENT_PROPOSAL_NOT_FOUND", "提案不存在");
    }
    AgentTransactionService.ClaimResult claim =
        transactions.claimProposal(
            command.proposalId(),
            command.userId(),
            command.decision(),
            command.digest(),
            command.expectedVersion(),
            profile,
            command.recentAuthentication());
    if (!claim.accepted() || claim.completed()) return result(run(command.runId()));
    ActionProposal proposal = claim.proposal();
    AgentToolAdapter tool = tool(proposal.getToolName(), profile.ageBand());
    AgentToolAdapter.ToolResult executed =
        tool.execute(
            new AgentToolAdapter.ToolCommand(
                command.userId(),
                proposal.getArgumentsJson(),
                "PROPOSAL:" + proposal.getPublicId()));
    if (executed == null || blank(executed.userMessage())) {
      throw error("AGENT_TOOL_RESULT_INVALID", "工具未返回可验证结果");
    }
    AgentRun completed =
        transactions.completeProposal(
            proposal.getId(),
            executed.resultJson(),
            executed.userMessage(),
            claim.run().getModelVersion(),
            claim.run().getPromptVersion());
    return result(completed);
  }

  @Transactional(readOnly = true)
  public List<AgentEventResult> listEvents(long userId, long runId, long afterEventId) {
    AgentRun run = run(runId);
    run.assertOwner(userId);
    return repository.listEvents(runId, Math.max(0, afterEventId), 200).stream()
        .map(e -> new AgentEventResult(e.id(), e.eventType(), e.safePayloadJson(), e.createdAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<MemoryResult> listMemories(long userId) {
    requireProfile(userId);
    return repository.listActiveMemories(userId).stream().map(this::memory).toList();
  }

  public MemoryResult updateMemory(UpdateMemoryCommand command) {
    requireProfile(command.userId());
    return memory(
        transactions.updateMemory(
            command.userId(),
            command.memoryId(),
            command.contentText(),
            command.expectedVersion()));
  }

  public MemoryResult deleteMemory(long userId, long memoryId, long expectedVersion) {
    requireProfile(userId);
    Memory memory = transactions.beginMemoryDeletion(userId, memoryId, expectedVersion);
    jobs.schedule(
        MemoryCleanupJobHandler.JOB_TYPE,
        String.valueOf(memoryId),
        "{\"memoryId\":" + memoryId + "}",
        8);
    return memory(memory);
  }

  AgentToolAdapter tool(String name, AgeBand ageBand) {
    AgentToolAdapter tool = tools.get(name);
    if (tool == null) throw error("AGENT_TOOL_NOT_ALLOWED", "工具未在白名单中");
    if (ageBand == AgeBand.TEEN && !tool.teenAllowed()) {
      throw error("AGENT_TOOL_NOT_ALLOWED", "青少年模式不允许该工具");
    }
    return tool;
  }

  AgentRun run(long id) {
    return repository.findRun(id).orElseThrow(() -> error("AGENT_RUN_NOT_FOUND", "Agent 运行不存在"));
  }

  private AccessProfile requireProfile(long userId) {
    AccessProfile profile = identities.getAccessProfile(userId);
    if (!profile.coreFeaturesAllowed() || profile.ageBand() == AgeBand.UNDER_14) {
      throw error("AGENT_ACCOUNT_RESTRICTED", "当前账号不能使用 Agent");
    }
    return profile;
  }

  private AgentRunResult result(AgentRun run) {
    AgentRunResult.ProposalResult proposal =
        repository
            .findPendingProposal(run.getId())
            .map(
                p ->
                    new AgentRunResult.ProposalResult(
                        p.getId(),
                        p.getPublicId(),
                        p.getToolName(),
                        p.getRiskLevel(),
                        p.getArgumentsJson(),
                        p.getRequestDigest(),
                        p.getExpiresAt(),
                        p.getStatus().name(),
                        p.getVersion()))
            .orElse(null);
    return new AgentRunResult(
        run.getId(),
        run.getPublicId(),
        run.getConversationId(),
        run.getUserId(),
        run.getScene(),
        run.getStatus().name(),
        run.getResultText(),
        run.getErrorCode(),
        run.getVersion(),
        proposal,
        run.getCreatedAt());
  }

  private ConversationResult conversation(AgentRepository.ConversationSnapshot c) {
    return new ConversationResult(
        c.id(), c.publicId(), c.userId(), c.scene(), c.title(), c.status(), c.createdAt());
  }

  private MemoryResult memory(Memory memory) {
    return new MemoryResult(
        memory.getId(),
        memory.getUserId(),
        memory.getPurpose(),
        memory.getContentText(),
        memory.getSourceRef(),
        memory.getSensitivity(),
        memory.getStatus().name(),
        memory.getVersion(),
        memory.getUpdatedAt());
  }

  private String writeJson(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw error("AGENT_REQUEST_INVALID", "Agent 请求无法序列化");
    }
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private BusinessException error(String code, String message) {
    return new BusinessException(code, message);
  }
}
