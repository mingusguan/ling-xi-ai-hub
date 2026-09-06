package com.lingxi.identity.api;

import java.util.List;

/** 为严重安全事件提供最小监护人通知目标，不返回监护关系详情。 */
public interface GuardianNotificationProvider {
  List<Long> activeGuardianUserIds(long teenUserId);
}
