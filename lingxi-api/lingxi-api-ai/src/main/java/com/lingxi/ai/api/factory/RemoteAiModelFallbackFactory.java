package com.lingxi.ai.api.factory;

import com.lingxi.ai.api.RemoteAiModelService;
import com.lingxi.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI模型服务降级处理
 *
 * @author lingxi
 */
@Component
public class RemoteAiModelFallbackFactory implements FallbackFactory<RemoteAiModelService> {
    
    private static final Logger log = LoggerFactory.getLogger(RemoteAiModelFallbackFactory.class);

    @Override
    public RemoteAiModelService create(Throwable throwable) {
        log.error("AI模型服务调用失败:{}", throwable.getMessage());
        return new RemoteAiModelService() {
            @Override
            public R<String> analyzeAndSuggest(Map<String, Object> context, String source) {
                return R.fail("AI完整分析服务暂时不可用:" + throwable.getMessage());
            }
        };
    }
}
