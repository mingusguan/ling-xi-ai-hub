package com.lingxi.identity.api;

import java.time.LocalDate;

/**
 * 手机号登录（或首次注册）命令。
 *
 * @param phoneNumber 手机号，允许带 +86 前缀与分隔符，服务端统一归一化
 * @param birthDate 出生日期；仅首次使用该手机号登录时必填，用于 14+ 准入与年龄分层
 * @param deviceId 设备标识，刷新令牌时必须与登录时一致
 * @param timezone 用户时区，仅在首次注册时使用
 */
public record PhoneLoginCommand(
    String phoneNumber, LocalDate birthDate, String deviceId, String timezone) {}
