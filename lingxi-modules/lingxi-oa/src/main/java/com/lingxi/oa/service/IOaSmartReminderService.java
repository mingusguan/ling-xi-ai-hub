package com.lingxi.oa.service;

import com.lingxi.oa.domain.OaSmartReminder;

import java.util.List;

public interface IOaSmartReminderService {

    List<OaSmartReminder> selectOaSmartReminderList(OaSmartReminder reminder);

    OaSmartReminder selectOaSmartReminderById(Long reminderId);

    int insertOaSmartReminder(OaSmartReminder reminder);

    int updateOaSmartReminder(OaSmartReminder reminder);

    int deleteOaSmartReminderById(Long reminderId);

    int countUnreadReminders(Long userId);

    boolean markAsRead(Long reminderId, Long userId);

    boolean sendReminder(OaSmartReminder reminder);
}
