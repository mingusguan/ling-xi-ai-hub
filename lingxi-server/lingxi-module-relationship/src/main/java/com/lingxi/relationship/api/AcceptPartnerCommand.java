package com.lingxi.relationship.api;

/** 被邀请人接受伙伴邀请。 */
public record AcceptPartnerCommand(long relationId, long inviteeUserId, long expectedVersion) {}
