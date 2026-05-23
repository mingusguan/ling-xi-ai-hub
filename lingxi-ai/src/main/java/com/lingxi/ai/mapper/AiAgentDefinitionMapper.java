package com.lingxi.ai.mapper;

import com.lingxi.ai.agent.domain.AiAgentDefinition;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 智能体定义 Mapper。
 */
public interface AiAgentDefinitionMapper {

    /**
     * 查询智能体定义列表。
     */
    List<AiAgentDefinition> selectAgentList(@Param("agentCode") String agentCode,
            @Param("agentName") String agentName,
            @Param("status") String status);

    /**
     * 按主键查询智能体。
     */
    AiAgentDefinition selectAgentById(@Param("agentId") Long agentId);

    /**
     * 按编码查询智能体。
     */
    AiAgentDefinition selectAgentByCode(@Param("agentCode") String agentCode);

    /**
     * 新增智能体。
     */
    int insertAgent(AiAgentDefinition agent);

    /**
     * 更新智能体。
     */
    int updateAgent(AiAgentDefinition agent);

    /**
     * 更新智能体状态。
     */
    int updateAgentStatus(@Param("agentId") Long agentId, @Param("status") String status,
            @Param("updateBy") String updateBy);
}
