package com.lingxi.system.api.local;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.system.api.RemoteUserService;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;
import com.lingxi.system.service.ISysConfigService;
import com.lingxi.system.service.ISysPermissionService;
import com.lingxi.system.service.ISysUserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 单体模式下的本地用户服务调用。
 */
@Service
public class LocalRemoteUserService implements RemoteUserService {

    private final ISysUserService userService;
    private final ISysPermissionService permissionService;
    private final ISysConfigService configService;

    public LocalRemoteUserService(ISysUserService userService, ISysPermissionService permissionService,
            ISysConfigService configService) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.configService = configService;
    }

    @Override
    public R<LoginUser> getUserInfo(String username, String source) {
        SysUser sysUser = userService.selectUserByUserName(username);
        if (StringUtils.isNull(sysUser)) {
            return R.fail("用户名或密码错误");
        }
        Set<String> roles = permissionService.getRolePermission(sysUser);
        Set<String> permissions = permissionService.getMenuPermission(sysUser);
        LoginUser loginUser = new LoginUser();
        loginUser.setSysUser(sysUser);
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        return R.ok(loginUser);
    }

    @Override
    public R<SysUser> getUserById(Long userId, String source) {
        return R.ok(userService.selectUserById(userId));
    }

    @Override
    public R<Boolean> registerUserInfo(SysUser sysUser, String source) {
        String username = sysUser.getUserName();
        if (!"true".equals(configService.selectConfigByKey("sys.account.registerUser"))) {
            return R.fail("当前系统没有开启注册功能");
        }
        if (!userService.checkUserNameUnique(sysUser)) {
            return R.fail("保存用户'" + username + "'失败，注册账号已存在");
        }
        return R.ok(userService.registerUser(sysUser));
    }

    @Override
    public R<Boolean> recordUserLogin(SysUser sysUser, String source) {
        return R.ok(userService.updateLoginInfo(sysUser));
    }

    @Override
    public R<List<SysUser>> listUsersByRole(String roleKey, String source) {
        return R.ok(userService.selectUsersByRoleKey(roleKey));
    }

    @Override
    public R<List<SysUser>> listUsersByRoleAndDept(String roleKey, Long deptId, String source) {
        return R.ok(userService.selectUsersByRoleKeyAndDept(roleKey, deptId));
    }

    @Override
    public R<List<SysUser>> getActiveUsers(String source) {
        return R.ok(userService.selectActiveUserList());
    }

    @Override
    public R<List<SysUser>> getUserByIds(List<Long> userIds, String source) {
        if (userIds == null || userIds.isEmpty()) {
            return R.ok(new ArrayList<>());
        }
        return R.ok(userService.selectUserByIds(userIds));
    }
}
