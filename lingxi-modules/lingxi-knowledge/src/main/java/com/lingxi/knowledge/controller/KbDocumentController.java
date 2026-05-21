package com.lingxi.knowledge.controller;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.common.security.annotation.Logical;
import com.lingxi.common.security.annotation.RequiresPermissions;
import com.lingxi.knowledge.domain.KbDocument;
import com.lingxi.knowledge.domain.dto.KbDocumentUploadRequest;
import com.lingxi.knowledge.service.IKbDocumentService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/knowledge/doc")
public class KbDocumentController extends BaseController
{
    @Resource
    private IKbDocumentService documentService;

    /**
     * 文档上传。
     */
    @RequiresPermissions(value = {"knowledge:document:add", "knowledge:document:list"}, logical = Logical.OR)
    @PostMapping("/upload")
    public R<?> upload(@Valid @ModelAttribute KbDocumentUploadRequest request, MultipartFile file) throws Exception
    {
        Long docId = documentService.uploadDocument(file, request.getFileUrl(), request.getVisibleDeptIds(), request.getCategoryId());
        return R.ok(Map.of("docId", docId, "status", 0));
    }

    /**
     * 文档列表。
     */
    @RequiresPermissions("knowledge:document:list")
    @GetMapping("/list")
    public TableDataInfo list(Long deptId, Long categoryId)
    {
        startPage();
        List<KbDocument> list = documentService.listByDept(deptId, categoryId);
        return getDataTable(list);
    }

    /**
     * 文档详情。
     */
    @RequiresPermissions("knowledge:document:list")
    @GetMapping("/{docId}")
    public R<?> detail(@PathVariable Long docId)
    {
        return R.ok(documentService.getById(docId));
    }

    /**
     * 删除文档。
     */
    @RequiresPermissions(value = {"knowledge:document:remove", "knowledge:document:list"}, logical = Logical.OR)
    @DeleteMapping("/{docId}")
    public R<?> delete(@PathVariable Long docId)
    {
        documentService.deleteDocument(docId);
        return R.ok();
    }

    /**
     * 重新向量化。
     */
    @RequiresPermissions(value = {"knowledge:document:edit", "knowledge:document:list"}, logical = Logical.OR)
    @PostMapping("/{docId}/re-embed")
    public R<?> reEmbed(@PathVariable Long docId)
    {
        documentService.reEmbed(docId);
        return R.ok();
    }
}
