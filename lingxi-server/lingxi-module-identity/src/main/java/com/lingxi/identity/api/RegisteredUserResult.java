package com.lingxi.identity.api;

/**
 * 已登记用户结果。
 *
 * @param userId 内部用户标识
 * @param publicId 对客户端公开的不可枚举标识
 * @param ageBand 年龄分层
 * @param status 账号状态
 * @param authorizationVersion 授权版本
 */
public record RegisteredUserResult(
    long userId,
    String publicId,
    AgeBand ageBand,
    AccountStatus status,
    long authorizationVersion) {}
