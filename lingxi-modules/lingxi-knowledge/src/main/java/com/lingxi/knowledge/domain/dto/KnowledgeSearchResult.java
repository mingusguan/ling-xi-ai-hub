package com.lingxi.knowledge.domain.dto;

public record KnowledgeSearchResult(
        String documentId,
        String chunkId,
        Double score,
        String content) {
}
