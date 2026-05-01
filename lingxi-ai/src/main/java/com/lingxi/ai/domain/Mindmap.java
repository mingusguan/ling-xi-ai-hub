package com.lingxi.ai.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.lingxi.common.core.web.domain.BaseEntity;

/**
 * 思维导图主表 sys_mindmap
 * 
 * @author cloud
 */
public class Mindmap extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 思维导图ID */
    private Long mindmapId;

    /** 思维导图标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 用户输入的提示词 */
    private String userPrompt;

    /** 思维导图JSON数据 */
    private String dataJson;

    /** 布局类型：logical-逻辑图, mind-思维导图, org-组织结构图, fishbone-鱼骨图 */
    private String layoutType;

    /** 主题样式 */
    private String theme;

    /** 节点数量 */
    private Integer nodeCount;

    /** 状态（0正常 1停用） */
    private String status;

    public Long getMindmapId()
    {
        return mindmapId;
    }

    public void setMindmapId(Long mindmapId)
    {
        this.mindmapId = mindmapId;
    }

    @NotBlank(message = "标题不能为空")
    @Size(min = 0, max = 200, message = "标题长度不能超过200个字符")
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @Size(min = 0, max = 500, message = "描述长度不能超过500个字符")
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getUserPrompt()
    {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt)
    {
        this.userPrompt = userPrompt;
    }

    public String getDataJson()
    {
        return dataJson;
    }

    public void setDataJson(String dataJson)
    {
        this.dataJson = dataJson;
    }

    public String getLayoutType()
    {
        return layoutType;
    }

    public void setLayoutType(String layoutType)
    {
        this.layoutType = layoutType;
    }

    public String getTheme()
    {
        return theme;
    }

    public void setTheme(String theme)
    {
        this.theme = theme;
    }

    public Integer getNodeCount()
    {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount)
    {
        this.nodeCount = nodeCount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("mindmapId", getMindmapId())
            .append("title", getTitle())
            .append("description", getDescription())
            .append("userPrompt", getUserPrompt())
            .append("dataJson", getDataJson())
            .append("layoutType", getLayoutType())
            .append("theme", getTheme())
            .append("nodeCount", getNodeCount())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
