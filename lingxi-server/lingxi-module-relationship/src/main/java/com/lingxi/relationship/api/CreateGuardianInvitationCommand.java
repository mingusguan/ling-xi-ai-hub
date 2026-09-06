package com.lingxi.relationship.api;

import java.util.Set;

/** 创建监护邀请命令。 */
public record CreateGuardianInvitationCommand(
    String requestKey, long teenUserId, Set<GuardianPermissionType> permissions) {}
