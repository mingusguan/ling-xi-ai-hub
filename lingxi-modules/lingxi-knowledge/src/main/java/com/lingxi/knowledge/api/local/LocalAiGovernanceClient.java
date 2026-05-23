package com.lingxi.knowledge.api.local;

import com.lingxi.ai.api.RemoteAiGovernanceService;
import com.lingxi.common.core.domain.R;
import com.lingxi.knowledge.api.AiGovernanceClient;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 单体模式下的本地AI治理日志客户端。
 */
@Service
@RequiredArgsConstructor
public class LocalAiGovernanceClient implements AiGovernanceClient {

    private final RemoteAiGovernanceService remoteAiGovernanceService;

    @Override
    public R<Void> record(Map<String, Object> payload) {
        return remoteAiGovernanceService.record(payload);
    }
}
