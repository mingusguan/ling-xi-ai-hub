package com.lingxi.system.service.impl;

import com.lingxi.system.domain.SysMessage;
import com.lingxi.system.mapper.SysMessageMapper;
import com.lingxi.system.service.ISysMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 系统消息 Service 业务层处理
 *
 * @author lingxi
 */
@Service
public class SysMessageServiceImpl implements ISysMessageService {
    private static final Logger log = LoggerFactory.getLogger(SysMessageServiceImpl.class);

    @Autowired
    private SysMessageMapper messageMapper;

    @Override
    public List<SysMessage> selectSysMessageList(SysMessage message) {
        return messageMapper.selectSysMessageList(message);
    }

    @Override
    public SysMessage selectSysMessageById(Long messageId) {
        return messageMapper.selectSysMessageById(messageId);
    }

    @Override
    public int insertSysMessage(SysMessage message) {
        return messageMapper.insertSysMessage(message);
    }

    @Override
    public int updateSysMessage(SysMessage message) {
        return messageMapper.updateSysMessage(message);
    }

    @Override
    public int deleteSysMessageById(Long messageId) {
        return messageMapper.deleteSysMessageById(messageId);
    }

    @Override
    public int countUnreadMessages(Long userId) {
        return messageMapper.countUnreadMessages(userId);
    }

    @Override
    public boolean markAsRead(Long messageId, Long userId) {
        SysMessage message = messageMapper.selectSysMessageById(messageId);
        if (message == null || !message.getUserId().equals(userId)) {
            return false;
        }
        message.setStatus("1");
        message.setReadTime(new Date());
        return messageMapper.updateSysMessage(message) > 0;
    }

    @Override
    public boolean sendMessage(SysMessage message) {
        try {
            message.setSendTime(new Date());
            message.setStatus("0");
            return messageMapper.insertSysMessage(message) > 0;
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return false;
        }
    }
}
