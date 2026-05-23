package com.lingxi.knowledge.api;

import com.lingxi.common.core.domain.R;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI治理日志记录接口。
 */
public interface AiGovernanceClient {

    /**
     * 记录AI治理日志。
     */
    @PostMapping("/ai/governance/record")
    R<Void> record(@RequestBody Map<String, Object> payload);
}
