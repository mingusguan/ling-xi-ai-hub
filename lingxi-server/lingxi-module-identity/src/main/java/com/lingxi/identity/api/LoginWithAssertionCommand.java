package com.lingxi.identity.api;

/** 使用受信身份断言登录的命令。 */
public record LoginWithAssertionCommand(
    String requestKey, String signedAssertion, String deviceId) {}
