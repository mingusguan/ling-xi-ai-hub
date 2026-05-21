package com.lingxi.ai.api;

import com.lingxi.common.core.domain.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * AI服务远程调用接口
 *
 * @author lingxi
 */
public interface RemoteAiService {

    /**
     * 生成审批建议
     *
     * @param context 审批上下文
     * @return AI生成的建议
     */
    @PostMapping("/ai/model/suggestion")
    R<String> generateSuggestion(@RequestBody Map<String, Object> context);

    /**
     * 风险分析
     *
     * @param formData 表单数据
     * @return JSON格式的风险分析结果
     */
    @PostMapping("/ai/model/risk")
    R<String> analyzeRisk(@RequestBody Map<String, Object> formData);

    /**
     * 生成推荐意见模板
     *
     * @param context 上下文
     * @return JSON数组格式的推荐意见
     */
    @PostMapping("/ai/model/templates")
    R<String> generateTemplates(@RequestBody Map<String, Object> context);

    /**
     * 完整审批分析（风险+建议+模板）
     *
     * @param context 审批上下文
     * @return JSON格式的完整分析结果
     */
    @PostMapping("/ai/model/analyze")
    R<String> analyzeAndSuggest(@RequestBody Map<String, Object> context);
}
