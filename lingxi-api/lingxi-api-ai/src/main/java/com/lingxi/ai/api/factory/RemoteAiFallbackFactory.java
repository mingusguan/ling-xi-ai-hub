package com.lingxi.ai.api.factory;

import com.lingxi.ai.api.RemoteAiService;
import com.lingxi.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI服务降级处理
 *
 * @author lingxi
 */
@Component
public class RemoteAiFallbackFactory implements FallbackFactory<RemoteAiService> {
    
    private static final Logger log = LoggerFactory.getLogger(RemoteAiFallbackFactory.class);

    @Override
    public RemoteAiService create(Throwable throwable) {
        log.error("AI服务调用失败:{}", throwable.getMessage());
        return new RemoteAiService() {
            @Override
            public R<String> generateSuggestion(Map<String, Object> context) {
                log.warn("生成审批建议降级");
                return R.ok("AI服务暂时不可用，请人工审核");
            }

            @Override
            public R<String> analyzeRisk(Map<String, Object> formData) {
                log.warn("风险分析降级");
                return R.ok("{\"riskLevel\":\"normal\",\"riskPoints\":[\"AI服务不可用，请人工审核\"]}");
            }

            @Override
            public R<String> generateTemplates(Map<String, Object> context) {
                log.warn("生成推荐意见降级");
                return R.ok("[\"同意\", \"驳回\", \"补充材料后重新提交\"]");
            }

            @Override
            public R<String> analyzeAndSuggest(Map<String, Object> context) {
                log.warn("完整审批分析降级");
                return R.ok("{\"riskLevel\":\"normal\",\"riskPoints\":[\"请人工审核\"],\"aiSuggestion\":\"AI服务不可用\",\"templates\":[\"同意\",\"驳回\"]}");
            }
        };
    }
}
