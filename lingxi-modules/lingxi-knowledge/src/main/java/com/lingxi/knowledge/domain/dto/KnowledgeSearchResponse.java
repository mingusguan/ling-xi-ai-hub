package com.lingxi.knowledge.domain.dto;

import java.util.List;

public record KnowledgeSearchResponse(
        String query,
        Integer total,
        List<KnowledgeSearchResult> results) {
}
