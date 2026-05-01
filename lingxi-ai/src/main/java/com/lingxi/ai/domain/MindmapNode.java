package com.lingxi.ai.domain;

import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.lingxi.common.core.web.domain.BaseEntity;

/**
 * 思维导图节点表 sys_mindmap_node
 * 
 * @author cloud
 */
public class MindmapNode extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 节点ID */
    private Long nodeId;

    /** 思维导图ID */
    private Long mindmapId;

    /** 父节点ID */
    private Long parentId;

    /** 节点文本内容 */
    private String nodeText;

    /** 节点层级 */
    private Integer nodeLevel;

    /** 同级排序 */
    private Integer nodeOrder;

    /** 背景颜色 */
    private String backgroundColor;

    /** 前景颜色 */
    private String foregroundColor;

    /** 字体大小 */
    private Integer fontSize;

    /** 是否展开（0收起 1展开） */
    private String isExpanded;

    public Long getNodeId()
    {
        return nodeId;
    }

    public void setNodeId(Long nodeId)
    {
        this.nodeId = nodeId;
    }

    public Long getMindmapId()
    {
        return mindmapId;
    }

    public void setMindmapId(Long mindmapId)
    {
        this.mindmapId = mindmapId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    @NotBlank(message = "节点内容不能为空")
    public String getNodeText()
    {
        return nodeText;
    }

    public void setNodeText(String nodeText)
    {
        this.nodeText = nodeText;
    }

    public Integer getNodeLevel()
    {
        return nodeLevel;
    }

    public void setNodeLevel(Integer nodeLevel)
    {
        this.nodeLevel = nodeLevel;
    }

    public Integer getNodeOrder()
    {
        return nodeOrder;
    }

    public void setNodeOrder(Integer nodeOrder)
    {
        this.nodeOrder = nodeOrder;
    }

    public String getBackgroundColor()
    {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }

    public String getForegroundColor()
    {
        return foregroundColor;
    }

    public void setForegroundColor(String foregroundColor)
    {
        this.foregroundColor = foregroundColor;
    }

    public Integer getFontSize()
    {
        return fontSize;
    }

    public void setFontSize(Integer fontSize)
    {
        this.fontSize = fontSize;
    }

    public String getIsExpanded()
    {
        return isExpanded;
    }

    public void setIsExpanded(String isExpanded)
    {
        this.isExpanded = isExpanded;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("nodeId", getNodeId())
            .append("mindmapId", getMindmapId())
            .append("parentId", getParentId())
            .append("nodeText", getNodeText())
            .append("nodeLevel", getNodeLevel())
            .append("nodeOrder", getNodeOrder())
            .append("backgroundColor", getBackgroundColor())
            .append("foregroundColor", getForegroundColor())
            .append("fontSize", getFontSize())
            .append("isExpanded", getIsExpanded())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
