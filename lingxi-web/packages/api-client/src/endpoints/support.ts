import type { ApiClient, LongId } from '../http';
import type {
  ClientBootstrapResult,
  GuardianInvitationResult,
  GuardianPermissionType,
  GuardianRelationResult,
  SupportTicketResult
} from '../types';

/** 监护与客服等辅助能力客户端。 */
export class GuardianApi {
  constructor(private readonly client: ApiClient) {}

  /** 青少年发起监护邀请；invitationToken 只在创建时返回一次。 */
  inviteGuardian(
    permissions: GuardianPermissionType[],
    idempotencyKey: string
  ): Promise<GuardianInvitationResult> {
    return this.client.send<GuardianInvitationResult>('/api/v1/guardian-relations/invitations', {
      method: 'POST',
      body: { permissions },
      idempotencyKey
    });
  }

  /** 成人接受监护邀请。 */
  acceptInvitation(invitationToken: string): Promise<GuardianRelationResult> {
    return this.client.send<GuardianRelationResult>(
      '/api/v1/guardian-relations/invitations/acceptance',
      { method: 'POST', body: { invitationToken } }
    );
  }

  /** 撤销监护关系，必须填写原因。 */
  revokeRelation(relationId: LongId, reason: string): Promise<GuardianRelationResult> {
    return this.client.send<GuardianRelationResult>(`/api/v1/guardian-relations/${relationId}`, {
      method: 'DELETE',
      body: { reason }
    });
  }

  /** 查询监护关系。 */
  getRelation(relationId: LongId): Promise<GuardianRelationResult> {
    return this.client.send<GuardianRelationResult>(`/api/v1/guardian-relations/${relationId}`);
  }

  /** 提交监护关系争议。 */
  submitDispute(relationId: LongId, reason: string): Promise<unknown> {
    return this.client.send<unknown>('/api/v1/guardian-disputes', {
      method: 'POST',
      body: { relationId, reason }
    });
  }
}

/** 客服工单客户端。 */
export class SupportApi {
  constructor(private readonly client: ApiClient) {}

  /** 创建工单；priority 只允许 URGENT/HIGH/NORMAL/LOW。 */
  createTicket(body: {
    category: string;
    subject: string;
    description: string;
    priority: 'URGENT' | 'HIGH' | 'NORMAL' | 'LOW';
  }): Promise<SupportTicketResult> {
    return this.client.send<SupportTicketResult>('/api/v1/support-tickets', {
      method: 'POST',
      body
    });
  }

  /** 查询本人工单。 */
  getTicket(ticketId: LongId): Promise<SupportTicketResult> {
    return this.client.send<SupportTicketResult>(`/api/v1/support-tickets/${ticketId}`);
  }
}

/** 运行时治理客户端：两端共用同一 bootstrap 契约。 */
export class RuntimeApi {
  constructor(private readonly client: ApiClient) {}

  /** 拉取升级策略、合规文档、功能开关与实验分组。 */
  bootstrap(platform: 'PC_WEB' | 'HARMONY', currentVersionCode = 0): Promise<ClientBootstrapResult> {
    return this.client.send<ClientBootstrapResult>('/api/v1/runtime/bootstrap', {
      query: { platform, currentVersionCode },
      anonymous: true
    });
  }
}
