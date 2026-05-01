package com.lingxi.ai.config;

import com.lingxi.ai.agent.ApprovalAssistantAgent;
import com.lingxi.ai.agent.DocumentWritingAgent;
import com.lingxi.ai.agent.MindmapAgent;
import com.lingxi.ai.agent.ReportAnalysisAgent;
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

    /**
     * 创建思维导图生成Agent
     */
    @Bean
    public MindmapAgent mindmapAgent() {
        return AiServices.builder(MindmapAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }

    /**
     * 创建公文助手 Agent
     */
    @Bean
    public DocumentWritingAgent documentWritingAgent() {
        return AiServices.builder(DocumentWritingAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }

    /**
     * 创建报表解读 Agent
     */
    @Bean
    public ReportAnalysisAgent reportAnalysisAgent() {
        return AiServices.builder(ReportAnalysisAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }
}
