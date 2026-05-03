package com.lingxi.oa.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OaMcpConfig {

    @Bean
    public ToolCallbackProvider oaToolCallbackProvider(OaMcpTools oaMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(oaMcpTools)
                .build();
    }
}
