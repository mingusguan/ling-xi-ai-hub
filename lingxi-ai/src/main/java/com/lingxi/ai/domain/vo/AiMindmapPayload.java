package com.lingxi.ai.domain.vo;

/**
 * 思维导图附加结果。
 */
public class AiMindmapPayload
{
    private String dataJson;

    private String layoutType;

    private String topic;

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

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }
}
