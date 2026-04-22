package com.lingxi.oa.service.impl;

import com.lingxi.oa.domain.OaSmartReminder;
import com.lingxi.oa.mapper.OaSmartReminderMapper;
import com.lingxi.oa.service.IOaSmartReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class OaSmartReminderServiceImpl implements IOaSmartReminderService {
    private static final Logger log = LoggerFactory.getLogger(OaSmartReminderServiceImpl.class);

    @Autowired
    private OaSmartReminderMapper reminderMapper;

    @Override
    public List<OaSmartReminder> selectOaSmartReminderList(OaSmartReminder reminder) {
        return reminderMapper.selectOaSmartReminderList(reminder);
    }

    @Override
    public OaSmartReminder selectOaSmartReminderById(Long reminderId) {
        return reminderMapper.selectOaSmartReminderById(reminderId);
    }

    @Override
    public int insertOaSmartReminder(OaSmartReminder reminder) {
        return reminderMapper.insertOaSmartReminder(reminder);
    }

    @Override
    public int updateOaSmartReminder(OaSmartReminder reminder) {
        return reminderMapper.updateOaSmartReminder(reminder);
    }

    @Override
    public int deleteOaSmartReminderById(Long reminderId) {
        return reminderMapper.deleteOaSmartReminderById(reminderId);
    }

    @Override
    public int countUnreadReminders(Long userId) {
        OaSmartReminder reminder = new OaSmartReminder();
        reminder.setUserId(userId);
        reminder.setStatus("0");
        return reminderMapper.selectOaSmartReminderList(reminder).size();
    }

    @Override
    public boolean markAsRead(Long reminderId, Long userId) {
        OaSmartReminder reminder = reminderMapper.selectOaSmartReminderById(reminderId);
        if (reminder == null || !reminder.getUserId().equals(userId)) {
            return false;
        }
        reminder.setStatus("1");
        reminder.setReadTime(new Date());
        return reminderMapper.updateOaSmartReminder(reminder) > 0;
    }

    @Override
    public boolean sendReminder(OaSmartReminder reminder) {
        try {
            reminder.setSendTime(new Date());
            reminder.setStatus("0");
            return reminderMapper.insertOaSmartReminder(reminder) > 0;
        } catch (Exception e) {
            log.error("发送提醒消息失败", e);
            return false;
        }
    }
}
