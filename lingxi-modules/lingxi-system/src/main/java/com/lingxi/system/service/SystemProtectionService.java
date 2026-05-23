package com.lingxi.system.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lingxi.common.core.exception.ServiceException;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.security.utils.SecurityUtils;
import com.lingxi.system.api.domain.SysDictData;
import com.lingxi.system.api.domain.SysDictType;
import com.lingxi.system.api.domain.SysRole;
import com.lingxi.system.api.domain.SysUser;
import com.lingxi.system.config.SystemProtectionProperties;
import com.lingxi.system.domain.SysConfig;
import com.lingxi.system.mapper.SysConfigMapper;
import com.lingxi.system.mapper.SysDictDataMapper;
import com.lingxi.system.mapper.SysDictTypeMapper;
import com.lingxi.system.mapper.SysRoleMapper;
import com.lingxi.system.mapper.SysUserMapper;

/**
 * 系统核心数据保护服务。
 *
 * @author lingxi
 */
@Service
public class SystemProtectionService
{
    @Autowired
    private SystemProtectionProperties properties;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private SysDictTypeMapper dictTypeMapper;

    @Autowired
    private SysDictDataMapper dictDataMapper;

    /**
     * 演示账号不允许改自己的密码，避免后续访问者无法登录。
     */
    public void checkDemoUserPasswordChange()
    {
        if (isCurrentDemoUser())
        {
            throw new ServiceException("演示账号不允许修改密码");
        }
    }

    /**
     * 演示账号不允许维护系统骨架数据，避免误操作导致菜单、权限或登录异常。
     *
     * @param operation 操作描述
     */
    public void checkCurrentUserSystemWriteAllowed(String operation)
    {
        if (isCurrentDemoUser())
        {
            throw new ServiceException("演示账号不允许" + operation);
        }
    }

    /**
     * 校验用户是否为受保护用户。
     *
     * @param user 用户信息
     * @param operation 操作描述
     */
    public void checkProtectedUser(SysUser user, String operation)
    {
        if (!isEnabled() || SecurityUtils.isAdmin())
        {
            return;
        }
        SysUser targetUser = getTargetUser(user);
        if (isProtectedUser(targetUser))
        {
            throw new ServiceException("受保护用户不允许" + operation);
        }
    }

    /**
     * 校验用户是否为受保护用户。
     *
     * @param userId 用户 ID
     * @param operation 操作描述
     */
    public void checkProtectedUser(Long userId, String operation)
    {
        if (StringUtils.isNotNull(userId))
        {
            checkProtectedUser(new SysUser(userId), operation);
        }
    }

    /**
     * 批量校验用户是否为受保护用户。
     *
     * @param userIds 用户 ID 列表
     * @param operation 操作描述
     */
    public void checkProtectedUsers(Long[] userIds, String operation)
    {
        if (StringUtils.isNotEmpty(userIds))
        {
            for (Long userId : userIds)
            {
                checkProtectedUser(userId, operation);
            }
        }
    }

    /**
     * 校验角色是否为受保护角色。
     *
     * @param role 角色信息
     * @param operation 操作描述
     */
    public void checkProtectedRole(SysRole role, String operation)
    {
        if (!isEnabled() || SecurityUtils.isAdmin())
        {
            return;
        }
        SysRole targetRole = getTargetRole(role);
        if (isProtectedRole(targetRole))
        {
            throw new ServiceException("受保护角色不允许" + operation);
        }
    }

    /**
     * 校验角色是否为受保护角色。
     *
     * @param roleId 角色 ID
     * @param operation 操作描述
     */
    public void checkProtectedRole(Long roleId, String operation)
    {
        if (StringUtils.isNotNull(roleId))
        {
            checkProtectedRole(new SysRole(roleId), operation);
        }
    }

    /**
     * 批量校验角色是否为受保护角色。
     *
     * @param roleIds 角色 ID 列表
     * @param operation 操作描述
     */
    public void checkProtectedRoles(Long[] roleIds, String operation)
    {
        if (StringUtils.isNotEmpty(roleIds))
        {
            for (Long roleId : roleIds)
            {
                checkProtectedRole(roleId, operation);
            }
        }
    }

    /**
     * 校验系统参数是否为受保护参数。
     *
     * @param config 参数信息
     * @param operation 操作描述
     */
    public void checkProtectedConfig(SysConfig config, String operation)
    {
        if (!isEnabled() || SecurityUtils.isAdmin())
        {
            return;
        }
        SysConfig targetConfig = getTargetConfig(config);
        if (targetConfig != null && containsIgnoreCase(properties.getProtectedConfigKeys(), targetConfig.getConfigKey()))
        {
            throw new ServiceException("受保护参数不允许" + operation);
        }
    }

    /**
     * 批量校验系统参数是否为受保护参数。
     *
     * @param configIds 参数 ID 列表
     * @param operation 操作描述
     */
    public void checkProtectedConfigs(Long[] configIds, String operation)
    {
        if (StringUtils.isNotEmpty(configIds))
        {
            for (Long configId : configIds)
            {
                SysConfig config = new SysConfig();
                config.setConfigId(configId);
                checkProtectedConfig(config, operation);
            }
        }
    }

