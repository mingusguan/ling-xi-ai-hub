package com.lingxi.companion.application;

/** 长期记忆外部投影清理端口；实现必须按请求幂等逻辑隔离向量和缓存。 */
public interface MemoryProjectionAdapter {
  void logicallyDelete(long memoryId, long userId);

  /** 按隐私请求隔离用户全部向量和缓存，返回可审计的非敏感凭证。 */
  String logicallyDeleteUserData(long requestId, long userId);
}
