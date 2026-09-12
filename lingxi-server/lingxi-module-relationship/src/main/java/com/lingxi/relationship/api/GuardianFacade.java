package com.lingxi.relationship.api;

import com.lingxi.kernel.PageResult;

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

  /** 当前用户相关的监护关系列表：既包含自己作为青少年被监护的关系，也包含自己作为监护人发起的关系。 */
  PageResult<GuardianRelationResult> listRelations(long participantUserId, int page, int pageSize);
}
