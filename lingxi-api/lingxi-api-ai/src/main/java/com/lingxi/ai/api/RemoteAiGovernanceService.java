package com.lingxi.ai.api;

import com.lingxi.common.core.domain.R;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI治理日志远程服务接口。
 */
public interface RemoteAiGovernanceService {

    /**
     * 记录AI治理日志。
     *
     * @param payload 治理日志载荷
     * @return 记录结果
     */
    @PostMapping("/ai/governance/record")
    R<Void> record(@RequestBody Map<String, Object> payload);
}
