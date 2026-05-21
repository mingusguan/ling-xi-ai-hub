package com.lingxi.knowledge.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 知识库文档上传请求参数。
 */
public class KbDocumentUploadRequest
{
    /**
     * 文件访问地址。
     */
    @NotBlank(message = "文件地址不能为空")
    private String fileUrl;

    /**
     * 文档分类 ID。
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 可见部门 ID 列表。
     */
    private List<Long> visibleDeptIds;

    public String getFileUrl()
    {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl)
    {
        this.fileUrl = fileUrl;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public List<Long> getVisibleDeptIds()
    {
        return visibleDeptIds;
    }

    public void setVisibleDeptIds(List<Long> visibleDeptIds)
    {
        this.visibleDeptIds = visibleDeptIds;
    }
}
