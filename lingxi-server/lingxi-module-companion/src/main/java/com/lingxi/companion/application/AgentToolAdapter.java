package com.lingxi.companion.application;

/** 经代码审核并编译注册的 Agent 工具；执行必须按 commandKey 保持业务幂等。 */
public interface AgentToolAdapter {
  enum RiskLevel {
    T0,
    T1,
    T2,
    T3
  }

  String toolName();

  RiskLevel riskLevel();

  boolean teenAllowed();

  ToolResult execute(ToolCommand command);

  record ToolCommand(long userId, String argumentsJson, String commandKey) {}

  record ToolResult(String resultJson, String userMessage) {}
}
