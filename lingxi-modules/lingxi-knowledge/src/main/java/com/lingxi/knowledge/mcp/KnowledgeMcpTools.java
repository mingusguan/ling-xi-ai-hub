package com.lingxi.knowledge.mcp;

import com.lingxi.knowledge.domain.dto.KnowledgeSearchResponse;
import com.lingxi.knowledge.service.IKbDocumentService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeMcpTools {

    private final IKbDocumentService documentService;

    public KnowledgeMcpTools(IKbDocumentService documentService) {
        this.documentService = documentService;
    }

    @Tool(
            name = "search_knowledge",
            description = "按语义相似度检索企业知识库内容片段。该工具为只读查询。")
    public KnowledgeSearchResponse searchKnowledge(
            @ToolParam(description = "检索关键词或问题文本。") String query,
            @ToolParam(description = "当前用户部门 ID，用于文档可见范围过滤。", required = false) Long deptId,
            @ToolParam(description = "返回结果最大数量，取值范围：1 到 10。", required = false) Integer maxResults,
            @ToolParam(description = "最小相似度阈值，取值范围：0.0 到 1.0。", required = false) Double minScore) {
        return documentService.searchKnowledge(query, deptId, maxResults, minScore);
    }
}
