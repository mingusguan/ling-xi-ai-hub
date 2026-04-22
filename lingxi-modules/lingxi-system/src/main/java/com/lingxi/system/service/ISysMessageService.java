package com.lingxi.system.service;

import com.lingxi.system.domain.SysMessage;

import java.util.List;

/**
 * 系统消息 Service 接口
 *
 * @author lingxi
 */
public interface ISysMessageService {
    
    /**
     * 查询消息列表
     *
     * @param message 消息条件
     * @return 消息列表
     */
    List<SysMessage> selectSysMessageList(SysMessage message);
    
    /**
     * 根据ID查询消息
     *
     * @param messageId 消息ID
     * @return 消息实体
     */
    SysMessage selectSysMessageById(Long messageId);
    
    /**
     * 插入消息
     *
     * @param message 消息实体
     * @return 影响行数
     */
    int insertSysMessage(SysMessage message);
    
    /**
     * 更新消息
     *
     * @param message 消息实体
     * @return 影响行数
     */
    int updateSysMessage(SysMessage message);
    
    /**
     * 删除消息
     *
     * @param messageId 消息ID
     * @return 影响行数
     */
    int deleteSysMessageById(Long messageId);
    
    /**
     * 统计用户未读消息数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    int countUnreadMessages(Long userId);
    
    /**
     * 标记消息为已读
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markAsRead(Long messageId, Long userId);
    
    /**
     * 发送消息
     *
     * @param message 消息实体
     * @return 是否成功
     */
    boolean sendMessage(SysMessage message);
}