    /**
     * 校验字典类型是否为受保护字典。
     *
     * @param dictType 字典类型信息
     * @param operation 操作描述
     */
    public void checkProtectedDictType(SysDictType dictType, String operation)
    {
        if (!isEnabled() || SecurityUtils.isAdmin())
        {
            return;
        }
        SysDictType targetDictType = getTargetDictType(dictType);
        if (targetDictType != null && containsIgnoreCase(properties.getProtectedDictTypes(), targetDictType.getDictType()))
        {
            throw new ServiceException("受保护字典不允许" + operation);
        }
    }

    /**
     * 批量校验字典类型是否为受保护字典。
     *
     * @param dictIds 字典类型 ID 列表
     * @param operation 操作描述
     */
    public void checkProtectedDictTypes(Long[] dictIds, String operation)
    {
        if (StringUtils.isNotEmpty(dictIds))
        {
            for (Long dictId : dictIds)
            {
                SysDictType dictType = new SysDictType();
                dictType.setDictId(dictId);
                checkProtectedDictType(dictType, operation);
            }
        }
    }

    /**
     * 校验字典数据是否属于受保护字典。
     *
     * @param dictData 字典数据信息
     * @param operation 操作描述
     */
    public void checkProtectedDictData(SysDictData dictData, String operation)
    {
        if (!isEnabled() || SecurityUtils.isAdmin())
        {
            return;
        }
        SysDictData targetDictData = getTargetDictData(dictData);
        if (targetDictData != null && containsIgnoreCase(properties.getProtectedDictTypes(), targetDictData.getDictType()))
        {
            throw new ServiceException("受保护字典数据不允许" + operation);
        }
    }

    /**
     * 批量校验字典数据是否属于受保护字典。
     *
     * @param dictCodes 字典数据 ID 列表
     * @param operation 操作描述
     */
    public void checkProtectedDictDataList(Long[] dictCodes, String operation)
    {
        if (StringUtils.isNotEmpty(dictCodes))
        {
            for (Long dictCode : dictCodes)
            {
                SysDictData dictData = new SysDictData();
                dictData.setDictCode(dictCode);
                checkProtectedDictData(dictData, operation);
            }
        }
    }

    private boolean isCurrentDemoUser()
    {
        if (!isEnabled())
        {
            return false;
        }
        return containsIgnoreCase(properties.getDemoUserNames(), SecurityUtils.getUsername());
    }

    private boolean isEnabled()
    {
        return properties != null && properties.isEnabled();
    }

    private SysUser getTargetUser(SysUser user)
    {
        if (user == null || user.getUserId() == null)
        {
            return user;
        }
        SysUser dbUser = userMapper.selectUserById(user.getUserId());
        return dbUser == null ? user : dbUser;
    }

    private SysRole getTargetRole(SysRole role)
    {
        if (role == null || role.getRoleId() == null)
        {
            return role;
        }
        SysRole dbRole = roleMapper.selectRoleById(role.getRoleId());
        return dbRole == null ? role : dbRole;
    }

    private SysConfig getTargetConfig(SysConfig config)
    {
        if (config == null || StringUtils.isNotNull(config.getConfigKey()))
        {
            return config;
        }
        return configMapper.selectConfigById(config.getConfigId());
    }

    private SysDictType getTargetDictType(SysDictType dictType)
    {
        if (dictType == null || StringUtils.isNotNull(dictType.getDictType()))
        {
            return dictType;
        }
        return dictTypeMapper.selectDictTypeById(dictType.getDictId());
    }

    private SysDictData getTargetDictData(SysDictData dictData)
    {
        if (dictData == null || StringUtils.isNotNull(dictData.getDictType()))
        {
            return dictData;
        }
        return dictDataMapper.selectDictDataById(dictData.getDictCode());
    }

    private boolean isProtectedUser(SysUser user)
    {
        return user != null && (user.isAdmin() || containsIgnoreCase(properties.getProtectedUserNames(), user.getUserName()));
    }

    private boolean isProtectedRole(SysRole role)
    {
        return role != null && (role.isAdmin()
                || containsLong(properties.getProtectedRoleIds(), role.getRoleId())
                || containsIgnoreCase(properties.getProtectedRoleKeys(), role.getRoleKey()));
    }

    private boolean containsIgnoreCase(List<String> values, String target)
    {
        if (values == null || target == null)
        {
            return false;
        }
        String normalizedTarget = target.trim();
        for (String value : values)
        {
            if (value != null && value.trim().equalsIgnoreCase(normalizedTarget))
            {
                return true;
            }
        }
        return false;
    }

    private boolean containsLong(List<Long> values, Long target)
    {
        if (values == null || target == null)
        {
            return false;
        }
        for (Long value : values)
        {
            if (value != null && value.longValue() == target.longValue())
            {
                return true;
            }
        }
        return false;
    }
}
