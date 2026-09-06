package com.lingxi.identity.api;

import java.time.LocalDate;

/**
 * 可信身份适配器完成年龄验证后提交的注册命令。
 *
 * @param requestKey 注册幂等键
 * @param verifiedBirthDate 已验证出生日期
 * @param verificationMethod 验证渠道或方法
 * @param evidenceReference 安全证据引用，不得传入证件明文
 * @param timezone IANA 用户时区
 */
public record RegisterVerifiedUserCommand(
    String requestKey,
    LocalDate verifiedBirthDate,
    String verificationMethod,
    String evidenceReference,
    String timezone) {}
