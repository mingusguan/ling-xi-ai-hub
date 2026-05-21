package com.lingxi.system.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.domain.R;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;

import java.util.List;

/**
 * 用户服务
 * 
 * @author cloud
 */
public interface RemoteUserService
{
    /**
     * 通过用户名查询用户信息
     *
     * @param username 用户名
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/user/info/{username}")
    public R<LoginUser> getUserInfo(@PathVariable("username") String username, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 通过用户ID查询用户信息
     *
     * @param userId 用户ID
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/user/inner/{userId}")
    public R<SysUser> getUserById(@PathVariable("userId") Long userId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 注册用户信息
     *
     * @param sysUser 用户信息
     * @param source 请求来源
     * @return 结果
     */
    @PostMapping("/user/register")
    public R<Boolean> registerUserInfo(@RequestBody SysUser sysUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 记录用户登录IP地址和登录时间
     *
     * @param sysUser 用户信息
     * @param source 请求来源
     * @return 结果
     */
    @PutMapping("/user/recordlogin")
    public R<Boolean> recordUserLogin(@RequestBody SysUser sysUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据角色查询用户列表
     *
     * @param roleKey 角色标识
     * @param source 请求来源
     * @return 用户列表
     */
    @GetMapping("/user/listByRole/{roleKey}")
    public R<List<SysUser>> listUsersByRole(@PathVariable("roleKey") String roleKey, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据角色和部门查询用户列表
     *
     * @param roleKey 角色标识
     * @param deptId 部门ID
     * @param source 请求来源
     * @return 用户列表
     */
    @GetMapping("/user/listByRoleAndDept/{roleKey}/{deptId}")
    public R<List<SysUser>> listUsersByRoleAndDept(@PathVariable("roleKey") String roleKey, @PathVariable("deptId") Long deptId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 获取所有在职员工ID列表
     * 在职状态: employment_status = '0'(在职) 或 '2'(试用期转正)
     *
     * @param source 请求来源
     * @return 用户ID列表
     */
    @GetMapping("/user/active/users")
    public R<List<SysUser>> getActiveUsers(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 批量查询用户信息
     *
     * @param userIds 用户ID列表
     * @param source 请求来源
     * @return 用户信息列表
     */
    @PostMapping("/user/inner/batch")
    public R<List<SysUser>> getUserByIds(@RequestBody List<Long> userIds, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
