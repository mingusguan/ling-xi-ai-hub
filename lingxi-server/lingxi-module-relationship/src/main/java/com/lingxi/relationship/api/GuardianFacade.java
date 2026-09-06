package com.lingxi.relationship.api;

/** 监护关系公开门面。 */
public interface GuardianFacade {
  /** 青少年账号创建监护邀请。 */
  GuardianInvitationResult createInvitation(CreateGuardianInvitationCommand command);

  /** 成人账号接受并激活监护关系。 */
  GuardianRelationResult acceptInvitation(AcceptGuardianInvitationCommand command);

  /** 关系任一方撤销关系，最后监护人失效时立即限制青少年账号。 */
  GuardianRelationResult revokeRelation(RevokeGuardianRelationCommand command);

  /** 查询关系详情并校验参与者。 */
  GuardianRelationResult getRelation(long relationId, long participantUserId);
}
