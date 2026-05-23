package com.lingxi.ai.agent.domain.vo;

import com.lingxi.ai.agent.domain.AiAgentDefinition;
import com.lingxi.ai.agent.domain.AiAgentStep;
import com.lingxi.ai.mcp.domain.McpToolBinding;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 智能体详情。
 */
@Data
public class AiAgentDetailVO {

    /** 智能体定义。 */
    private AiAgentDefinition agent;

    /** 编排步骤。 */
    private List<AiAgentStep> steps = new ArrayList<>();

    /** 已绑定的MCP工具。 */
    private List<McpToolBinding> bindings = new ArrayList<>();
}
