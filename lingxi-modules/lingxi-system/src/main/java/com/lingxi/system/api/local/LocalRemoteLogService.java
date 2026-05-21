package com.lingxi.system.api.local;

import com.lingxi.common.core.domain.R;
import com.lingxi.system.api.RemoteLogService;
import com.lingxi.system.api.domain.SysLogininfor;
import com.lingxi.system.api.domain.SysOperLog;
import com.lingxi.system.service.ISysLogininforService;
import com.lingxi.system.service.ISysOperLogService;
import org.springframework.stereotype.Service;

/**
 * 单体模式下的本地日志服务调用。
 */
@Service
public class LocalRemoteLogService implements RemoteLogService {

    private final ISysOperLogService operLogService;
    private final ISysLogininforService logininforService;

    public LocalRemoteLogService(ISysOperLogService operLogService, ISysLogininforService logininforService) {
        this.operLogService = operLogService;
        this.logininforService = logininforService;
    }

    @Override
    public R<Boolean> saveLog(SysOperLog sysOperLog, String source) {
        return R.ok(operLogService.insertOperlog(sysOperLog) > 0);
    }

    @Override
    public R<Boolean> saveLogininfor(SysLogininfor sysLogininfor, String source) {
        return R.ok(logininforService.insertLogininfor(sysLogininfor) > 0);
    }
}
