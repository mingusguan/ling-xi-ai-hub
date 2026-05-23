package com.lingxi.ai.mapper;

import com.lingxi.ai.agent.domain.AiAgentStep;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 智能体编排步骤 Mapper。
 */
public interface AiAgentStepMapper {

    /**
     * 查询智能体步骤列表。
     */
    List<AiAgentStep> selectStepsByAgentCode(@Param("agentCode") String agentCode);

    /**
     * 按主键查询步骤。
     */
    AiAgentStep selectStepById(@Param("stepId") Long stepId);

    /**
     * 新增步骤。
     */
    int insertStep(AiAgentStep step);

    /**
     * 更新步骤。
     */
    int updateStep(AiAgentStep step);

    /**
     * 删除步骤。
     */
    int deleteStepById(@Param("stepId") Long stepId);
}
