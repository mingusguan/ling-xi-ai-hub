package com.lingxi.engagement.api;

import java.util.List;

/** 跨端同步、触达与系统日历公开门面。 */
public interface EngagementFacade {
  NotificationPreferenceResult updatePreference(UpdateNotificationPreferenceCommand command);

  long scheduleNotification(ScheduleNotificationCommand command);

  List<NotificationResult> listNotifications(long userId, long cursor, int limit);

  void markNotificationRead(long userId, long messageId);

  CalendarBindingResult bindCalendar(CalendarBindingCommand command);

  long projectCalendar(CalendarProjectionCommand command);

  CalendarBindingResult revokeCalendar(RevokeCalendarCommand command);

  SyncPageResult getChanges(long userId, long cursor, List<String> domains, int limit);

  OfflineCommandResult applyOfflineCommand(OfflineSyncCommand command);
}
