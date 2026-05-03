package com.lingxi.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 公文助手请求对象。
 */
public class DocumentWritingRequest
{
    @NotBlank(message = "处理模式不能为空")
    private String mode;

    @NotBlank(message = "文稿类型不能为空")
    private String documentType;

    @NotBlank(message = "语气风格不能为空")
    private String tone;

    @NotBlank(message = "写作主题不能为空")
    @Size(max = 500, message = "写作主题长度不能超过500个字符")
    private String topic;

    @Size(max = 3000, message = "背景信息长度不能超过3000个字符")
    private String background;

    @Size(max = 12000, message = "原始内容长度不能超过12000个字符")
    private String sourceContent;

    @Size(max = 3000, message = "附加要求长度不能超过3000个字符")
    private String requirements;

    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        this.mode = mode;
    }

    public String getDocumentType()
    {
        return documentType;
    }

    public void setDocumentType(String documentType)
    {
        this.documentType = documentType;
    }

    public String getTone()
    {
        return tone;
    }

    public void setTone(String tone)
    {
        this.tone = tone;
    }

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }

    public String getBackground()
    {
        return background;
    }

    public void setBackground(String background)
    {
        this.background = background;
    }

    public String getSourceContent()
    {
        return sourceContent;
    }

    public void setSourceContent(String sourceContent)
    {
        this.sourceContent = sourceContent;
    }

    public String getRequirements()
    {
        return requirements;
    }

    public void setRequirements(String requirements)
    {
        this.requirements = requirements;
    }

}
