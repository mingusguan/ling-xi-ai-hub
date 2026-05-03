package com.lingxi.ai.domain.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 公文助手结果对象。
 */
public class DocumentWritingResult
{
    private String title;

    private String summary;

    private String documentType;

    private String tone;

    private String content;

    private List<String> outline = new ArrayList<>();

    private List<String> polishPoints = new ArrayList<>();

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
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

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public List<String> getOutline()
    {
        return outline;
    }

    public void setOutline(List<String> outline)
    {
        this.outline = outline == null ? Collections.emptyList() : outline;
    }

    public List<String> getPolishPoints()
    {
        return polishPoints;
    }

    public void setPolishPoints(List<String> polishPoints)
    {
        this.polishPoints = polishPoints == null ? Collections.emptyList() : polishPoints;
    }

}
