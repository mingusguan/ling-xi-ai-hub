<script setup lang="ts">
import { ref } from 'vue';
import type { GuardianPermissionType, GuardianRelationResult } from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

const session = useSessionStore();
const permissions = ref<GuardianPermissionType[]>(['VIEW_GOAL_PROGRESS', 'RECEIVE_SAFETY_ALERTS']);
const invitationToken = ref<string | null>(null);
const acceptToken = ref('');
const relationId = ref('');
const relation = ref<GuardianRelationResult | null>(null);
const revokeReason = ref('');
const disputeReason = ref('');
const message = ref<string | null>(null);
const errorMessage = ref<string | null>(null);
const busy = ref(false);

const permissionLabels: Record<GuardianPermissionType, string> = {
  VIEW_GOAL_PROGRESS: '查看目标进度',
  MANAGE_SCHEDULE: '管理日程安排',
  RECEIVE_SAFETY_ALERTS: '接收安全告警'
};

function toggle(permission: GuardianPermissionType): void {
  permissions.value = permissions.value.includes(permission)
    ? permissions.value.filter((item) => item !== permission)
    : [...permissions.value, permission];
}

async function invite(): Promise<void> {
  busy.value = true;
  errorMessage.value = null;
  message.value = null;
  try {
    const result = await api.guardian.inviteGuardian(permissions.value, api.http.newIdempotencyKey());
    invitationToken.value = result.invitationToken;
    relationId.value = String(result.relationId);
    message.value = '邀请已创建，请把邀请令牌交给监护人；令牌只在创建时返回一次，3 天内有效。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '邀请创建失败';
  } finally {
    busy.value = false;
  }
}

async function accept(): Promise<void> {
  busy.value = true;
  errorMessage.value = null;
  try {
    relation.value = await api.guardian.acceptInvitation(acceptToken.value.trim());
    message.value = '监护关系已生效。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '接受邀请失败';
  } finally {
    busy.value = false;
  }
}

async function loadRelation(): Promise<void> {
  if (!relationId.value.trim()) {
    return;
  }
  try {
    relation.value = await api.guardian.getRelation(relationId.value.trim());
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '关系查询失败';
  }
}

async function revoke(): Promise<void> {
  if (!relationId.value.trim() || !revokeReason.value.trim()) {
    errorMessage.value = '撤销监护关系必须填写原因';
    return;
  }
  busy.value = true;
  try {
    relation.value = await api.guardian.revokeRelation(relationId.value.trim(), revokeReason.value.trim());
    message.value = '监护关系已撤销；最后一个监护人失效时青少年账号会被立即限制。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '撤销失败';
  } finally {
    busy.value = false;
  }
}

async function submitDispute(): Promise<void> {
  if (!relationId.value.trim() || !disputeReason.value.trim()) {
    return;
  }
  try {
    await api.guardian.submitDispute(relationId.value.trim(), disputeReason.value.trim());
    message.value = '争议已提交，处理期间监护关系变更会被冻结。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '争议提交失败';
  }
}
</script>

<template>
  <section class="lx-card">
    <h2>监护与青少年模式</h2>
    <p class="lx-muted">
      当前身份：{{ session.profile?.ageBand ?? '未知' }} / {{ session.profile?.accountStatus ?? '未知' }}。
      14—17 周岁必须绑定监护人并由服务端强制青少年模式；监护人默认不能读取完整私密对话。
    </p>
    <p v-if="message" class="lx-tag">{{ message }}</p>
    <p v-if="errorMessage" class="lx-error">{{ errorMessage }}</p>
  </section>

  <section class="lx-card guardian__block">
    <h3>青少年：发起监护邀请</h3>
    <div class="lx-row guardian__permissions">
      <label v-for="(label, key) in permissionLabels" :key="key" class="lx-row">
        <input
          type="checkbox"
          :checked="permissions.includes(key)"
          @change="toggle(key)"
        />
        {{ label }}
      </label>
    </div>
    <button class="lx-button" :disabled="busy" @click="invite">生成邀请令牌</button>
    <p v-if="invitationToken" class="guardian__token">
      邀请令牌：<code>{{ invitationToken }}</code>
    </p>
  </section>

  <section class="lx-card guardian__block">
    <h3>成人：接受监护邀请</h3>
    <div class="lx-row">
      <input v-model="acceptToken" class="lx-input" placeholder="粘贴邀请令牌" />
      <button class="lx-button" :disabled="busy" @click="accept">接受邀请</button>
    </div>
  </section>

  <section class="lx-card guardian__block">
    <h3>关系查询、撤销与争议</h3>
    <div class="lx-row">
      <input v-model="relationId" class="lx-input" placeholder="关系标识 relationId" />
      <button class="lx-button lx-button--ghost" @click="loadRelation">查询关系</button>
    </div>
    <div v-if="relation" class="relation">
      <p>
        关系 #{{ relation.relationId }} · 状态 {{ relation.status }} · 权限
        {{ relation.permissions.join('、') || '无' }}
      </p>
      <p class="lx-muted">
        未成年人 {{ relation.teenUserId }} · 监护人 {{ relation.guardianUserId ?? '未绑定' }} · 生效时间
        {{ relation.effectiveAt ? new Date(relation.effectiveAt).toLocaleString('zh-CN') : '未生效' }}
      </p>
    </div>
    <div class="lx-field">
      <label>撤销原因</label>
      <input v-model="revokeReason" class="lx-input" />
    </div>
    <button class="lx-button lx-button--ghost" :disabled="busy" @click="revoke">撤销监护关系</button>
    <div class="lx-field">
      <label>争议说明</label>
      <input v-model="disputeReason" class="lx-input" placeholder="监护关系存在争议时提交人工处理" />
    </div>
    <button class="lx-button lx-button--ghost" @click="submitDispute">提交争议</button>
  </section>
</template>

<style scoped>
.guardian__block {
  margin-top: var(--lx-space-5);
}

.guardian__permissions {
  flex-wrap: wrap;
  margin-bottom: var(--lx-space-3);
}

.guardian__token code {
  background: var(--lx-color-surface-muted);
  border-radius: var(--lx-radius-sm);
  padding: 2px 6px;
  word-break: break-all;
}

.relation {
  background: var(--lx-color-surface-muted);
  border-radius: var(--lx-radius-sm);
  margin: var(--lx-space-3) 0;
  padding: var(--lx-space-3);
}
</style>
