import type { ApiClient, LongId, PageResult } from '../http';
import type {
  PartnerGrantResult,
  PartnerPermission,
  PartnerRelationResult,
  ShareLinkResult
} from '../types';

/** 伙伴邀请、授权与受控分享接口客户端。 */
export class PartnerApi {
  constructor(private readonly client: ApiClient) {}

  /** 分页查询与本人相关的伙伴关系（邀请方或被邀请方）。 */
  listRelations(page = 1, pageSize = 20): Promise<PageResult<PartnerRelationResult>> {
    return this.client.send<PageResult<PartnerRelationResult>>('/api/v1/relationships/partners', {
      query: { page, pageSize }
    });
  }

  /** 邀请伙伴；双方都必须是成人账号，服务端按年龄策略校验。 */
  invite(inviteeUserId: LongId, idempotencyKey: string): Promise<PartnerRelationResult> {
    return this.client.send<PartnerRelationResult>('/api/v1/relationships/partners', {
      method: 'POST',
      body: { inviteeUserId },
      idempotencyKey
    });
  }

  /** 接受伙伴邀请。 */
  accept(relationId: LongId, expectedVersion: LongId): Promise<PartnerRelationResult> {
    return this.client.send<PartnerRelationResult>(
      `/api/v1/relationships/partners/${relationId}/accept`,
      { method: 'POST', body: { expectedVersion } }
    );
  }

  /** 终止伙伴关系。 */
  terminate(
    relationId: LongId,
    expectedVersion: LongId,
    reason = 'TERMINATE'
  ): Promise<PartnerRelationResult> {
    return this.client.send<PartnerRelationResult>(
      `/api/v1/relationships/partners/${relationId}`,
      { method: 'DELETE', query: { expectedVersion, reason } }
    );
  }

  /** 查询本人作为授权方发出的全部目标授权（含已过期与已撤销）。 */
  listGrants(): Promise<PartnerGrantResult[]> {
    return this.client.send<PartnerGrantResult[]>('/api/v1/relationships/grants');
  }

  /** 新增或更新伙伴对某目标的授权。 */
  updateGrant(
    relationId: LongId,
    body: {
      goalId: LongId;
      permissions: PartnerPermission[];
      expiresAt: string | null;
      expectedVersion: LongId;
    }
  ): Promise<PartnerGrantResult> {
    return this.client.send<PartnerGrantResult>(
      `/api/v1/relationships/partners/${relationId}/grants`,
      { method: 'PUT', body }
    );
  }

  /** 撤销伙伴授权。 */
  revokeGrant(grantId: LongId, expectedVersion: LongId): Promise<PartnerGrantResult> {
    return this.client.send<PartnerGrantResult>(`/api/v1/relationships/grants/${grantId}`, {
      method: 'DELETE',
      query: { expectedVersion }
    });
  }

  /** 分页查询本人创建的分享链接；列表不回显明文 token。 */
  listShares(page = 1, pageSize = 20): Promise<PageResult<ShareLinkResult>> {
    return this.client.send<PageResult<ShareLinkResult>>('/api/v1/shares', {
      query: { page, pageSize }
    });
  }

  /** 创建受控分享链接；rawToken 只在本次响应中返回一次。 */
  createShare(
    body: {
      resourceType: string;
      resourceId: string;
      fields: string[];
      expiresAt: string | null;
      visitLimit: number | null;
      password: string | null;
    },
    idempotencyKey: string
  ): Promise<ShareLinkResult> {
    return this.client.send<ShareLinkResult>('/api/v1/shares', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 撤销分享链接。 */
  revokeShare(shareId: LongId, expectedVersion: LongId): Promise<void> {
    return this.client.send<void>(`/api/v1/shares/${shareId}`, {
      method: 'DELETE',
      query: { expectedVersion }
    });
  }

  /** 凭 token 访问分享内容；命中访问上限或已过期时返回业务错误。 */
  accessShare(token: string, password: string | null = null): Promise<ShareLinkResult> {
    return this.client.send<ShareLinkResult>('/api/v1/shares/access', {
      method: 'POST',
      body: { token, password }
    });
  }

  /** 提交举报；targetType 为 GOAL/PARTNER/SHARE 等资源类型。 */
  report(body: {
    targetType: string;
    targetId: string;
    reasonCode: string;
    evidenceReference: string | null;
  }): Promise<unknown> {
    return this.client.send<unknown>('/api/v1/reports', { method: 'POST', body });
  }
}
