package com.lingxi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lingxi.common.core.constant.UserConstants;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.system.domain.SysSubsystem;
import com.lingxi.system.mapper.SysSubsystemMapper;
import com.lingxi.system.service.ISysSubsystemService;

/**
 * 子系统Service业务层处理
 * 
 * @author cloud
 */
@Service
public class SysSubsystemServiceImpl implements ISysSubsystemService
{
    @Autowired
    private SysSubsystemMapper subsystemMapper;

    /**
     * 查询子系统
     * 
     * @param subsystemId 子系统主键
     * @return 子系统
     */
    @Override
    public SysSubsystem selectSubsystemById(Long subsystemId)
    {
        return subsystemMapper.selectSubsystemById(subsystemId);
    }

    /**
     * 根据编码查询子系统
     * 
     * @param subsystemCode 子系统编码
     * @return 子系统
     */
    @Override
    public SysSubsystem selectSubsystemByCode(String subsystemCode)
    {
        return subsystemMapper.selectSubsystemByCode(subsystemCode);
    }

    /**
     * 查询子系统列表
     * 
     * @param subsystem 子系统
     * @return 子系统
     */
    @Override
    public List<SysSubsystem> selectSubsystemList(SysSubsystem subsystem)
    {
        return subsystemMapper.selectSubsystemList(subsystem);
    }

    /**
     * 新增子系统
     * 
     * @param subsystem 子系统
     * @return 结果
     */
    @Override
    public int insertSubsystem(SysSubsystem subsystem)
    {
        if (!checkSubsystemCodeUnique(subsystem))
        {
            throw new RuntimeException("新增子系统'" + subsystem.getSubsystemName() + "'失败，子系统编码已存在");
        }
        return subsystemMapper.insertSubsystem(subsystem);
    }

    /**
     * 修改子系统
     * 
     * @param subsystem 子系统
     * @return 结果
     */
    @Override
    public int updateSubsystem(SysSubsystem subsystem)
    {
        if (!checkSubsystemCodeUnique(subsystem))
        {
            throw new RuntimeException("修改子系统'" + subsystem.getSubsystemName() + "'失败，子系统编码已存在");
        }
        return subsystemMapper.updateSubsystem(subsystem);
    }

    /**
     * 批量删除子系统
     * 
     * @param subsystemIds 需要删除的子系统主键
     * @return 结果
     */
    @Override
    public int deleteSubsystemByIds(Long[] subsystemIds)
    {
        return subsystemMapper.deleteSubsystemByIds(subsystemIds);
    }

    /**
     * 删除子系统信息
     * 
     * @param subsystemId 子系统主键
     * @return 结果
     */
    @Override
    public int deleteSubsystemById(Long subsystemId)
    {
        return subsystemMapper.deleteSubsystemById(subsystemId);
    }

    /**
     * 校验子系统编码是否唯一
     * 
     * @param subsystem 子系统
     * @return 结果
     */
    @Override
    public boolean checkSubsystemCodeUnique(SysSubsystem subsystem)
    {
        Long subsystemId = StringUtils.isNull(subsystem.getSubsystemId()) ? -1L : subsystem.getSubsystemId();
        SysSubsystem info = subsystemMapper.checkSubsystemCodeUnique(subsystem.getSubsystemCode());
        if (StringUtils.isNotNull(info) && info.getSubsystemId().longValue() != subsystemId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
