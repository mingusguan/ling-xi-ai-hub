<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import type { PartnerPermission } from '@lingxi/api-client';
import { useGoalStore } from '@/stores/goal';
import { usePartnerStore } from '@/stores/partner';

const partner = usePartnerStore();
const goalStore = useGoalStore();
const notice = ref<string | null>(null);

const inviteForm = reactive({ inviteeUserId: '' });
const grantForm = reactive({
  relationId: '',
  goalId: '',
  permissions: ['VIEW_PROGRESS'] as PartnerPermission[],
  expiresAt: ''
});
/** 服务端要求分享必须带过期时间且晚于当前时刻；默认给出 7 天。 */
function defaultShareExpiry(): string {
  const target = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${target.getFullYear()}-${pad(target.getMonth() + 1)}-${pad(target.getDate())}T${pad(target.getHours())}:${pad(target.getMinutes())}`;
}

const shareForm = reactive({
  resourceType: 'GOAL',
  resourceId: '',
  fields: ['title', 'progress'],
  expiresAt: defaultShareExpiry(),
  visitLimit: '',
  password: ''
});

const permissionLabels: Record<PartnerPermission, string> = {
  VIEW_PROGRESS: '查看进度',
  ENCOURAGE: '鼓励互动',
  COMMENT: '留言评论',
  CO_CHECK_IN: '共同打卡'
};

const relationStatusLabels: Record<string, string> = {
  INVITED: '待接受',
  ACTIVE: '已生效',
  TERMINATED: '已终止',
  REJECTED: '已拒绝'
};

/** 可分享的字段必须在服务端 GOAL_FIELDS 白名单内。 */
const shareableFields = ['title', 'successCriteria', 'status', 'progress'];

onMounted(async () => {
  await Promise.all([
    goalStore.loadGoals(),
    partner.loadRelations(),
    partner.loadGrants(),
    partner.loadShares()
  ]);
});

function formatTime(value: string | null): string {
  return value ? new Date(value).toLocaleString('zh-CN') : '—';
}

function goalTitle(goalId: string): string {
  return goalStore.goals.find((goal) => goal.goalId === goalId)?.title ?? '目标不在当前列表';
}

function togglePermission(permission: PartnerPermission): void {
  grantForm.permissions = grantForm.permissions.includes(permission)
    ? grantForm.permissions.filter((item) => item !== permission)
    : [...grantForm.permissions, permission];
}

function toggleField(field: string): void {
  shareForm.fields = shareForm.fields.includes(field)
    ? shareForm.fields.filter((item) => item !== field)
    : [...shareForm.fields, field];
}

async function invite(): Promise<void> {
  notice.value = null;
  if (!inviteForm.inviteeUserId.trim()) {
    notice.value = '请填写对方用户标识';
    return;
  }
  const created = await partner.invite(inviteForm.inviteeUserId.trim());
  notice.value = created ? `邀请已发出（关系 ${created.relationId}）` : partner.errorMessage;
  if (created) {
    inviteForm.inviteeUserId = '';
  }
}

async function accept(relationId: string): Promise<void> {
  const relation = partner.relations.find((item) => item.relationId === relationId);
  if (!relation) {
    return;
  }
  const updated = await partner.accept(relation);
  notice.value = updated ? '邀请已接受' : partner.errorMessage;
}

async function terminate(relationId: string): Promise<void> {
  const relation = partner.relations.find((item) => item.relationId === relationId);
  if (!relation) {
    return;
  }
  const updated = await partner.terminate(relation);
  notice.value = updated ? '伙伴关系已终止' : partner.errorMessage;
}

async function saveGrant(): Promise<void> {
  notice.value = null;
  if (!grantForm.relationId.trim() || !grantForm.goalId.trim()) {
    notice.value = '请选择伙伴关系与目标';
    return;
  }
  if (grantForm.permissions.length === 0) {
    notice.value = '至少保留一个授权权限';
    return;
  }
  const existing = partner.grants.find(
    (item) => item.relationId === grantForm.relationId && item.goalId === grantForm.goalId
  );
  const updated = await partner.updateGrant(
    grantForm.relationId.trim(),
    grantForm.goalId.trim(),
    grantForm.permissions,
    grantForm.expiresAt ? new Date(grantForm.expiresAt).toISOString() : null,
    existing?.version ?? '0'
  );
  notice.value = updated ? '授权已保存' : partner.errorMessage;
}

async function revokeGrant(grantId: string): Promise<void> {
  const grant = partner.grants.find((item) => item.grantId === grantId);
  if (!grant) {
    return;
  }
  const ok = await partner.revokeGrant(grant);
  notice.value = ok ? '授权已撤销' : partner.errorMessage;
}

async function createShare(): Promise<void> {
  notice.value = null;
  if (!shareForm.resourceId.trim()) {
    notice.value = '请填写分享资源标识';
    return;
  }
  if (shareForm.fields.length === 0) {
    notice.value = '至少选择一个分享字段';
    return;
  }
  if (!shareForm.expiresAt) {
    notice.value = '分享必须设置过期时间（服务端强制）';
    return;
  }
  if (new Date(shareForm.expiresAt).getTime() <= Date.now()) {
    notice.value = '过期时间必须晚于当前时刻';
    return;
  }
  const created = await partner.createShare({
    resourceType: shareForm.resourceType,
    resourceId: shareForm.resourceId.trim(),
    fields: shareForm.fields,
    expiresAt: new Date(shareForm.expiresAt).toISOString(),
    visitLimit: shareForm.visitLimit ? Number(shareForm.visitLimit) : null,
    password: shareForm.password.trim() === '' ? null : shareForm.password.trim()
  });
  notice.value = created ? '分享链接已创建，明文 token 只显示一次' : partner.errorMessage;
}

async function revokeShare(shareId: string): Promise<void> {
  const share = partner.shares.find((item) => item.shareId === shareId);
  if (!share) {
    return;
  }
  const ok = await partner.revokeShare(share);
  notice.value = ok ? '分享链接已撤销' : partner.errorMessage;
}

async function copyToken(): Promise<void> {
  if (!partner.latestShareToken) {
    return;
  }
  try {
    await navigator.clipboard.writeText(partner.latestShareToken);
    notice.value = '分享 token 已复制';
  } catch {
    notice.value = '浏览器禁止写入剪贴板，请手工复制';
  }
}
</script>

<template>
  <section class="lx-card">
    <header class="lx-row partner__head">
      <div>
        <h2>伙伴与分享</h2>
        <p class="lx-muted">
          伙伴双方都必须是成人账号；授权按目标最小化，仅覆盖服务端白名单字段。
          分享必须设置过期时间且晚于当前时刻，token 只在创建时返回一次、列表不回显；
          所有撤销走乐观版本。
        </p>
      </div>
      <span class="lx-tag">
        {{ partner.activeRelations.length }} 个生效伙伴 · {{ partner.activeShares.length }} 个有效分享
      </span>
    </header>

    <p v-if="partner.errorMessage" class="lx-error">{{ partner.errorMessage }}</p>
    <p v-if="notice" class="lx-muted">{{ notice }}</p>

    <h3 class="partner__section">邀请伙伴</h3>
    <div class="lx-row">
      <input
        v-model="inviteForm.inviteeUserId"
        class="lx-input"
        inputmode="numeric"
        placeholder="对方用户标识（userId）"
      />
      <button class="lx-button" @click="invite">发出邀请</button>
    </div>

    <h3 class="partner__section">
      伙伴关系 <span class="lx-tag">共 {{ partner.relationTotal }} 条</span>
    </h3>
    <div v-if="partner.relations.length === 0" class="lx-empty">还没有伙伴关系。</div>
    <table v-else class="lx-table">
      <thead>
        <tr>
          <th>关系</th>
          <th>邀请方</th>
          <th>被邀请方</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="relation in partner.relations" :key="relation.relationId">
          <td>{{ relation.relationId }}</td>
          <td>{{ relation.inviterUserId }}</td>
          <td>{{ relation.inviteeUserId }}</td>
          <td>
            <span class="lx-tag" :class="{ 'lx-tag--warn': relation.status !== 'ACTIVE' }">
              {{ relationStatusLabels[relation.status] ?? relation.status }}
            </span>
          </td>
          <td class="lx-row">
            <button
              v-if="relation.status === 'INVITED'"
              class="lx-button lx-button--ghost"
              @click="accept(relation.relationId)"
            >
              接受
            </button>
            <button
              v-if="relation.status === 'ACTIVE' || relation.status === 'INVITED'"
              class="lx-button lx-button--ghost"
              @click="terminate(relation.relationId)"
            >
              终止
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <h3 class="partner__section">目标授权</h3>
    <div class="partner__form">
      <label class="lx-muted">
        伙伴关系
        <select v-model="grantForm.relationId" class="lx-input">
          <option value="">请选择</option>
          <option v-for="relation in partner.activeRelations" :key="relation.relationId" :value="relation.relationId">
            {{ relation.relationId }}（对方 {{ relation.inviterUserId }} / {{ relation.inviteeUserId }}）
          </option>
        </select>
      </label>
      <label class="lx-muted">
        目标
        <select v-model="grantForm.goalId" class="lx-input">
          <option value="">请选择</option>
          <option v-for="goal in goalStore.goals" :key="goal.goalId" :value="goal.goalId">
            {{ goal.title }}
          </option>
        </select>
      </label>
      <label class="lx-muted">
        过期时间（可留空）
        <input v-model="grantForm.expiresAt" class="lx-input" type="datetime-local" />
      </label>
    </div>
    <div class="lx-row partner__chips">
      <button
        v-for="permission in (['VIEW_PROGRESS', 'ENCOURAGE', 'COMMENT', 'CO_CHECK_IN'] as PartnerPermission[])"
        :key="permission"
        class="lx-button"
        :class="{ 'lx-button--ghost': !grantForm.permissions.includes(permission) }"
        @click="togglePermission(permission)"
      >
        {{ permissionLabels[permission] }}
      </button>
      <button class="lx-button" @click="saveGrant">保存授权</button>
    </div>

    <div v-if="partner.grants.length === 0" class="lx-empty">还没有发出任何授权。</div>
    <table v-else class="lx-table">
      <thead>
        <tr>
          <th>授权</th>
          <th>伙伴关系</th>
          <th>目标</th>
          <th>权限</th>
          <th>过期时间</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="grant in partner.grants" :key="grant.grantId">
          <td>{{ grant.grantId }}</td>
          <td>{{ grant.relationId }}</td>
          <td>{{ goalTitle(grant.goalId) }}</td>
          <td>{{ grant.permissions.map((item) => permissionLabels[item] ?? item).join('、') }}</td>
          <td>{{ formatTime(grant.expiresAt) }}</td>
          <td>
            <span class="lx-tag" :class="{ 'lx-tag--warn': grant.status !== 'ACTIVE' }">
              {{ grant.status }}
            </span>
          </td>
          <td>
            <button
              v-if="grant.status === 'ACTIVE'"
              class="lx-button lx-button--ghost"
              @click="revokeGrant(grant.grantId)"
            >
              撤销
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <h3 class="partner__section">分享链接</h3>
    <div class="partner__form">
      <label class="lx-muted">
        资源类型
        <select v-model="shareForm.resourceType" class="lx-input">
          <option value="GOAL">目标</option>
          <option value="ACHIEVEMENT">成就</option>
        </select>
      </label>
      <label class="lx-muted">
        资源标识
        <input v-model="shareForm.resourceId" class="lx-input" inputmode="numeric" placeholder="目标标识" />
      </label>
      <label class="lx-muted">
        过期时间（必填）
        <input v-model="shareForm.expiresAt" class="lx-input" type="datetime-local" required />
      </label>
      <label class="lx-muted">
        访问次数上限（可留空）
        <input v-model="shareForm.visitLimit" class="lx-input" inputmode="numeric" />
      </label>
      <label class="lx-muted">
        访问密码（可留空）
        <input v-model="shareForm.password" class="lx-input" />
      </label>
    </div>
    <div class="lx-row partner__chips">
      <button
        v-for="field in shareableFields"
        :key="field"
        class="lx-button"
        :class="{ 'lx-button--ghost': !shareForm.fields.includes(field) }"
        @click="toggleField(field)"
      >
        {{ field }}
      </button>
      <button class="lx-button" @click="createShare">创建分享链接</button>
    </div>
    <div v-if="partner.latestShareToken" class="lx-card partner__token">
      <strong>本次分享 token（只显示一次）</strong>
      <code>{{ partner.latestShareToken }}</code>
      <button class="lx-button lx-button--ghost" @click="copyToken">复制</button>
    </div>

    <div v-if="partner.shares.length === 0" class="lx-empty">还没有分享链接。</div>
    <table v-else class="lx-table">
      <thead>
        <tr>
          <th>分享</th>
          <th>资源</th>
          <th>字段</th>
          <th>访问</th>
          <th>过期时间</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="share in partner.shares" :key="share.shareId">
          <td>{{ share.shareId }}</td>
          <td>{{ share.resourceType }}/{{ share.resourceId }}</td>
          <td>{{ share.fields.join('、') }}</td>
          <td>{{ share.visitCount }} / {{ share.visitLimit ?? '不限' }}</td>
          <td>{{ formatTime(share.expiresAt) }}</td>
          <td>
            <span class="lx-tag" :class="{ 'lx-tag--warn': share.status !== 'ACTIVE' }">
              {{ share.status }}
            </span>
          </td>
          <td>
            <button
              v-if="share.status === 'ACTIVE'"
              class="lx-button lx-button--ghost"
              @click="revokeShare(share.shareId)"
            >
              撤销
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.partner__head {
  justify-content: space-between;
}

.partner__section {
  margin: var(--lx-space-5) 0 var(--lx-space-3);
}

.partner__form {
  display: grid;
  gap: var(--lx-space-3);
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

.partner__chips {
  flex-wrap: wrap;
  margin: var(--lx-space-3) 0;
}

.partner__token {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-2);
  margin: var(--lx-space-3) 0;
  word-break: break-all;
}
</style>
