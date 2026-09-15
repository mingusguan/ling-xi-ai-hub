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

  /**
   * 模型不可用时的降级回复文案。
   *
   * <p>为什么要有这段固定文案，而不是让运行直接失败：走 SSE 的对话里，用户看到的是
   * 一个永远停在「正在输入」的占位；而模型未配置或供应商持续报错这类原因重试无用，
   * 最后只能超时断流，用户完全不知道发生了什么。显式说明「这次没有生成回复」比让他干等更诚实。
   */
  static final String DEGRADED_REPLY =
      "伙伴暂时无法回应，这次没有生成有效回复。请稍后再说一次；如果反复出现，可以在「客服工单」里反馈。";

  /** 降级回复的版本标记，便于后台把它与真实模型输出区分开。 */
  static final String DEGRADED_MODEL_VERSION = "UNAVAILABLE";
  static final String DEGRADED_PROMPT_VERSION = "DEGRADED";

  private final AgentRepository repository;
  private final IdentityFacade identities;
  private final CompanionApplicationService service;
  private final AgentTransactionService transactions;
  private final AgentSafetyPolicy safety;
  private final AgentModelAdapter model;
  private final AgentRuntimeConfigProvider runtimeConfig;
  private final CompanionPreferenceProvider preferences;
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
      CompanionPreferenceProvider preferences,
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
    this.preferences = preferences;
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
      // 模型未配置时不重试：这是配置缺失而不是瞬时故障，重试只会重复失败并最终断流。
      if (model == null) {
        return status(degradeUnavailable(runId));
      }
      if(runtimeConfig==null)throw new BusinessException("AGENT_RUNTIME_CONFIG_UNAVAILABLE","Agent 运行时配置解析器尚未配置");
      List<Long> attachments =
          json.readValue(
              repository.findRunAttachmentContext(runId), new TypeReference<List<Long>>() {});
      long modelStartedAt=System.nanoTime();
      // 用户在新手引导里选的沟通风格、称呼与常见阻塞原因随请求一起交给模型适配器；
      // 少了这一步，引导里那些选项就是白填的。
      AgentModelAdapter.AgentDecision decision;
      try {
        decision =
            model.decide(
                new AgentModelAdapter.AgentRequest(
                    runId, run.getUserId(), run.getScene(), input, attachments, profile.ageBand(),
                    runtimeConfig.resolve(run.getScene()),
                    preferences.findPreference(run.getUserId())));
      } catch (RuntimeException exception) {
        // 供应商超时、限流、返回格式异常都会落到这里：同样是「这次没能生成回复」，
        // 而不是「伙伴无话可说」，因此走同一条降级路径。
        return status(degradeUnavailable(runId));
      }
      long latencyMillis=Math.max(0,(System.nanoTime()-modelStartedAt)/1_000_000L);
      if (decision == null) {
        return status(degradeUnavailable(runId));
      }
      events.publish(new AgentModelUsageEvent(UUID.randomUUID().toString(),runId,run.getScene(),
          profile.ageBand().name(),decision.modelVersion(),decision.promptVersion(),latencyMillis,
          Math.max(0,decision.inputTokens()),Math.max(0,decision.outputTokens()),
          Math.max(0,decision.costMinor()),Instant.now()));
      if (decision.toolName() == null || decision.toolName().isBlank()) {
        if (decision.responseText() == null || decision.responseText().isBlank()) {
          return status(degradeUnavailable(runId));
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

  /**
   * 模型不可用时的降级收口：给用户一条可读的说明，而不是让他对着「正在输入」干等。
   *
   * <p>这条回复的来源标记为 {@link #DEGRADED_MODEL_VERSION} / {@link #DEGRADED_PROMPT_VERSION}，
   * 后台据此把降级回复与真实模型输出分开统计，不会把降级误算成模型产出。
   */
  private AgentRun degradeUnavailable(long runId) {
    return transactions.completeDirect(
        runId, DEGRADED_REPLY, DEGRADED_MODEL_VERSION, DEGRADED_PROMPT_VERSION);
  }
}
