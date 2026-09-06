package com.lingxi.identity.api;

/** 有效监护关系建立后激活青少年账号。 */
public record ActivateTeenCommand(long teenUserId, long guardianRelationId) {}
