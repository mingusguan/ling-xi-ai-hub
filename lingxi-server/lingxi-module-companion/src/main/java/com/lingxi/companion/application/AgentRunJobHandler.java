package com.lingxi.companion.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.companion.domain.*;
import com.lingxi.companion.api.AgentRuntimeConfigProvider;
import com.lingxi.companion.api.AgentModelUsageEvent;
import com.lingxi.identity.api.*;
import com.lingxi.kernel.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 可恢复 Agent Run worker；模型和工具外部调用均位于数据库事务之外。 */
@Component
public class AgentRunJobHandler implements AsyncJobHandler {
  private final AgentRepository repository;
  private final IdentityFacade identities;
  private final CompanionApplicationService service;
  private final AgentTransactionService transactions;
  private final AgentSafetyPolicy safety;
  private final AgentModelAdapter model;
  private final AgentRuntimeConfigProvider runtimeConfig;
  private final ObjectMapper json;
  private final Duration proposalTtl;
  private final DomainEventPublisher events;

  public AgentRunJobHandler(
      AgentRepository repository,
      IdentityFacade identities,
      CompanionApplicationService service,
      AgentTransactionService transactions,
      AgentSafetyPolicy safety,
      List<AgentModelAdapter> models,
      List<AgentRuntimeConfigProvider> runtimeConfigProviders,
      DomainEventPublisher events,
      ObjectMapper json,
      @Value("${lingxi.agent.proposal-ttl:PT10M}") Duration proposalTtl) {
    this.repository = repository;
    this.identities = identities;
    this.service = service;
    this.transactions = transactions;
    this.safety = safety;
    this.model = models.stream().findFirst().orElse(null);
    this.runtimeConfig = runtimeConfigProviders.stream().findFirst().orElse(null);
    this.json = json;
    this.proposalTtl = proposalTtl;
    this.events=events;
  }

  public boolean supports(String jobType) {
    return CompanionApplicationService.RUN_JOB.equals(jobType);
  }

  public String handle(AsyncJobMessage message) throws Exception {
    long runId = json.readTree(message.payloadJson()).path("runId").asLong();
    AgentRun current = service.run(runId);
    if (current.getStatus() == AgentRun.Status.SUCCEEDED
        || current.getStatus() == AgentRun.Status.WAITING_CONFIRMATION
        || current.getStatus() == AgentRun.Status.FAILED_FINAL
        || current.getStatus() == AgentRun.Status.CANCELLED) {
      return status(current);
    }
    try {
      AgentRun run = transactions.beginRun(runId);
      AccessProfile profile = identities.getAccessProfile(run.getUserId());
      if (!profile.coreFeaturesAllowed()
          || profile.authorizationVersion() != run.getAuthorizationVersion()) {
        throw new BusinessException("AGENT_AUTH_CHANGED", "Agent 运行授权已失效");
      }
      String input = repository.findRunInput(runId);
      Optional<AgentSafetyPolicy.SafetyDecision> risk = safety.evaluate(input);
      if (risk.isPresent()) return status(transactions.completeSafety(runId, risk.get()));
      if (model == null) {
        throw new BusinessException("AGENT_MODEL_UNAVAILABLE", "Agent 模型尚未配置");
      }
      if(runtimeConfig==null)throw new BusinessException("AGENT_RUNTIME_CONFIG_UNAVAILABLE","Agent 运行时配置解析器尚未配置");
      List<Long> attachments =
          json.readValue(
              repository.findRunAttachmentContext(runId), new TypeReference<List<Long>>() {});
      long modelStartedAt=System.nanoTime();
      AgentModelAdapter.AgentDecision decision =
          model.decide(
              new AgentModelAdapter.AgentRequest(
                  runId, run.getUserId(), run.getScene(), input, attachments, profile.ageBand(),
                  runtimeConfig.resolve(run.getScene())));
      long latencyMillis=Math.max(0,(System.nanoTime()-modelStartedAt)/1_000_000L);
      if (decision == null) {
        throw new BusinessException("AGENT_MODEL_RESULT_INVALID", "模型未返回有效结果");
      }
      events.publish(new AgentModelUsageEvent(UUID.randomUUID().toString(),runId,run.getScene(),
          profile.ageBand().name(),decision.modelVersion(),decision.promptVersion(),latencyMillis,
          Math.max(0,decision.inputTokens()),Math.max(0,decision.outputTokens()),
          Math.max(0,decision.costMinor()),Instant.now()));
      if (decision.toolName() == null || decision.toolName().isBlank()) {
        if (decision.responseText() == null || decision.responseText().isBlank()) {
          throw new BusinessException("AGENT_MODEL_RESULT_INVALID", "模型响应为空");
        }
        return status(
            transactions.completeDirect(
                runId, decision.responseText(), decision.modelVersion(), decision.promptVersion()));
      }
      AgentToolAdapter tool = service.tool(decision.toolName(), profile.ageBand());
      String arguments = decision.argumentsJson() == null ? "{}" : decision.argumentsJson();
      String digest = AgentTransactionService.Digests.sha256(tool.toolName() + "\n" + arguments);
      if (tool.riskLevel() == AgentToolAdapter.RiskLevel.T2
          || tool.riskLevel() == AgentToolAdapter.RiskLevel.T3) {
        ActionProposal proposal =
            transactions.createProposal(
                run,
                tool.toolName(),
                tool.riskLevel().name(),
                arguments,
                digest,
                Instant.now().plus(proposalTtl),
                decision.modelVersion(),
                decision.promptVersion());
        return "{\"status\":\"WAITING_CONFIRMATION\",\"proposalId\":" + proposal.getId() + "}";
      }
      AgentToolAdapter.ToolResult toolResult =
          tool.execute(
              new AgentToolAdapter.ToolCommand(
                  run.getUserId(), arguments, "RUN:" + run.getPublicId() + ":" + tool.toolName()));
      if (toolResult == null
          || toolResult.userMessage() == null
          || toolResult.userMessage().isBlank()) {
        throw new BusinessException("AGENT_TOOL_RESULT_INVALID", "工具未返回可验证结果");
      }
      return status(
          transactions.completeDirect(
              runId, toolResult.userMessage(), decision.modelVersion(), decision.promptVersion()));
    } catch (BusinessException exception) {
      transactions.fail(runId, exception.getCode(), false);
      return "{\"status\":\"FAILED_FINAL\",\"errorCode\":\"" + exception.getCode() + "\"}";
    } catch (RuntimeException exception) {
      transactions.fail(runId, "AGENT_DEPENDENCY_FAILURE", true);
      throw exception;
    }
  }

  private String status(AgentRun run) {
    return "{\"status\":\"" + run.getStatus().name() + "\"}";
  }
}
