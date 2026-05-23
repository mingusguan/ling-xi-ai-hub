package com.lingxi.system.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.text.Convert;
import com.lingxi.common.core.utils.DateUtils;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.core.utils.poi.ExcelUtil;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.domain.AjaxResult;
import com.lingxi.common.core.web.page.TableDataInfo;
import com.lingxi.common.log.annotation.Log;
import com.lingxi.common.log.enums.BusinessType;
import com.lingxi.common.security.annotation.InnerAuth;
import com.lingxi.common.security.annotation.RequiresPermissions;
import com.lingxi.common.security.service.TokenService;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.domain.SysDept;
import com.lingxi.system.api.domain.SysRole;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.api.model.LoginUser;
import com.lingxi.system.service.ISysConfigService;
import com.lingxi.system.service.ISysDeptService;
import com.lingxi.system.service.ISysPermissionService;
import com.lingxi.system.service.ISysPostService;
import com.lingxi.system.service.ISysRoleService;
import com.lingxi.system.service.ISysUserService;
import com.lingxi.system.service.SystemProtectionService;
import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.oa.api.RemoteLeaveQuotaService;

/**
 * 用户信息
 * 
 * @author cloud
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private ISysPostService postService;

    @Autowired
    private ISysPermissionService permissionService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private RemoteLeaveQuotaService remoteLeaveQuotaService;

    @Autowired
    private SystemProtectionService systemProtectionService;

    /**
     * 获取用户列表
     */
    @RequiresPermissions("system:user:list")
    @GetMapping("/list")
    public TableDataInfo list(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:user:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user)
    {
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.exportExcel(response, list, "用户数据");
    }

    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @RequiresPermissions("system:user:import")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        systemProtectionService.checkCurrentUserSystemWriteAllowed("导入用户");
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        String operName = SecurityUtils.getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws IOException
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.importTemplateExcel(response, "用户数据");
    }

    /**
     * 获取当前用户信息
     */
    @InnerAuth
    @GetMapping("/info/{username}")
    public R<LoginUser> info(@PathVariable("username") String username)
    {
        SysUser sysUser = userService.selectUserByUserName(username);
        if (StringUtils.isNull(sysUser))
        {
            return R.fail("用户名或密码错误");
        }
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(sysUser);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(sysUser);
        LoginUser sysUserVo = new LoginUser();
        sysUserVo.setSysUser(sysUser);
        sysUserVo.setRoles(roles);
        sysUserVo.setPermissions(permissions);
        return R.ok(sysUserVo);
    }

    /**
     * 注册用户信息
     */
    @InnerAuth
    @PostMapping("/register")
    public R<Boolean> register(@RequestBody SysUser sysUser)
    {
        String username = sysUser.getUserName();
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return R.fail("当前系统没有开启注册功能！");
        }
        if (!userService.checkUserNameUnique(sysUser))
        {
            return R.fail("保存用户'" + username + "'失败，注册账号已存在");
        }
        return R.ok(userService.registerUser(sysUser));
    }

    /**
     *记录用户登录IP地址和登录时间
     */
    @InnerAuth
    @PutMapping("/recordlogin")
    public R<Boolean> recordlogin(@RequestBody SysUser sysUser)
    {
        return R.ok(userService.updateLoginInfo(sysUser));
    }

    /**
     * 根据角色查询用户列表
     */
    @InnerAuth
    @GetMapping("/listByRole/{roleKey}")
    public R<List<SysUser>> listUsersByRole(@PathVariable("roleKey") String roleKey)
    {
        List<SysUser> users = userService.selectUsersByRoleKey(roleKey);
        return R.ok(users);
    }

    /**
     * 根据角色和部门查询用户列表
     */
    @InnerAuth
    @GetMapping("/listByRoleAndDept/{roleKey}/{deptId}")
    public R<List<SysUser>> listUsersByRoleAndDept(@PathVariable("roleKey") String roleKey, @PathVariable("deptId") Long deptId)
    {
        List<SysUser> users = userService.selectUsersByRoleKeyAndDept(roleKey, deptId);
        return R.ok(users);
    }

    /**
     * 获取所有在职员工ID列表
     * 在职状态: employment_status = '0'(在职) 或 '2'(试用期转正)
     */
    @InnerAuth
    @GetMapping("/active/users")
    public R<List<SysUser>> getActiveUserIds()
    {
        List<SysUser> activeUsers = userService.selectActiveUserList();
        return R.ok(activeUsers);
    }

    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getSysUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions))
        {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        return ajax;
    }

    // 检查初始密码是否提醒修改
    public boolean initPasswordIsModify(Date pwdUpdateDate)
    {
        Integer initPasswordModify = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }

    // 检查密码是否过期
    public boolean passwordIsExpiration(Date pwdUpdateDate)
    {
        Integer passwordValidateDays = Convert.toInt(configService.selectConfigByKey("sys.account.passwordValidateDays"));
        if (passwordValidateDays != null && passwordValidateDays > 0)
        {
            if (StringUtils.isNull(pwdUpdateDate))
            {
                // 如果从未修改过初始密码，直接提醒过期
                return true;
            }
            Date nowDate = DateUtils.getNowDate();
            return DateUtils.differentDaysByMillisecond(nowDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }

    /**
     * 根据用户编号获取详细信息(外部调用)
     */
    @RequiresPermissions("system:user:query")
    @GetMapping(value = { "/", "/{userId}" })
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId)
    {
        return getUserInfoDetail(userId);
    }

    /**
     * 根据用户编号获取详细信息(内部调用)
     */
    @InnerAuth
    @GetMapping("/inner/{userId}")
    public R<SysUser> getInfoInner(@PathVariable("userId") Long userId)
    {
        SysUser sysUser = userService.selectUserById(userId);
        return R.ok(sysUser);
    }

    /**
     * 批量查询用户信息(内部调用)
     */
    @InnerAuth
    @PostMapping("/inner/batch")
    public R<List<SysUser>> getInfoBatch(@RequestBody List<Long> userIds)
    {
        if (userIds == null || userIds.isEmpty()) {
            return R.ok(new ArrayList<>());
        }
        List<SysUser> users = userService.selectUserByIds(userIds);
        return R.ok(users);
    }

    /**
     * 获取用户详细信息(公共方法)
     */
    private AjaxResult getUserInfoDetail(Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        if (StringUtils.isNotNull(userId))
        {
            userService.checkUserDataScope(userId);
            SysUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            ajax.put("postIds", postService.selectPostListByUserId(userId));
            ajax.put("roleIds", sysUser.getRoles().stream().map(SysRole::getRoleId).collect(Collectors.toList()));
        }
        List<SysRole> roles = roleService.selectRoleAll();
        ajax.put("roles", SecurityUtils.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        ajax.put("posts", postService.selectPostAll());
        return ajax;
    }

    /**
     * 新增用户
     */
    @RequiresPermissions("system:user:add")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user)
    {
        systemProtectionService.checkCurrentUserSystemWriteAllowed("新增用户");
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setCreateBy(SecurityUtils.getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        AjaxResult result = toAjax(userService.insertUser(user));
        
        // 新增用户时联动处理员工信息
        if (result.isSuccess()) {
            try {
                handleEmployeeChange(user);
            } catch (Exception e) {
                logger.error("联动更新假期额度失败: userId={}", user.getUserId(), e);
            }
        }
        return result;
    }

    /**
     * 修改用户
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUser user)
    {
        systemProtectionService.checkCurrentUserSystemWriteAllowed("修改用户");
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(SecurityUtils.getUsername());
        AjaxResult result = toAjax(userService.updateUser(user));
        
        // 联动处理员工信息变化(假期额度)
        if (result.isSuccess()) {
            try {
                handleEmployeeChange(user);
            } catch (Exception e) {
                logger.error("联动更新假期额度失败: userId={}", user.getUserId(), e);
            }
        }
        return result;
    }
    
    /**
     * 根据员工信息变化联动处理假期额度
     */
    private void handleEmployeeChange(SysUser user) 
    {
        Long userId = user.getUserId();
        String employmentStatus = user.getEmploymentStatus();
        Date entryDate = user.getEntryDate();
        
        if (StringUtils.isEmpty(employmentStatus)) {
            return;
        }
        
        // 场景1: 离职 - 停用所有假期额度
        if ("1".equals(employmentStatus)) {
            try {
                remoteLeaveQuotaService.disableUserQuotas(userId, SecurityConstants.INNER);
                logger.info("员工离职,已停用假期额度: userId={}", userId);
            } catch (Exception e) {
                logger.error("停用假期额度失败: userId={}", userId, e);
            }
        }
        // 场景2: 新入职或试用期转正 - 重新计算假期额度
        else if (("0".equals(employmentStatus) || "2".equals(employmentStatus)) && entryDate != null) {
            try {
                //获取当年年份
                Integer year = LocalDate.now().getYear();
                remoteLeaveQuotaService.generateAnnualQuota(year, user, SecurityConstants.INNER);
                logger.info("员工入职/转正,已重新计算假期额度: userId={}, year={}", userId, year);
            } catch (Exception e) {
                logger.error("重新计算假期额度失败: userId={}", userId, e);
            }
        }
    }

    /**
     * 删除用户
     */
    @RequiresPermissions("system:user:remove")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        systemProtectionService.checkCurrentUserSystemWriteAllowed("删除用户");
        if (ArrayUtils.contains(userIds, SecurityUtils.getUserId()))
        {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(userIds));
    }

    /**
     * 重置密码
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysUser user)
    {
        systemProtectionService.checkCurrentUserSystemWriteAllowed("重置用户密码");
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.resetPwd(user));
    }

    /**
     * 状态修改
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user)
    {
        systemProtectionService.checkCurrentUserSystemWriteAllowed("修改用户状态");
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    /**
     * 根据用户编号获取授权角色
     */
    @RequiresPermissions("system:user:query")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = userService.selectUserById(userId);
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        ajax.put("user", user);
        ajax.put("roles", SecurityUtils.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return ajax;
    }

    /**
     * 用户授权角色
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds)
    {
        systemProtectionService.checkCurrentUserSystemWriteAllowed("修改用户授权");
        systemProtectionService.checkProtectedUser(userId, "修改授权");
        userService.checkUserDataScope(userId);
        roleService.checkRoleDataScope(roleIds);
        userService.insertUserAuth(userId, roleIds);
        return success();
    }

    /**
     * 获取部门树列表
     */
    @GetMapping("/deptTree")
    public AjaxResult deptTree(SysDept dept)
    {
        return success(deptService.selectDeptTreeList(dept));
    }
}
