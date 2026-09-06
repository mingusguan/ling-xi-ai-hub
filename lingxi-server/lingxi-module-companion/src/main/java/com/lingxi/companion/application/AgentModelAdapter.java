package com.lingxi.companion.application;

import com.lingxi.identity.api.AgeBand;
import com.lingxi.companion.api.AgentRuntimeConfigProvider.AgentRuntimeConfig;
import java.util.List;

/** 模型适配端口。模型只能提出白名单工具名称，不能直接修改业务事实。 */
public interface AgentModelAdapter {
  AgentDecision decide(AgentRequest request);

  record AgentRequest(
      long runId,
      long userId,
      String scene,
      String input,
      List<Long> attachmentFileIds,
      AgeBand ageBand,
      AgentRuntimeConfig runtimeConfig) {}

  record AgentDecision(
      String responseText,
      String toolName,
      String argumentsJson,
      String modelVersion,
      String promptVersion,
      long inputTokens,
      long outputTokens,
      long costMinor) {}
}
