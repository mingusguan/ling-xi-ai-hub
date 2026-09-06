package com.lingxi.companion.api;

/** Agent 调用模型前解析已经审批发布的运行时配置。 */
public interface AgentRuntimeConfigProvider {

  AgentRuntimeConfig resolve(String scene);

  record AgentRuntimeConfig(String modelProviderRef,String modelProviderDigest,
      String modelRouteRef,String modelRouteDigest,String promptRef,String promptDigest) {}
}
