package com.lingxi.ai.api.local;

import com.lingxi.ai.api.RemoteAiGovernanceService;
import com.lingxi.ai.governance.service.IAiGovernanceService;
import com.lingxi.common.core.domain.R;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 单体模式下的本地AI治理日志服务。
 */
@Service
@RequiredArgsConstructor
public class LocalRemoteAiGovernanceService implements RemoteAiGovernanceService {

    private final IAiGovernanceService governanceService;

    @Override
    public R<Void> record(Map<String, Object> payload) {
        governanceService.recordExternal(payload);
        return R.ok();
    }
}
