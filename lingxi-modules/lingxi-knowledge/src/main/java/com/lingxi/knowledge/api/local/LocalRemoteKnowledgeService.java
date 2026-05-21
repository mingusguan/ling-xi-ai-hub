package com.lingxi.knowledge.api.local;

import com.lingxi.common.core.domain.R;
import com.lingxi.knowledge.api.RemoteKnowledgeService;
import com.lingxi.knowledge.domain.dto.KnowledgeSearchResponse;
import com.lingxi.knowledge.service.IKbDocumentService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 单体模式下的本地知识库服务调用。
 */
@Service
@RequiredArgsConstructor
public class LocalRemoteKnowledgeService implements RemoteKnowledgeService {

    private final IKbDocumentService documentService;

    @Override
    public R<Map<String, Object>> askQuestion(Map<String, Object> request) {
        String question = String.valueOf(request.getOrDefault("question", request.getOrDefault("query", "")));
        Long deptId = request.get("deptId") instanceof Number number ? number.longValue() : null;
        KnowledgeSearchResponse response = documentService.searchKnowledge(question, deptId, 5, 0.0);
        Map<String, Object> result = new HashMap<>();
        result.put("query", response.query());
        result.put("total", response.total());
        result.put("results", response.results());
        return R.ok(result);
    }
}
