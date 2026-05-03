package com.lingxi.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 报表解读请求对象。
 */
public class ReportInterpretRequest
{
    @NotBlank(message = "报表名称不能为空")
    @Size(max = 200, message = "报表名称长度不能超过200个字符")
    private String reportTitle;

    @Size(max = 1000, message = "分析重点长度不能超过1000个字符")
    private String analysisFocus;

    @Size(max = 2000, message = "业务背景长度不能超过2000个字符")
    private String businessContext;

    @Size(max = 20000, message = "表格内容长度不能超过20000个字符")
    private String tableContent;

    public String getReportTitle()
    {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle)
    {
        this.reportTitle = reportTitle;
    }

    public String getAnalysisFocus()
    {
        return analysisFocus;
    }

    public void setAnalysisFocus(String analysisFocus)
    {
        this.analysisFocus = analysisFocus;
    }

    public String getBusinessContext()
    {
        return businessContext;
    }

    public void setBusinessContext(String businessContext)
    {
        this.businessContext = businessContext;
    }

    public String getTableContent()
    {
        return tableContent;
    }

    public void setTableContent(String tableContent)
    {
        this.tableContent = tableContent;
    }

}
