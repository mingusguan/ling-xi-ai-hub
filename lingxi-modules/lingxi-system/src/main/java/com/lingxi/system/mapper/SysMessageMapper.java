package com.lingxi.system.mapper;

import com.lingxi.system.domain.SysMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统消息 Mapper 接口
 *
 * @author lingxi
 */
@Mapper
public interface SysMessageMapper {
    
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
}
