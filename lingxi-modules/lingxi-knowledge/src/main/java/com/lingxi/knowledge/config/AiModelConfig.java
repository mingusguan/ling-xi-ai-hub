package com.lingxi.knowledge.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI模型配置类
 *
 * @author lingxi
 */
@Configuration
public class AiModelConfig {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.model}")
    private String modelName;

    @Value("${dashscope.embedding-model}")
    private String embeddingModel;

    /**
     * 创建通义千问聊天模型
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.1f)
                .build();
    }

    /**
     * 通义向量模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModel)
                .build();
    }
}
