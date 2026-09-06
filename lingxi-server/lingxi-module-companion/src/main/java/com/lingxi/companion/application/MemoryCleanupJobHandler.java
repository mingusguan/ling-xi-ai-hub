package com.lingxi.companion.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.companion.domain.*;
import com.lingxi.kernel.*;
import java.util.List;
import org.springframework.stereotype.Component;

/** 将长期记忆删除传播到所有已配置投影后再完成本地生命周期。 */
@Component
public class MemoryCleanupJobHandler implements AsyncJobHandler {
  public static final String JOB_TYPE = "companion.memory.cleanup";
  private final AgentRepository repository;
  private final AgentTransactionService transactions;
  private final List<MemoryProjectionAdapter> projections;
  private final ObjectMapper json;

  public MemoryCleanupJobHandler(
      AgentRepository repository,
      AgentTransactionService transactions,
      List<MemoryProjectionAdapter> projections,
      ObjectMapper json) {
    this.repository = repository;
    this.transactions = transactions;
    this.projections = List.copyOf(projections);
    this.json = json;
  }

  public boolean supports(String jobType) {
    return JOB_TYPE.equals(jobType);
  }

  public String handle(AsyncJobMessage message) throws Exception {
    long memoryId = json.readTree(message.payloadJson()).path("memoryId").asLong();
    Memory memory =
        repository
            .findMemory(memoryId)
            .orElseThrow(() -> new BusinessException("AGENT_MEMORY_NOT_FOUND", "记忆不存在"));
    if (memory.getStatus() != Memory.Status.DELETED) {
      for (MemoryProjectionAdapter projection : projections) {
        projection.logicallyDelete(memoryId, memory.getUserId());
      }
      transactions.completeMemoryDeletion(memoryId);
    }
    return "{\"status\":\"DELETED\"}";
  }
}
