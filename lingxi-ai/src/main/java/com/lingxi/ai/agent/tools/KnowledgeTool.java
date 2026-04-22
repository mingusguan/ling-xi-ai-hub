package com.lingxi.ai.agent.tools;

import com.lingxi.ai.context.AgentContext;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库查询工具
 */
@Slf4j
@Component
public class KnowledgeTool {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public KnowledgeTool(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * 在知识库中搜索相关内容
     * Agent会自主决定是否调用此工具
     */
    @Tool("搜索企业知识库中与查询相关的文档内容，返回最相关的知识片段。会自动根据用户所在部门过滤权限。")
    public String searchKnowledge(String query, Long deptId) {
        return searchKnowledgeInternal(query, deptId, 5, 0.7);
    }

    /**
     * 执行向量检索
     */
    private String searchKnowledgeInternal(String query, Long deptId, int maxResults, double minScore) {
        try {
            log.info("Agent调用知识库检索工具，query={}, deptId={}", query, deptId);

            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // 查询所有文档，然后在Java层过滤
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults * 2)  // 多查一些，过滤后可能不够
                    .minScore(minScore)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(request);
            
            List<TextSegment> allMatches = new ArrayList<>();
            Map<String, Double> embeddingScores = new HashMap<>();

            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                TextSegment segment = match.embedded();
                
                // 检查文档权限
                boolean hasPermission = false;
                if (segment.metadata() != null) {
                    String visibleDeptIds = segment.metadata().getString("visible_dept_ids");
                    
                    // 如果 visible_dept_ids 为 null，表示公共文档，所有人可见
                    if (visibleDeptIds == null || visibleDeptIds.isEmpty()) {
                        hasPermission = true;
                    } else if (deptId != null) {
                        // 检查当前部门是否在可见部门列表中
                        String[] deptIdArray = visibleDeptIds.split(",");
                        for (String id : deptIdArray) {
                            if (String.valueOf(deptId).equals(id.trim())) {
                                hasPermission = true;
                                break;
                            }
                        }
                    }
                }
                
                // 如果有权限，则保留
                if (hasPermission) {
                    if (!embeddingScores.containsKey(match.embeddingId())) {
                        allMatches.add(segment);
                        embeddingScores.put(match.embeddingId(), match.score());
                    }
                }
            }

            // 3. 构建返回结果
            StringBuilder sb = new StringBuilder();
            for (TextSegment segment : allMatches) {
                String content = segment.text();
                sb.append(content).append("\n---\n");

                Map<String, Object> source = new HashMap<>();
                source.put("content", content);
                if (segment.metadata() != null) {
                    source.put("documentId", segment.metadata().getString("document_id"));
                }
                AgentContext.addSource(source);
            }

            log.info("知识库检索完成，返回{}条结果", AgentContext.getSources().size());
            String result = sb.toString();
            if (result.isBlank()) {
                return "未在知识库中找到相关内容";
            }
            return result;

        } catch (Exception e) {
            log.error("知识库检索失败", e);
            return "知识库检索失败：" + e.getMessage();
        }
    }
}
