package com.lingxi.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lingxi.ai.agent.ApprovalAssistantAgent;
import com.lingxi.ai.service.IAiModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 通义千问AI模型服务实现
 *
 * @author lingxi
 */
@Service
public class QwenAiModelServiceImpl implements IAiModelService {

    private static final Logger log = LoggerFactory.getLogger(QwenAiModelServiceImpl.class);

    @Autowired
    private ApprovalAssistantAgent approvalAssistant;

    @Override
    public String analyzeAndSuggest(Map<String, Object> context) {
        String prompt = buildApprovalPrompt(context);
        try {
            String result = approvalAssistant.analyzeAndSuggest(prompt);
            try {
                JSONObject json = JSON.parseObject(result);
                if (json.containsKey("riskLevel") && json.containsKey("aiSuggestion") && json.containsKey("templates")) {
                    return result;
                } else {
                    log.warn("AI返回结果缺少必需字段");
                    return buildDefaultAnalyzeResult();
                }
            } catch (Exception e) {
                log.warn("AI返回的不是有效JSON");
                return buildDefaultAnalyzeResult();
            }
        } catch (Exception e) {
            log.error("完整审批分析失败", e);
            return buildDefaultAnalyzeResult();
        }
    }

    private String buildDefaultAnalyzeResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("riskLevel", "normal");
        result.put("riskPoints", new String[]{"请人工审核"});
        result.put("aiSuggestion", "AI分析失败，请人工审核该申请。");
        result.put("templates", new String[]{"同意", "驳回", "补充材料后重新提交"});
        return JSON.toJSONString(result);
    }

    private String buildApprovalPrompt(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位经验丰富的OA审批专家，请根据以下信息提供审批建议：\n\n");

        String businessType = (String) context.get("businessType");
        if ("leave".equals(businessType)) {
            sb.append("【请假申请】\n");
            sb.append("申请人：").append(context.get("applicantName")).append("\n");
            sb.append("请假类型：").append(context.get("leaveType")).append("\n");
            sb.append("请假天数：").append(context.get("leaveDays")).append("天\n");
            sb.append("开始时间：").append(context.get("startDate")).append("\n");
            sb.append("结束时间：").append(context.get("endDate")).append("\n");
            sb.append("请假事由：").append(context.get("reason")).append("\n");
        } else if ("expense".equals(businessType)) {
            sb.append("【报销申请】\n");
            sb.append("申请人：").append(context.get("applicantName")).append("\n");
            sb.append("报销类型：").append(context.get("expenseType")).append("\n");
            sb.append("报销金额：").append(context.get("amount")).append("元\n");
            sb.append("报销日期：").append(context.get("expenseDate")).append("\n");
            sb.append("报销事由：").append(context.get("reason")).append("\n");
        }

        sb.append("\n请从以下几个维度给出审批建议：\n");
        sb.append("1. 合规性分析\n");
        sb.append("2. 风险评估\n");
        sb.append("3. 建议操作\n");
        sb.append("4. 审批意见\n");
        sb.append("\n请用简洁专业的语言回答，控制在300字以内。");

        return sb.toString();
    }
}
