package com.lingxi.oa.api;

import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.domain.R;
import com.lingxi.system.api.domain.SysUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 假期额度远程服务接口(供其他模块调用)
 */
public interface RemoteLeaveQuotaService
{
    /**
     * 停用用户所有假期额度(离职时调用)
     * 
     * @param userId 用户ID
     * @return 结果
     */
    @PostMapping("/leave/quota/disable")
    public R<Boolean> disableUserQuotas(@RequestParam("userId") Long userId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 生成假期额度(入职/转正时调用)
     *
     * @param year 年份
     * @param user 用户信息
     * @return 结果
     */
    @PostMapping("/leave/quota/generate")
    public R<Boolean> generateAnnualQuota(
        @RequestParam("year") Integer year,
        @RequestBody SysUser user,
        @RequestHeader(SecurityConstants.FROM_SOURCE) String source
    );
}
