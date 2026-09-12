package com.lingxi.engagement.api;

import java.util.List;

/** 跨端同步、触达与系统日历公开门面。 */
public interface EngagementFacade {
  NotificationPreferenceResult updatePreference(UpdateNotificationPreferenceCommand command);

  long scheduleNotification(ScheduleNotificationCommand command);

  List<NotificationResult> listNotifications(long userId, long cursor, int limit);

  void markNotificationRead(long userId, long messageId);

  CalendarBindingResult bindCalendar(CalendarBindingCommand command);

  /** 查询本人全部日历绑定，供客户端展示当前绑定与撤销入口。 */
  List<CalendarBindingResult> listCalendarBindings(long userId);

  long projectCalendar(CalendarProjectionCommand command);

  CalendarBindingResult revokeCalendar(RevokeCalendarCommand command);

  SyncPageResult getChanges(long userId, long cursor, List<String> domains, int limit);

  OfflineCommandResult applyOfflineCommand(OfflineSyncCommand command);
}
