package com.lingxi.engagement.domain;

import com.lingxi.engagement.api.NotificationChannel;
import com.lingxi.kernel.BusinessException;
import java.time.*;
import java.util.*;

/** 用户通知偏好聚合；青少年强制免打扰不能被用户关闭。 */
public class NotificationPreference {
  private final long userId;
  private final String scene;
  private Set<NotificationChannel> channels;
  private LocalTime quietStart;
  private LocalTime quietEnd;
  private String timezone;
  private long version;
  private LocalDateTime updatedAt;

  private NotificationPreference(
      long userId,
      String scene,
      Set<NotificationChannel> channels,
      LocalTime quietStart,
      LocalTime quietEnd,
      String timezone,
      long version,
      LocalDateTime updatedAt) {
    this.userId = userId;
    this.scene = scene;
    this.channels = Set.copyOf(channels);
    this.quietStart = quietStart;
    this.quietEnd = quietEnd;
    ZoneId.of(timezone);
    this.timezone = timezone;
    this.version = version;
    this.updatedAt = updatedAt;
  }

  public static NotificationPreference create(
      long userId,
      String scene,
      Set<NotificationChannel> channels,
      LocalTime quietStart,
      LocalTime quietEnd,
      String timezone,
      boolean teen,
      LocalDateTime now) {
    validate(userId, scene, channels);
    return new NotificationPreference(
        userId,
        scene,
        channels,
        teen ? LocalTime.of(22, 0) : quietStart,
        teen ? LocalTime.of(7, 0) : quietEnd,
        timezone,
        0,
        now);
  }

  public static NotificationPreference rehydrate(
      long userId,
      String scene,
      Set<NotificationChannel> channels,
      LocalTime quietStart,
      LocalTime quietEnd,
      String timezone,
      long version,
      LocalDateTime updatedAt) {
    return new NotificationPreference(
        userId, scene, channels, quietStart, quietEnd, timezone, version, updatedAt);
  }

  public void update(
      Set<NotificationChannel> nextChannels,
      LocalTime nextStart,
      LocalTime nextEnd,
      String nextTimezone,
      boolean teen,
      long expectedVersion,
      LocalDateTime now) {
    if (version != expectedVersion) {
      throw new BusinessException("ENG_PREFERENCE_CONFLICT", "通知偏好已在其他终端更新");
    }
    validate(userId, scene, nextChannels);
    ZoneId.of(nextTimezone);
    channels = Set.copyOf(nextChannels);
    quietStart = teen ? LocalTime.of(22, 0) : nextStart;
    quietEnd = teen ? LocalTime.of(7, 0) : nextEnd;
    timezone = nextTimezone;
    version++;
    updatedAt = now;
  }

  public boolean allows(NotificationChannel channel, Instant instant, boolean safetyCritical) {
    if (!channels.contains(channel)) {
      return false;
    }
    if (safetyCritical || quietStart == null || quietEnd == null) {
      return true;
    }
    LocalTime local = instant.atZone(ZoneId.of(timezone)).toLocalTime();
    boolean quiet =
        quietStart.isBefore(quietEnd)
            ? !local.isBefore(quietStart) && local.isBefore(quietEnd)
            : !local.isBefore(quietStart) || local.isBefore(quietEnd);
    return !quiet;
  }

  private static void validate(long userId, String scene, Set<NotificationChannel> channels) {
    if (userId <= 0 || scene == null || scene.isBlank() || channels == null) {
      throw new BusinessException("ENG_INVALID_PREFERENCE", "通知偏好参数不合法");
    }
  }

  public long getUserId() {
    return userId;
  }

  public String getScene() {
    return scene;
  }

  public Set<NotificationChannel> getChannels() {
    return channels;
  }

  public LocalTime getQuietStart() {
    return quietStart;
  }

  public LocalTime getQuietEnd() {
    return quietEnd;
  }

  public String getTimezone() {
    return timezone;
  }

  public long getVersion() {
    return version;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
