package com.lingxi.relationship.api;

/** 主动邀请指定用户成为同行伙伴。 */
public record InvitePartnerCommand(String requestKey, long inviterUserId, long inviteeUserId) {}
