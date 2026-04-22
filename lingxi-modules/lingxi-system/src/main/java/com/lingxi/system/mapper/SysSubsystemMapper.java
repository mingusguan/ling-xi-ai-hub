package com.lingxi.system.mapper;

import java.util.List;
import com.lingxi.system.domain.SysSubsystem;

/**
 * 子系统Mapper接口
 * 
 * @author cloud
 */
public interface SysSubsystemMapper
{
    /**
     * 查询子系统
     * 
     * @param subsystemId 子系统主键
     * @return 子系统
     */
    public SysSubsystem selectSubsystemById(Long subsystemId);

    /**
     * 根据编码查询子系统
     * 
     * @param subsystemCode 子系统编码
     * @return 子系统
     */
    public SysSubsystem selectSubsystemByCode(String subsystemCode);

    /**
     * 查询子系统列表
     * 
     * @param subsystem 子系统
     * @return 子系统集合
     */
    public List<SysSubsystem> selectSubsystemList(SysSubsystem subsystem);

    /**
     * 新增子系统
     * 
     * @param subsystem 子系统
     * @return 结果
     */
    public int insertSubsystem(SysSubsystem subsystem);

    /**
     * 修改子系统
     * 
     * @param subsystem 子系统
     * @return 结果
     */
    public int updateSubsystem(SysSubsystem subsystem);

    /**
     * 删除子系统
     * 
     * @param subsystemId 子系统主键
     * @return 结果
     */
    public int deleteSubsystemById(Long subsystemId);

    /**
     * 批量删除子系统
     * 
     * @param subsystemIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSubsystemByIds(Long[] subsystemIds);

    /**
     * 校验子系统编码是否唯一
     * 
     * @param subsystemCode 子系统编码
     * @return 结果
     */
    public SysSubsystem checkSubsystemCodeUnique(String subsystemCode);
}
