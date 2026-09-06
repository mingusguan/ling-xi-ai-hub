package com.lingxi.relationship.api;

/** 伙伴关系、授权与受控分享公开门面。 */
public interface PartnerFacade {
  PartnerRelationResult invite(InvitePartnerCommand command);

  PartnerRelationResult accept(AcceptPartnerCommand command);

  PartnerRelationResult terminate(
      long operatorUserId, long relationId, long expectedVersion, String reason);

  PartnerGrantResult updateGrant(UpdatePartnerGrantCommand command);

  PartnerGrantResult revokeGrant(long ownerUserId, long grantId, long expectedVersion);

  PartnerInteractionResult interact(CreatePartnerInteractionCommand command);

  ReportResult report(CreateReportCommand command);

  boolean authorizeGoal(long actorUserId, long goalId, PartnerPermission permission);

  ShareLinkResult createShare(CreateShareCommand command);

  ShareLinkResult accessShare(String rawToken, String password);

  void revokeShare(long ownerUserId, long shareId, long expectedVersion);
}
