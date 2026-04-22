package com.lingxi.knowledge.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus向量库配置
 */
@Configuration
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private Integer port;

    @Value("${milvus.collection-name:kb_embedding}")
    private String collectionName;

    @Value("${milvus.dimension:1024}")
    private Integer dimension;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return MilvusEmbeddingStore.builder()
                .host(host)
                .port(port)
                .collectionName(collectionName)
                .dimension(dimension)
                .autoFlushOnInsert(true)
                .build();
    }
}
