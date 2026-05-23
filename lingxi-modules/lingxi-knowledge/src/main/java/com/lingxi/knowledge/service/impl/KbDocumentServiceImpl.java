package com.lingxi.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.knowledge.domain.KbDocument;
import com.lingxi.knowledge.domain.KbDocumentChunk;
import com.lingxi.knowledge.domain.dto.KnowledgeSearchResponse;
import com.lingxi.knowledge.domain.dto.KnowledgeSearchResult;
import com.lingxi.knowledge.mapper.KbDocumentChunkMapper;
import com.lingxi.knowledge.mapper.KbDocumentMapper;
import com.lingxi.knowledge.service.IKbDocumentService;
import com.lingxi.knowledge.util.DocumentUtil;
import com.lingxi.common.core.utils.file.FileUrlUtils;
import com.lingxi.system.api.RemoteFileService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument> implements IKbDocumentService
{
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int DEFAULT_SEARCH_LIMIT = 5;
    private static final int MAX_SEARCH_LIMIT = 10;
    private static final double DEFAULT_MIN_SCORE = 0.7D;

    @Resource
    private KbDocumentChunkMapper chunkMapper;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Resource
    private KbDocumentMapper documentMapper;

    @Resource
    private ChatLanguageModel chatLanguageModel;

    @Resource
    private RemoteFileService remoteFileService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long uploadDocument(MultipartFile file, String fileUrl, List<Long> visibleDeptIds, Long categoryId) throws Exception
    {
        String fullText = DocumentUtil.extractText(file);
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        KbDocument document = new KbDocument();
        document.setDocName(originalFilename);
        document.setDocType(extension);
        document.setFileUrl(normalizeFileUrl(fileUrl));
        document.setFileSize(file.getSize());
        document.setVisibleDeptIds(joinDeptIds(visibleDeptIds));
        document.setCategoryId(categoryId);
        document.setStatus(0);
        document.setCreateTime(LocalDateTime.now());
        document.setCreateUser(SecurityUtils.getUserId());
        document.setDocSummary(generateSummary(fullText));
        save(document);

        saveChunks(document.getDocId(), fullText);
        embedDocument(document);
        return document.getDocId();
    }

    @Override
    public List<KbDocument> listByDept(Long deptId, Long categoryId)
    {
        return documentMapper.selectListByDeptWithChildren(deptId, categoryId);
    }

    @Override
    public void deleteDocument(Long docId)
    {
        List<KbDocumentChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<KbDocumentChunk>()
                .eq(KbDocumentChunk::getDocId, docId));

        if (!chunks.isEmpty()) {
            List<String> milvusIds = chunks.stream()
                    .map(KbDocumentChunk::getMilvusId)
                    .filter(id -> id != null && !id.isEmpty())
                    .toList();
            if (!milvusIds.isEmpty()) {
                try {
                    embeddingStore.removeAll(milvusIds);
                    log.info("[知识库] 删除向量完成, docId={}, count={}", docId, milvusIds.size());
                } catch (Exception e) {
                    log.error("[知识库] 删除向量失败, docId={}", docId, e);
                }
            }
        }

        chunkMapper.delete(new LambdaQueryWrapper<KbDocumentChunk>().eq(KbDocumentChunk::getDocId, docId));
        removeById(docId);
        log.info("[知识库] 删除文档完成, docId={}", docId);
    }

    @Override
    public void reEmbed(Long docId)
    {
        KbDocument document = getById(docId);
        if (document == null) {
            throw new ServiceException("文档不存在");
        }

        chunkMapper.delete(new LambdaQueryWrapper<KbDocumentChunk>().eq(KbDocumentChunk::getDocId, docId));
        try {
            String fullText = DocumentUtil.extractTextFromUrl(document.getFileUrl());
            saveChunks(docId, fullText);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("文档文本重新提取失败");
        }
        embedDocument(document);
        log.info("[知识库] 重新向量化完成, docId={}", docId);
    }

    @Override
    public KnowledgeSearchResponse searchKnowledge(String query, Long deptId, Integer maxResults, Double minScore)
    {
        if (query == null || query.isBlank()) {
            return new KnowledgeSearchResponse(query, 0, Collections.emptyList());
        }

        int limit = normalizeLimit(maxResults);
        double threshold = normalizeMinScore(minScore);
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(limit * 2)
                .minScore(threshold)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(request);
        List<KnowledgeSearchResult> results = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
            TextSegment segment = match.embedded();
            if (!isVisibleToDept(segment, deptId)) {
                continue;
            }
            results.add(toSearchResult(match));
            if (results.size() >= limit) {
                break;
            }
        }
        return new KnowledgeSearchResponse(query, results.size(), results);
    }

    private String joinDeptIds(List<Long> visibleDeptIds)
    {
        if (visibleDeptIds == null || visibleDeptIds.isEmpty()) {
            return null;
        }
        return visibleDeptIds.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse(null);
    }

    private String normalizeFileUrl(String fileUrl)
    {
        if (StringUtils.isBlank(fileUrl)) {
            throw new ServiceException("文件地址不能为空");
        }
        R<String> result = remoteFileService.normalize(fileUrl);
        if (R.isError(result)) {
            throw new ServiceException(StringUtils.defaultString(result.getMsg(), "文件地址规范化失败"));
        }
        String normalized = FileUrlUtils.normalizeForStorage(result.getData());
        if (StringUtils.isBlank(normalized)) {
            throw new ServiceException("文件地址规范化失败");
        }
        if (StringUtils.contains(normalized, "://") || StringUtils.contains(normalized, "?")) {
            throw new ServiceException("文件地址不能保存域名或临时签名参数");
        }
        return normalized;
    }

    private int normalizeLimit(Integer maxResults)
    {
        if (maxResults == null) {
            return DEFAULT_SEARCH_LIMIT;
        }
        return Math.max(1, Math.min(maxResults, MAX_SEARCH_LIMIT));
    }

    private double normalizeMinScore(Double minScore)
    {
        if (minScore == null) {
            return DEFAULT_MIN_SCORE;
        }
        return Math.max(0D, Math.min(minScore, 1D));
    }

    private boolean isVisibleToDept(TextSegment segment, Long deptId)
    {
        if (segment.metadata() == null) {
            return true;
        }
        String visibleDeptIds = segment.metadata().getString("visible_dept_ids");
        if (visibleDeptIds == null || visibleDeptIds.isBlank()) {
            return true;
        }
        if (deptId == null) {
            return false;
        }
        String currentDeptId = String.valueOf(deptId);
        for (String id : visibleDeptIds.split(",")) {
            if (currentDeptId.equals(id.trim())) {
                return true;
            }
        }
        return false;
    }

    private KnowledgeSearchResult toSearchResult(EmbeddingMatch<TextSegment> match)
    {
        TextSegment segment = match.embedded();
        String documentId = null;
        String chunkId = null;
        if (segment.metadata() != null) {
            documentId = segment.metadata().getString("document_id");
            chunkId = segment.metadata().getString("chunk_id");
        }
        return new KnowledgeSearchResult(documentId, chunkId, match.score(), segment.text());
    }

    private String getExtension(String filename)
    {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private void saveChunks(Long docId, String fullText)
    {
        if (fullText == null || fullText.isBlank()) {
            log.warn("[知识库] 文档文本为空, docId={}", docId);
            return;
        }

        List<KbDocumentChunk> chunks = new ArrayList<>();
        int index = 0;
        int start = 0;
        while (start < fullText.length()) {
            int end = Math.min(start + CHUNK_SIZE, fullText.length());
            String content = fullText.substring(start, end).trim();
            if (!content.isEmpty()) {
                KbDocumentChunk chunk = new KbDocumentChunk();
                chunk.setDocId(docId);
                chunk.setChunkContent(content);
                chunk.setChunkIndex(index++);
                chunk.setEmbedStatus(0);
                chunk.setCreateTime(LocalDateTime.now());
                chunks.add(chunk);
            }
            start += CHUNK_SIZE - CHUNK_OVERLAP;
        }

        chunks.forEach(chunkMapper::insert);
        log.info("[知识库] 分块入库完成, docId={}, chunkCount={}", docId, chunks.size());
    }

    public void embedDocument(KbDocument document)
    {
        List<KbDocumentChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<KbDocumentChunk>()
                .eq(KbDocumentChunk::getDocId, document.getDocId())
                .eq(KbDocumentChunk::getEmbedStatus, 0)
                .orderByAsc(KbDocumentChunk::getChunkIndex));
        if (chunks.isEmpty()) {
            log.info("[知识库] 无待向量化分块, docId={}", document.getDocId());
            return;
        }

        List<Embedding> embeddings = new ArrayList<>();
        List<TextSegment> segments = new ArrayList<>();
        for (KbDocumentChunk chunk : chunks) {
            Metadata metadata = new Metadata();
            metadata.put("document_id", String.valueOf(document.getDocId()));
            if (document.getVisibleDeptIds() != null) {
                metadata.put("visible_dept_ids", document.getVisibleDeptIds());
            }
            metadata.put("chunk_id", String.valueOf(chunk.getChunkId()));

            segments.add(TextSegment.from(chunk.getChunkContent(), metadata));
            embeddings.add(Embedding.from(embeddingModel.embed(chunk.getChunkContent()).content().vector()));
        }

        List<String> milvusIds = embeddingStore.addAll(embeddings, segments);
        for (int i = 0; i < chunks.size(); i++) {
            KbDocumentChunk chunk = chunks.get(i);
            chunk.setEmbedStatus(1);
            chunk.setMilvusId(milvusIds.get(i));
            chunkMapper.updateById(chunk);
        }

        KbDocument update = new KbDocument();
        update.setDocId(document.getDocId());
        update.setStatus(2);
        documentMapper.updateById(update);
        log.info("[知识库] 向量化完成, docId={}, chunkCount={}", document.getDocId(), chunks.size());
    }

    private String generateSummary(String fullText)
    {
        try {
            if (fullText == null || fullText.isBlank()) {
                return "";
            }
            String textForSummary = fullText.length() > 2000 ? fullText.substring(0, 2000) : fullText;
            String prompt = "请为以下文档内容生成一段简洁的摘要（100-200字），概括文档的主要内容：\n\n" + textForSummary;
            String summary = chatLanguageModel.generate(prompt);
            return summary == null ? "" : summary.trim();
        } catch (Exception e) {
            log.error("[知识库] 生成文档摘要失败", e);
            return "";
        }
    }
}
