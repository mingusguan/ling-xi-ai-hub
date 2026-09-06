package com.lingxi.relationship.api;

/** 撤销监护关系命令。 */
public record RevokeGuardianRelationCommand(long relationId, long operatorUserId, String reason) {}
