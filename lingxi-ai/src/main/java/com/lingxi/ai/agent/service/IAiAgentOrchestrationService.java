package com.lingxi.ai.agent.service;

import com.lingxi.ai.agent.domain.AiAgentDefinition;
import com.lingxi.ai.agent.domain.AiAgentStep;
import com.lingxi.ai.agent.domain.dto.AiAgentSaveRequest;
import com.lingxi.ai.agent.domain.dto.AiAgentStepSaveRequest;
import com.lingxi.ai.agent.domain.vo.AiAgentDetailVO;
import java.util.List;

/**
 * 智能体编排服务。
 */
public interface IAiAgentOrchestrationService {

    /**
     * 查询智能体列表。
     */
    List<AiAgentDefinition> listAgents(String agentCode, String agentName, String status);

    /**
     * 查询智能体详情。
     */
    AiAgentDetailVO getAgentDetail(Long agentId);

    /**
     * 保存智能体。
     */
    int saveAgent(AiAgentSaveRequest request);

    /**
     * 发布智能体。
     */
    int publishAgent(Long agentId);

    /**
     * 停用智能体。
     */
    int disableAgent(Long agentId);

    /**
     * 查询步骤列表。
     */
    List<AiAgentStep> listSteps(String agentCode);

    /**
     * 保存步骤。
     */
    int saveStep(AiAgentStepSaveRequest request);

    /**
     * 删除步骤。
     */
    int deleteStep(Long stepId);
}
