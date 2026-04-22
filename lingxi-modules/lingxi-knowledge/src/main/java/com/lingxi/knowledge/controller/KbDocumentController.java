package com.lingxi.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.knowledge.domain.KbDocument;
import com.lingxi.knowledge.service.IKbDocumentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge/doc")
public class KbDocumentController extends BaseController {

    @Resource
    private IKbDocumentService documentService;

    /**
     * 文档上传（自动分块+向量入库）
     */
    @PostMapping("/upload")
    public R<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileUrl") String fileUrl,
            @RequestParam(value = "visibleDeptIds", required = false) List<Long> visibleDeptIds,
            @RequestParam Long categoryId,
            @RequestParam(required = false) List<Long> tagIds) throws Exception {
        Long docId = documentService.uploadDocument(file, fileUrl, visibleDeptIds, categoryId, tagIds);
        return R.ok(Map.of("docId", docId, "status", 0));
    }

    /**
     * 按部门查询文档列表（可按分类过滤）- 支持分页
     */
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long categoryId) {
        startPage();
        List<KbDocument> list = documentService.listByDept(deptId, categoryId);
        return getDataTable(list);
    }

    /**
     * 文档详情
     */
    @GetMapping("/{docId}")
    public R<?> detail(@PathVariable Long docId) {
        return R.ok(documentService.getById(docId));
    }

    /**
     * 删除文档（同时删除分块记录）
     */
    @DeleteMapping("/{docId}")
    public R<?> delete(@PathVariable Long docId) {
        documentService.deleteDocument(docId);
        return R.ok();
    }

    /**
     * 重新向量化（重新划分并 embed）
     */
    @PostMapping("/{docId}/re-embed")
    public R<?> reEmbed(@PathVariable Long docId) {
        documentService.reEmbed(docId);
        return R.ok();
    }
}
