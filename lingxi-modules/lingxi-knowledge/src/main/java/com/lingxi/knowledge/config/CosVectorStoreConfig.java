package com.lingxi.knowledge.config;

import com.lingxi.knowledge.vector.CosVectorEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 向量桶存储装配。
 */
@Configuration
@ConditionalOnProperty(prefix = "knowledge", name = "vector-store", havingValue = "cos-vector", matchIfMissing = true)
public class CosVectorStoreConfig {

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(CosVectorConfig cosVectorConfig) {
        return new CosVectorEmbeddingStore(cosVectorConfig);
    }
}
