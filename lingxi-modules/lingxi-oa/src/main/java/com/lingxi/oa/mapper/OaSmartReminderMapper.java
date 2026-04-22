package com.lingxi.oa.mapper;

import com.lingxi.oa.domain.OaSmartReminder;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 智能提醒 Mapper 接口
 *
 * @author lingxi
 */
@Mapper
public interface OaSmartReminderMapper {
    
    /**
     * 查询智能提醒列表
     *
     * @param reminder 提醒条件
     * @return 提醒列表
     */
    List<OaSmartReminder> selectOaSmartReminderList(OaSmartReminder reminder);
    
    /**
     * 根据ID查询智能提醒
     *
     * @param reminderId 提醒ID
     * @return 提醒实体
     */
    OaSmartReminder selectOaSmartReminderById(Long reminderId);
    
    /**
     * 插入智能提醒
     *
     * @param reminder 提醒实体
     * @return 影响行数
     */
    int insertOaSmartReminder(OaSmartReminder reminder);
    
    /**
     * 更新智能提醒
     *
     * @param reminder 提醒实体
     * @return 影响行数
     */
    int updateOaSmartReminder(OaSmartReminder reminder);
    
    /**
     * 删除智能提醒
     *
     * @param reminderId 提醒ID
     * @return 影响行数
     */
    int deleteOaSmartReminderById(Long reminderId);
}
