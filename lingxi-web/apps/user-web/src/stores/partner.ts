import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type {
  PartnerGrantResult,
  PartnerPermission,
  PartnerRelationResult,
  ShareLinkResult
} from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

/** 伙伴关系、目标授权与受控分享状态。 */
export const usePartnerStore = defineStore('partner', () => {
  const relations = ref<PartnerRelationResult[]>([]);
  /** 关系总数为服务端 long，按字符串展示，禁止 Number 转换。 */
  const relationTotal = ref<string>('0');
  const grants = ref<PartnerGrantResult[]>([]);
  const shares = ref<ShareLinkResult[]>([]);
  const shareTotal = ref<string>('0');
  /** 最近一次创建的分享链接，rawToken 只在此处保留一次。 */
  const latestShareToken = ref<string | null>(null);
  const loading = ref(false);
  const errorMessage = ref<string | null>(null);

  const activeRelations = computed(() =>
    relations.value.filter((item) => item.status === 'ACTIVE')
  );
  const pendingRelations = computed(() =>
    relations.value.filter((item) => item.status === 'INVITED')
  );
  const activeShares = computed(() =>
    shares.value.filter((item) => item.status === 'ACTIVE')
  );

  async function guard<T>(action: () => Promise<T>): Promise<T | null> {
    const session = useSessionStore();
    loading.value = true;
    errorMessage.value = null;
    try {
      await session.ensureFreshToken();
      return await action();
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '请求失败';
      return null;
    } finally {
      loading.value = false;
    }
  }

  /** 加载与本人相关的伙伴关系。 */
  async function loadRelations(page = 1, pageSize = 50): Promise<void> {
    const result = await guard(() => api.partner.listRelations(page, pageSize));
    if (result) {
      relations.value = result.items;
      relationTotal.value = result.total;
    }
  }

  /** 加载本人发出的目标授权。 */
  async function loadGrants(): Promise<void> {
    const result = await guard(() => api.partner.listGrants());
    if (result) {
      grants.value = result;
    }
  }

  /** 加载本人创建的分享链接。 */
  async function loadShares(page = 1, pageSize = 50): Promise<void> {
    const result = await guard(() => api.partner.listShares(page, pageSize));
    if (result) {
      shares.value = result.items;
      shareTotal.value = result.total;
    }
  }

  /** 邀请伙伴；双方都必须是通过服务端年龄校验的成人账号。 */
  async function invite(inviteeUserId: string): Promise<PartnerRelationResult | null> {
    const created = await guard(() =>
      api.partner.invite(inviteeUserId, api.http.newIdempotencyKey())
    );
    if (created) {
      await loadRelations();
    }
    return created;
  }

  /** 接受伙伴邀请。 */
  async function accept(relation: PartnerRelationResult): Promise<PartnerRelationResult | null> {
    const updated = await guard(() => api.partner.accept(relation.relationId, relation.version));
    if (updated) {
      relations.value = relations.value.map((item) =>
        item.relationId === updated.relationId ? updated : item
      );
    }
    return updated;
  }

  /** 终止伙伴关系。 */
  async function terminate(relation: PartnerRelationResult): Promise<PartnerRelationResult | null> {
    const updated = await guard(() =>
      api.partner.terminate(relation.relationId, relation.version)
    );
    if (updated) {
      relations.value = relations.value.map((item) =>
        item.relationId === updated.relationId ? updated : item
      );
      await loadGrants();
    }
    return updated;
  }

  /** 新增或更新对某目标的伙伴授权。 */
  async function updateGrant(
    relationId: string,
    goalId: string,
    permissions: PartnerPermission[],
    expiresAt: string | null,
    expectedVersion: string
  ): Promise<PartnerGrantResult | null> {
    const updated = await guard(() =>
      api.partner.updateGrant(relationId, { goalId, permissions, expiresAt, expectedVersion })
    );
    if (updated) {
      const exists = grants.value.some((item) => item.grantId === updated.grantId);
      grants.value = exists
        ? grants.value.map((item) => (item.grantId === updated.grantId ? updated : item))
        : [updated, ...grants.value];
    }
    return updated;
  }

  /** 撤销伙伴授权。 */
  async function revokeGrant(grant: PartnerGrantResult): Promise<boolean> {
    const result = await guard(() => api.partner.revokeGrant(grant.grantId, grant.version));
    if (result) {
      await loadGrants();
      return true;
    }
    return false;
  }

  /** 创建受控分享；rawToken 只在本响应中返回一次。 */
  async function createShare(input: {
    resourceType: string;
    resourceId: string;
    fields: string[];
    expiresAt: string | null;
    visitLimit: number | null;
    password: string | null;
  }): Promise<ShareLinkResult | null> {
    const created = await guard(() =>
      api.partner.createShare(input, api.http.newIdempotencyKey())
    );
    if (created) {
      latestShareToken.value = created.rawToken;
      await loadShares();
    }
    return created;
  }

  /** 撤销分享链接。 */
  async function revokeShare(share: ShareLinkResult): Promise<boolean> {
    const session = useSessionStore();
    loading.value = true;
    errorMessage.value = null;
    try {
      await session.ensureFreshToken();
      await api.partner.revokeShare(share.shareId, share.version);
      await loadShares();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '撤销失败';
      return false;
    } finally {
      loading.value = false;
    }
  }

  return {
    relations,
    relationTotal,
    grants,
    shares,
    shareTotal,
    latestShareToken,
    activeRelations,
    pendingRelations,
    activeShares,
    loading,
    errorMessage,
    loadRelations,
    loadGrants,
    loadShares,
    invite,
    accept,
    terminate,
    updateGrant,
    revokeGrant,
    createShare,
    revokeShare
  };
});
