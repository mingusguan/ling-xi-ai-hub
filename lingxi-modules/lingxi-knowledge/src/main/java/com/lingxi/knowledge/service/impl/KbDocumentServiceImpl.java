package com.lingxi.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.knowledge.domain.KbDocument;
import com.lingxi.knowledge.domain.KbDocumentChunk;
import com.lingxi.knowledge.mapper.KbDocumentChunkMapper;
import com.lingxi.knowledge.mapper.KbDocumentMapper;
import com.lingxi.knowledge.service.IKbDocumentService;
import com.lingxi.knowledge.util.DocumentUtil;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument> implements IKbDocumentService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long uploadDocument(MultipartFile file, String fileUrl, List<Long> visibleDeptIds, Long categoryId, List<Long> tagIds) throws Exception {
        // 1. 提取文本
        String fullText = DocumentUtil.extractText(file);
        
        // 2. 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        // 3. 入库（新增 categoryId）
        KbDocument document = new KbDocument();
        document.setDocName(originalFilename);
        document.setDocType(extension);  // 存储简短的扩展名
        document.setFileUrl(fileUrl);
        document.setFileSize(file.getSize());
        // 设置可见部门列表
        if (visibleDeptIds != null && !visibleDeptIds.isEmpty()) {
            document.setVisibleDeptIds(visibleDeptIds.stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "," + b)
                    .orElse(null));
        } else {
            // 公共文档不设置可见部门（NULL表示对所有部门可见）
            document.setVisibleDeptIds(null);
        }
        document.setCategoryId(categoryId); // 绑定分类
        document.setStatus(0);
        document.setCreateTime(LocalDateTime.now());
        document.setCreateUser(SecurityUtils.getUserId());
        // 4. 生成文档摘要
        String summary = generateSummary(fullText);
        document.setDocSummary(summary);
        save(document);
        // 5. 分块入库
        saveChunks(document.getDocId(), fullText);
            
        // 6. 向量化分块并写入 Milvus
        embedDocument(document);
        return document.getDocId();
    }

    /**
     * 从文件名中提取扩展名
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 将全文按固定窗口分块并持久化到 kb_document_chunk
     */
    @Override
    public List<KbDocument> listByDept(Long deptId, Long categoryId) {
        return documentMapper.selectListByDeptWithChildren(deptId, categoryId);
    }

    @Override
    public void deleteDocument(Long docId) {
        // 1. 查询该文档的所有分块，获取 Milvus ID
        List<KbDocumentChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<KbDocumentChunk>()
                        .eq(KbDocumentChunk::getDocId, docId)
        );

        // 2. 从 Milvus 中删除对应的向量数据
        if (!chunks.isEmpty()) {
            List<String> milvusIds = chunks.stream()
                    .map(KbDocumentChunk::getMilvusId)
                    .filter(id -> id != null && !id.isEmpty())
                    .toList();
            
            if (!milvusIds.isEmpty()) {
                try {
                    embeddingStore.removeAll(milvusIds);
                    log.info("[向量删除] 从 Milvus 删除 {} 条向量, docId={}", milvusIds.size(), docId);
                } catch (Exception e) {
                    log.error("[向量删除] Milvus 删除失败, docId={}", docId, e);
                }
            }
        }

        // 3. 删除数据库中的分块记录
        chunkMapper.delete(new LambdaQueryWrapper<KbDocumentChunk>()
                .eq(KbDocumentChunk::getDocId, docId));
        
        // 4. 逻辑删除文档
        removeById(docId);
        log.info("[文档删除] docId={}", docId);
    }

    @Override
    public void reEmbed(Long docId) {
        KbDocument document = getById(docId);
        if (document == null) {
            throw new RuntimeException("文档不存在: " + docId);
        }
        chunkMapper.delete(new LambdaQueryWrapper<KbDocumentChunk>()
                .eq(KbDocumentChunk::getDocId, docId));
        try {
            String fullText = DocumentUtil.extractTextFromUrl(document.getFileUrl());
            saveChunks(docId, fullText);
        } catch (Exception e) {
            throw new RuntimeException("文档文本重新提取失败", e);
        }
        embedDocument(document);
        log.info("[重新向量化]完成, docId={}", docId);
    }

    private void saveChunks(Long docId, String fullText) {
        if (fullText == null || fullText.isBlank()) {
            log.warn("[KbDocumentService] 文档文本为空, docId={}", docId);
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
        log.info("[KbDocumentService] 分块入库完成, docId={}, 共 {} 块", docId, chunks.size());
    }

    /**
     * 文档向量化
     */
    public void embedDocument(KbDocument document) {
        // 1. 查询待向量化的分块
        List<KbDocumentChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<KbDocumentChunk>()
                        .eq(KbDocumentChunk::getDocId, document.getDocId())
                        .eq(KbDocumentChunk::getEmbedStatus, 0)
                        .orderByAsc(KbDocumentChunk::getChunkIndex)
        );

        if (chunks.isEmpty()) {
            log.info("[KbDocumentParseService] 无待向量化分块, docId={}", document.getDocId());
            return;
        }

        // 3. 批量向量化并写入 Milvus（携带 dept_id + document_id metadata）
        List<Embedding> embeddings = new ArrayList<>();
        List<TextSegment> segments = new ArrayList<>();

        for (KbDocumentChunk chunk : chunks) {
            Metadata metadata = new Metadata();
            metadata.put("document_id", String.valueOf(document.getDocId()));
            // 只有当 visibleDeptIds 不为 null 时才存储
            if (document.getVisibleDeptIds() != null) {
                metadata.put("visible_dept_ids", document.getVisibleDeptIds());
            }
            metadata.put("chunk_id", String.valueOf(chunk.getChunkId()));

            TextSegment segment = TextSegment.from(chunk.getChunkContent(), metadata);
            Embedding embedding = Embedding.from(embeddingModel.embed(chunk.getChunkContent()).content().vector());

            embeddings.add(embedding);
            segments.add(segment);
        }

        List<String> milvusIds = embeddingStore.addAll(embeddings, segments);

        // 4. 更新分块的 embedStatus 和 milvusId
        for (int i = 0; i < chunks.size(); i++) {
            KbDocumentChunk chunk = chunks.get(i);
            chunk.setEmbedStatus(1);
            chunk.setMilvusId(milvusIds.get(i));
            chunkMapper.updateById(chunk);
        }

        // 5. 更新文档状态为"已入库"
        KbDocument update = new KbDocument();
        update.setDocId(document.getDocId());
        update.setStatus(2);
        documentMapper.updateById(update);

        log.info("[KbDocumentParseService] 文档向量化完成，docId={}, visible_dept_ids={}, chunks={}",
                document.getDocId(), document.getVisibleDeptIds(), chunks.size());
    }

    /**
     * 使用 AI 模型生成文档摘要
     */
    private String generateSummary(String fullText) {
        try {
            if (fullText == null || fullText.isBlank()) {
                return "";
            }
            
            // 截取前 2000 字符用于生成摘要（避免 token 超限）
            String textForSummary = fullText.length() > 2000 ? fullText.substring(0, 2000) : fullText;
            
            String prompt = "请为以下文档内容生成一段简洁的摘要（100-200 字），概括文档的主要内容：\n\n" + textForSummary;
            
            String summary = chatLanguageModel.generate(prompt);
            
            log.info("[文档摘要] 生成成功，长度：{}", summary.length());
            return summary.trim();
        } catch (Exception e) {
            log.error("[文档摘要] 生成失败", e);
            return "";
        }
    }
}
