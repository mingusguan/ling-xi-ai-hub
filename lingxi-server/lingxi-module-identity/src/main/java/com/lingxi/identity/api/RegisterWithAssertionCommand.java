package com.lingxi.identity.api;

/** 使用受信身份断言完成注册的命令。 */
public record RegisterWithAssertionCommand(
    String requestKey, String signedAssertion, String timezone) {}
