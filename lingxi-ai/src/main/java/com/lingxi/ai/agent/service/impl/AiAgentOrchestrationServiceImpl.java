package com.lingxi.ai.agent.service.impl;

import com.lingxi.ai.agent.domain.AiAgentDefinition;
import com.lingxi.ai.agent.domain.AiAgentStep;
import com.lingxi.ai.agent.domain.dto.AiAgentSaveRequest;
import com.lingxi.ai.agent.domain.dto.AiAgentStepSaveRequest;
import com.lingxi.ai.agent.domain.vo.AiAgentDetailVO;
import com.lingxi.ai.agent.service.IAiAgentOrchestrationService;
import com.lingxi.ai.mapper.AiAgentDefinitionMapper;
import com.lingxi.ai.mapper.AiAgentStepMapper;
import com.lingxi.ai.mcp.constant.McpMarketConstants;
import com.lingxi.ai.mcp.service.IMcpToolMarketService;
import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.security.utils.SecurityUtils;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 智能体编排服务实现。
 */
@Service
public class AiAgentOrchestrationServiceImpl implements IAiAgentOrchestrationService {

    private static final String YES = "1";

    private final AiAgentDefinitionMapper agentMapper;

    private final AiAgentStepMapper stepMapper;

    private final IMcpToolMarketService toolMarketService;

    public AiAgentOrchestrationServiceImpl(AiAgentDefinitionMapper agentMapper, AiAgentStepMapper stepMapper,
            IMcpToolMarketService toolMarketService) {
        this.agentMapper = agentMapper;
        this.stepMapper = stepMapper;
        this.toolMarketService = toolMarketService;
    }

    @Override
    public List<AiAgentDefinition> listAgents(String agentCode, String agentName, String status) {
        return agentMapper.selectAgentList(agentCode, agentName, status);
    }

    @Override
    public AiAgentDetailVO getAgentDetail(Long agentId) {
        AiAgentDefinition agent = requireAgent(agentId);
        AiAgentDetailVO detail = new AiAgentDetailVO();
        detail.setAgent(agent);
        detail.setSteps(stepMapper.selectStepsByAgentCode(agent.getAgentCode()));
        detail.setBindings(toolMarketService.listBindings(agent.getAgentCode()));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveAgent(AiAgentSaveRequest request) {
        Date now = new Date();
        String username = SecurityUtils.getUsername();
        AiAgentDefinition agent = buildAgent(request);
        agent.setUpdateBy(username);
        agent.setUpdateTime(now);
        if (request.getAgentId() == null) {
            assertAgentCodeAvailable(request.getAgentCode());
            agent.setStatus(StringUtils.defaultIfEmpty(request.getStatus(), McpMarketConstants.TOOL_STATUS_DRAFT));
            agent.setAuditEnabled(StringUtils.defaultIfEmpty(request.getAuditEnabled(), YES));
            agent.setCreateBy(username);
            agent.setCreateTime(now);
            return agentMapper.insertAgent(agent);
        }
        agent.setAgentId(request.getAgentId());
        requireAgent(request.getAgentId());
        return agentMapper.updateAgent(agent);
    }

    @Override
    public int publishAgent(Long agentId) {
        AiAgentDefinition agent = requireAgent(agentId);
        if (StringUtils.isBlank(agent.getSystemPrompt())) {
            throw new ServiceException("发布智能体前需要配置系统提示词");
        }
        return agentMapper.updateAgentStatus(agentId, McpMarketConstants.TOOL_STATUS_PUBLISHED,
                SecurityUtils.getUsername());
    }

    @Override
    public int disableAgent(Long agentId) {
        requireAgent(agentId);
        return agentMapper.updateAgentStatus(agentId, McpMarketConstants.TOOL_STATUS_DISABLED,
                SecurityUtils.getUsername());
    }

    @Override
    public List<AiAgentStep> listSteps(String agentCode) {
        return stepMapper.selectStepsByAgentCode(agentCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveStep(AiAgentStepSaveRequest request) {
        AiAgentDefinition agent = agentMapper.selectAgentByCode(request.getAgentCode());
        if (agent == null) {
            throw new ServiceException("智能体不存在，无法保存编排步骤");
        }
        Date now = new Date();
        String username = SecurityUtils.getUsername();
        AiAgentStep step = buildStep(request);
        step.setUpdateBy(username);
        step.setUpdateTime(now);
        if (request.getStepId() == null) {
            step.setEnabled(StringUtils.defaultIfEmpty(request.getEnabled(), YES));
            step.setFailurePolicy(StringUtils.defaultIfEmpty(request.getFailurePolicy(), "STOP"));
            step.setCreateBy(username);
            step.setCreateTime(now);
            return stepMapper.insertStep(step);
        }
        step.setStepId(request.getStepId());
        requireStep(request.getStepId());
        return stepMapper.updateStep(step);
    }

    @Override
    public int deleteStep(Long stepId) {
        requireStep(stepId);
        return stepMapper.deleteStepById(stepId);
    }

    private AiAgentDefinition buildAgent(AiAgentSaveRequest request) {
        AiAgentDefinition agent = new AiAgentDefinition();
        agent.setAgentCode(request.getAgentCode());
        agent.setAgentName(request.getAgentName());
        agent.setBusinessScene(request.getBusinessScene());
        agent.setDescription(request.getDescription());
        agent.setSystemPrompt(request.getSystemPrompt());
        agent.setGuardrails(request.getGuardrails());
        agent.setStatus(request.getStatus());
        agent.setOwnerTeam(request.getOwnerTeam());
        agent.setAuditEnabled(request.getAuditEnabled());
        return agent;
    }

    private AiAgentStep buildStep(AiAgentStepSaveRequest request) {
        AiAgentStep step = new AiAgentStep();
        step.setAgentCode(request.getAgentCode());
        step.setStepOrder(request.getStepOrder());
        step.setStepName(request.getStepName());
        step.setStepType(request.getStepType());
        step.setToolName(request.getToolName());
        step.setInstruction(request.getInstruction());
        step.setConfigJson(request.getConfigJson());
        step.setFailurePolicy(request.getFailurePolicy());
        step.setEnabled(request.getEnabled());
        return step;
    }

    private void assertAgentCodeAvailable(String agentCode) {
        if (agentMapper.selectAgentByCode(agentCode) != null) {
            throw new ServiceException("智能体编码已存在");
        }
    }

    private AiAgentDefinition requireAgent(Long agentId) {
        AiAgentDefinition agent = agentMapper.selectAgentById(agentId);
        if (agent == null) {
            throw new ServiceException("智能体不存在");
        }
        return agent;
    }

    private void requireStep(Long stepId) {
        if (stepMapper.selectStepById(stepId) == null) {
            throw new ServiceException("编排步骤不存在");
        }
    }
}
