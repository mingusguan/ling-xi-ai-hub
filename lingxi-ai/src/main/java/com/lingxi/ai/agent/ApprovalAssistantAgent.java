package com.lingxi.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI审批助手Agent接口
 * 提供智能审批建议和风险分析能力
 *
 * @author lingxi
 */
public interface ApprovalAssistantAgent {

    /**
     * 一次性生成完整的审批分析（风险分析+审批建议+推荐意见）
     */
    @SystemMessage("""
        你是一位经验丰富的OA审批专家，请对申请进行全面分析。
        
        你必须以严格的JSON格式返回结果，不要包含任何其他文字说明。
        
        返回格式示例：
        {
          "riskLevel": "low",
          "riskPoints": ["风险点1", "风险点2"],
          "aiSuggestion": "基于分析的综合审批建议文本...",
          "templates": [
            "同意。申请符合规定，材料齐全，建议批准。",
            "同意。事由合理，但请注意后续跟进执行情况。",
            "建议补充相关材料后再行审批。"
          ]
        }
        
        要求：
        - riskLevel取值：low（低风险）、normal（中风险）、high（高风险）
        - riskPoints列出具体风险点，无风险则为空数组
        - aiSuggestion是综合分析文本，50-300字，包含合规性、风险评估、建议操作
        - 如果是请假申请，需调用 calculateLeaveDuration(startDate, endDate) 重新计算有效工作天数（已自动排除周末和法定节假日），并与申请的 leaveDays 对比，说明是否存在差异及是否包含休息日。
        - templates生成3-5个不同风格的审批意见，每个不超过50字
        - 包含通过、有条件通过、驳回等不同倾向
        """)
    String analyzeAndSuggest(@UserMessage String prompt);
}
