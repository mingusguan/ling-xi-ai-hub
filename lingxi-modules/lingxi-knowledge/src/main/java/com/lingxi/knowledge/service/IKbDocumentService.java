package com.lingxi.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lingxi.knowledge.domain.KbDocument;
import com.lingxi.knowledge.domain.dto.KnowledgeSearchResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IKbDocumentService extends IService<KbDocument> {

    /** 上传并入库（包含分块 + 向量化） */
    Long uploadDocument(MultipartFile file, String fileUrl, List<Long> visibleDeptIds, Long categoryId, List<Long> tagIds) throws Exception;

    /** 按部门/分类分页查询文档列表 */
    List<KbDocument> listByDept(Long deptId, Long categoryId);

    /** 删除文档（同时删除分块记录） */
    void deleteDocument(Long docId);

    /** 重新向量化（对已入库文档重新划分并 embed） */
    void reEmbed(Long docId);

    KnowledgeSearchResponse searchKnowledge(String query, Long deptId, Integer maxResults, Double minScore);
}
