package com.lingxi.relationship.api;

/** 成人监护人接受邀请命令。 */
public record AcceptGuardianInvitationCommand(String invitationToken, long guardianUserId) {}
