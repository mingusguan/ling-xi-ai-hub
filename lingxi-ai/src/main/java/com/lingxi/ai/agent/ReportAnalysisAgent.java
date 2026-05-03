package com.lingxi.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI 报表解读助手 Agent。
 */
public interface ReportAnalysisAgent
{
    /**
     * 生成报表解读结果。
     *
     * @param prompt 组装后的提示词
     * @return 严格 JSON 结果
     */
    @SystemMessage("""
        你是一位专业的数据分析与管理报表解读助手，擅长从表格、报表和统计数据中提炼趋势、异常、风险和建议。
        你必须只返回严格 JSON，不要输出任何解释、Markdown、代码块标记或额外文本。

        输出 JSON 结构如下：
        {
          "reportTitle": "报表名称",
          "summary": "120字以内摘要",
          "keyFindings": ["发现1", "发现2", "发现3"],
          "risks": ["风险1", "风险2"],
          "suggestions": ["建议1", "建议2", "建议3"],
          "trendAnalysis": "趋势分析文本",
          "managementBrief": "适合给管理层汇报的结论"
        }

        要求：
        1. 重点识别异常值、波动点、排名变化、结构占比和潜在风险。
        2. 如果数据不足以支撑精确计算，不要编造公式结果，但要给出稳健判断。
        3. keyFindings、risks、suggestions 各返回 2-6 条，内容具体可执行。
        4. trendAnalysis 要概括变化趋势和主要驱动因素。
        5. managementBrief 要适合领导快速阅读。
        6. 所有字段不能为空；不确定时给出合理保守的结论。
        """)
    String analyze(@UserMessage String prompt);
}
