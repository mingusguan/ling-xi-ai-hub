package com.lingxi.kernel;

import java.time.ZoneId;

/**
 * 账号级免打扰时段提供方。
 *
 * <p>引导画像里的免打扰时段属于账号级设置，而判定「现在能不能打扰用户」在触达模块。
 * 通过 kernel 端口而不是直接依赖身份模块，避免触达与身份之间形成模块循环依赖。
 *
 * <p>返回 null 表示用户没有设置账号级免打扰，调用方应按「不限制」处理。
 */
public interface QuietHoursProvider {

  /**
   * 读取某用户的账号级免打扰时段。
   *
   * @param userId 用户标识
   * @param zone 判定所用时区；通常取账号时区或通知偏好时区
   * @return 免打扰时段；用户未设置时返回 null
   */
  QuietHoursWindow findQuietHours(long userId, ZoneId zone);

  /**
   * 读取某用户的账号时区。
   *
   * <p>通知偏好里没有时区时用它兜底，避免把「按 Asia/Shanghai 判定」这种假设
   * 硬编码进触达逻辑——用户在国外时免打扰时段会整段错位。
   *
   * @return 账号时区；用户不存在时返回 null
   */
  ZoneId findAccountZone(long userId);
}
