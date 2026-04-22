package com.lingxi.ai.service;

import java.util.Map;

/**
 * AI模型服务接口
 *
 * @author lingxi
 */
public interface IAiModelService {

    /**
     * 完整审批分析（风险+建议+模板）
     */
    String analyzeAndSuggest(Map<String, Object> context);
}
