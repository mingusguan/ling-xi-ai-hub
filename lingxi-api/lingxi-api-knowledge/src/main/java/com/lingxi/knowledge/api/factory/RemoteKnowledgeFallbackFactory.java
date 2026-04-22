package com.lingxi.knowledge.api.factory;

import com.lingxi.common.core.domain.R;
import com.lingxi.knowledge.api.RemoteKnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识库服务降级处理
 *
 * @author lingxi
 */
@Component
public class RemoteKnowledgeFallbackFactory implements FallbackFactory<RemoteKnowledgeService> {
    
    private static final Logger log = LoggerFactory.getLogger(RemoteKnowledgeFallbackFactory.class);

    @Override
    public RemoteKnowledgeService create(Throwable throwable) {
        log.error("知识库服务调用失败:{}", throwable.getMessage());
        return new RemoteKnowledgeService() {
            @Override
            public R<Map<String, Object>> askQuestion(Map<String, Object> request) {
                log.warn("知识库问答降级");
                Map<String, Object> data = new HashMap<>();
                data.put("answer", "知识库服务暂时不可用，请稍后重试");
                data.put("sources", new java.util.ArrayList<>());
                return R.ok(data);
            }
        };
    }
}
