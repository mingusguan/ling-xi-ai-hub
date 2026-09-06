package com.lingxi.identity.api;

/**
 * 其他业务模块执行服务端授权时使用的最小账号视图。
 *
 * @param userId 用户标识
 * @param ageBand 年龄分层
 * @param status 账号状态
 * @param authorizationVersion 授权版本
 * @param coreFeaturesAllowed 是否允许使用普通核心功能
 */
public record AccessProfile(
    long userId,
    AgeBand ageBand,
    AccountStatus status,
    long authorizationVersion,
    boolean coreFeaturesAllowed) {}
