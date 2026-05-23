package com.lingxi.system.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 系统核心数据保护配置。
 *
 * @author lingxi
 */
@Configuration
@ConfigurationProperties(prefix = "lingxi.system.protection")
public class SystemProtectionProperties
{
    /**
     * 是否启用系统核心数据保护。
     */
    private boolean enabled = true;

    /**
     * 演示账号名单，这些账号不能修改密码，也不能维护用户、角色、菜单等系统骨架数据。
     */
    private List<String> demoUserNames = new ArrayList<>(List.of("demo"));

    /**
     * 受保护用户账号，非超级管理员不能修改、禁用、删除或重置这些账号。
     */
    private List<String> protectedUserNames = new ArrayList<>(List.of("mingus", "demo"));

    /**
     * 受保护角色 ID，非超级管理员不能修改这些角色的授权关系。
     */
    private List<Long> protectedRoleIds = new ArrayList<>(List.of(1L));

    /**
     * 受保护角色标识，非超级管理员不能修改这些角色的授权关系。
     */
    private List<String> protectedRoleKeys = new ArrayList<>(List.of("mingus", "demo"));

    /**
     * 受保护参数键，避免登录、密码和注册等基础行为被误改。
     */
    private List<String> protectedConfigKeys = new ArrayList<>(List.of(
            "sys.account.registerUser",
            "sys.user.initPassword",
            "sys.account.initPasswordModify",
            "sys.account.passwordValidateDays"));

    /**
     * 受保护字典类型，避免基础页面枚举被误删或误改。
     */
    private List<String> protectedDictTypes = new ArrayList<>(List.of(
            "sys_normal_disable",
            "sys_user_sex",
            "sys_show_hide",
            "sys_notice_type",
            "sys_notice_status",
            "sys_oper_type",
            "sys_common_status",
            "sys_job_group",
            "sys_job_status",
            "sys_yes_no",
            "sys_job_level"));

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public List<String> getDemoUserNames()
    {
        return demoUserNames;
    }

    public void setDemoUserNames(List<String> demoUserNames)
    {
        this.demoUserNames = demoUserNames;
    }

    public List<String> getProtectedUserNames()
    {
        return protectedUserNames;
    }

    public void setProtectedUserNames(List<String> protectedUserNames)
    {
        this.protectedUserNames = protectedUserNames;
    }

    public List<Long> getProtectedRoleIds()
    {
        return protectedRoleIds;
    }

    public void setProtectedRoleIds(List<Long> protectedRoleIds)
    {
        this.protectedRoleIds = protectedRoleIds;
    }

    public List<String> getProtectedRoleKeys()
    {
        return protectedRoleKeys;
    }

    public void setProtectedRoleKeys(List<String> protectedRoleKeys)
    {
        this.protectedRoleKeys = protectedRoleKeys;
    }

    public List<String> getProtectedConfigKeys()
    {
        return protectedConfigKeys;
    }

    public void setProtectedConfigKeys(List<String> protectedConfigKeys)
    {
        this.protectedConfigKeys = protectedConfigKeys;
    }

    public List<String> getProtectedDictTypes()
    {
        return protectedDictTypes;
    }

    public void setProtectedDictTypes(List<String> protectedDictTypes)
    {
        this.protectedDictTypes = protectedDictTypes;
    }
}
