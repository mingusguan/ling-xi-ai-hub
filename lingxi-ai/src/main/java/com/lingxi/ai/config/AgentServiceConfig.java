package com.lingxi.ai.config;

import com.lingxi.ai.agent.ApprovalAssistantAgent;
import com.lingxi.ai.agent.XiaolingAgent;
import com.lingxi.ai.agent.tools.KnowledgeTool;
import com.lingxi.ai.agent.tools.OaTool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent服务配置类
 */
@Configuration
public class AgentServiceConfig {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private KnowledgeTool knowledgeTool;

    @Autowired
    private OaTool oaTool;

    /**
     * 创建小灵儿助手Agent
     */
    @Bean
    public XiaolingAgent xiaolingAgent() {
        return AiServices.builder(XiaolingAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(knowledgeTool, oaTool)
                .build();
    }

    /**
     * 创建审批助手Agent
     */
    @Bean
    public ApprovalAssistantAgent approvalAssistant() {
        return AiServices.builder(ApprovalAssistantAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(oaTool)
                .build();
    }
}
