package com.lingxi.system.api.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.lingxi.common.core.domain.R;
import com.lingxi.system.api.RemoteUserService;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;

import java.util.List;

/**
 * 用户服务降级处理
 * 
 * @author cloud
 */
@Component
public class RemoteUserFallbackFactory implements FallbackFactory<RemoteUserService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteUserFallbackFactory.class);

    @Override
    public RemoteUserService create(Throwable throwable)
    {
        log.error("用户服务调用失败:{}", throwable.getMessage());
        return new RemoteUserService()
        {
            @Override
            public R<LoginUser> getUserInfo(String username, String source)
            {
                return R.fail("获取用户失败:" + throwable.getMessage());
            }

            @Override
            public R<SysUser> getUserById(Long userId, String source)
            {
                return R.fail("获取用户失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> registerUserInfo(SysUser sysUser, String source)
            {
                return R.fail("注册用户失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> recordUserLogin(SysUser sysUser, String source)
            {
                return R.fail("记录用户登录信息失败:" + throwable.getMessage());
            }

            @Override
            public R<List<SysUser>> listUsersByRole(String roleKey, String source) {
                return R.fail("获取角色下用户失败:" + throwable.getMessage());
            }

            @Override
            public R<List<SysUser>> listUsersByRoleAndDept(String roleKey, Long deptId, String source) {
                return R.fail("获取角色下部门用户失败:" + throwable.getMessage());
            }

            @Override
            public R<List<SysUser>> getActiveUsers(String source) {
                return R.fail("获取所有在职员工失败:" + throwable.getMessage());
            }

            @Override
            public R<List<SysUser>> getUserByIds(List<Long> userIds, String source) {
                return R.fail("批量获取用户信息失败:" + throwable.getMessage());
            }
        };
    }
}
