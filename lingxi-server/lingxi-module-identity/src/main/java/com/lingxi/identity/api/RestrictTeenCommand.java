package com.lingxi.identity.api;

/** 最后有效监护关系失效后限制青少年账号。 */
public record RestrictTeenCommand(long teenUserId, long guardianRelationId, String reason) {}
