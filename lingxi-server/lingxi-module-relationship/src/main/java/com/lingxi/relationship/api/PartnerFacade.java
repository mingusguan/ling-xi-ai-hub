package com.lingxi.relationship.api;

import com.lingxi.kernel.PageResult;
import java.util.List;

/** 伙伴关系、授权与受控分享公开门面。 */
public interface PartnerFacade {
  PartnerRelationResult invite(InvitePartnerCommand command);

  PartnerRelationResult accept(AcceptPartnerCommand command);

  PartnerRelationResult terminate(
      long operatorUserId, long relationId, long expectedVersion, String reason);

  /** 分页查询与本人相关的伙伴关系（邀请方或被邀请方）。 */
  PageResult<PartnerRelationResult> listRelations(long participantUserId, int page, int pageSize);

  /** 查询本人作为授权方发出的全部目标授权。 */
  List<PartnerGrantResult> listGrants(long ownerUserId);

  PartnerGrantResult updateGrant(UpdatePartnerGrantCommand command);

  PartnerGrantResult revokeGrant(long ownerUserId, long grantId, long expectedVersion);

  PartnerInteractionResult interact(CreatePartnerInteractionCommand command);

  ReportResult report(CreateReportCommand command);

  boolean authorizeGoal(long actorUserId, long goalId, PartnerPermission permission);

  ShareLinkResult createShare(CreateShareCommand command);

  /** 分页查询本人创建的分享链接；响应不含明文 token（只在创建时返回一次）。 */
  PageResult<ShareLinkResult> listShares(long ownerUserId, int page, int pageSize);

  ShareLinkResult accessShare(String rawToken, String password);

  void revokeShare(long ownerUserId, long shareId, long expectedVersion);
}
