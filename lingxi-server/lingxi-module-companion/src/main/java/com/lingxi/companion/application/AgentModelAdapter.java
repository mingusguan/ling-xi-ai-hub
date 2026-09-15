package com.lingxi.companion.application;

import com.lingxi.identity.api.AgeBand;
import com.lingxi.companion.api.AgentRuntimeConfigProvider.AgentRuntimeConfig;
import com.lingxi.kernel.CompanionPreference;
import java.util.List;

/** 模型适配端口。模型只能提出白名单工具名称，不能直接修改业务事实。 */
public interface AgentModelAdapter {
  AgentDecision decide(AgentRequest request);

  /**
   * 一次模型调用的输入。
   *
   * <p>{@code preference} 是新手引导画像里与对话有关的偏好（沟通风格、称呼、常见阻塞原因）。
   * 它必须随请求一起传给模型适配器，否则用户在引导里选的「教练式」「温和」完全没有效果——
   * 引导收集了却没人读，等于让用户白填。
   */
  record AgentRequest(
      long runId,
      long userId,
      String scene,
      String input,
      List<Long> attachmentFileIds,
      AgeBand ageBand,
      AgentRuntimeConfig runtimeConfig,
      CompanionPreference preference) {}

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
