package com.lingxi.knowledge.api;

import com.lingxi.common.core.domain.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 知识库远程服务
 *
 * @author lingxi
 */
public interface RemoteKnowledgeService {

    /**
     * 知识库问答
     *
     * @param request 问答请求
     * @return AI回答
     */
    @PostMapping("/knowledge/qa/chat")
    R<Map<String, Object>> askQuestion(@RequestBody Map<String, Object> request);
}
