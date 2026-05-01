package com.lingxi.ai.domain.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 报表解读结果对象。
 */
public class ReportInterpretResult
{
    private String reportTitle;

    private String summary;

    private List<String> keyFindings = new ArrayList<>();

    private List<String> risks = new ArrayList<>();

    private List<String> suggestions = new ArrayList<>();

    private String trendAnalysis;

    private String managementBrief;

    private String mindmapPrompt;

    private String parsedTablePreview;

    private AiMindmapPayload mindmap;

    public String getReportTitle()
    {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle)
    {
        this.reportTitle = reportTitle;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public List<String> getKeyFindings()
    {
        return keyFindings;
    }

    public void setKeyFindings(List<String> keyFindings)
    {
        this.keyFindings = keyFindings;
    }

    public List<String> getRisks()
    {
        return risks;
    }

    public void setRisks(List<String> risks)
    {
        this.risks = risks;
    }

    public List<String> getSuggestions()
    {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions)
    {
        this.suggestions = suggestions;
    }

    public String getTrendAnalysis()
    {
        return trendAnalysis;
    }

    public void setTrendAnalysis(String trendAnalysis)
    {
        this.trendAnalysis = trendAnalysis;
    }

    public String getManagementBrief()
    {
        return managementBrief;
    }

    public void setManagementBrief(String managementBrief)
    {
        this.managementBrief = managementBrief;
    }

    public String getMindmapPrompt()
    {
        return mindmapPrompt;
    }

    public void setMindmapPrompt(String mindmapPrompt)
    {
        this.mindmapPrompt = mindmapPrompt;
    }

    public String getParsedTablePreview()
    {
        return parsedTablePreview;
    }

    public void setParsedTablePreview(String parsedTablePreview)
    {
        this.parsedTablePreview = parsedTablePreview;
    }

    public AiMindmapPayload getMindmap()
    {
        return mindmap;
    }

    public void setMindmap(AiMindmapPayload mindmap)
    {
        this.mindmap = mindmap;
    }
}
