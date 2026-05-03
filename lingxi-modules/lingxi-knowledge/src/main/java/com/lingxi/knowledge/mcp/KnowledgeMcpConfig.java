package com.lingxi.knowledge.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeMcpConfig {

    @Bean
    public ToolCallbackProvider knowledgeToolCallbackProvider(KnowledgeMcpTools knowledgeMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(knowledgeMcpTools)
                .build();
    }
}
