<script setup lang="ts">
import { computed, ref } from 'vue';
import type { PrivacyRequestResult, PrivacyRequestType } from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

const session = useSessionStore();
const requestType = ref<PrivacyRequestType>('EXPORT');
const modules = ref<string[]>([]);
const requests = ref<PrivacyRequestResult[]>([]);
const exportContent = ref<string | null>(null);
const appealBirthDate = ref('');
const appealEvidence = ref('');
const ticket = ref({ category: 'PRIVACY', subject: '', description: '', priority: 'NORMAL' as const });
const message = ref<string | null>(null);
const errorMessage = ref<string | null>(null);
const busy = ref(false);

const availableModules = [
  'identity',
  'goal',
  'companion',
  'engagement',
  'relationship',
  'content',
  'commerce',
  'operations'
];

const typeLabels: Record<PrivacyRequestType, string> = {
  EXPORT: '导出我的数据',
  CORRECTION: '更正我的数据',
  DELETE_DATA: '删除我的数据',
  CLOSE_ACCOUNT: '注销账号（7 天冷静期）'
};

const scopeHint = computed(() =>
  requestType.value === 'CLOSE_ACCOUNT'
    ? '注销必须覆盖全部模块，服务端会拒绝部分范围。'
    : '不选择模块表示全部模块。'
);

function toggleModule(name: string): void {
  modules.value = modules.value.includes(name)
    ? modules.value.filter((item) => item !== name)
    : [...modules.value, name];
}

async function createRequest(): Promise<void> {
  busy.value = true;
  errorMessage.value = null;
  message.value = null;
  try {
    await session.ensureFreshToken();
    const scopeJson =
      requestType.value === 'CLOSE_ACCOUNT' || modules.value.length === 0
        ? '{}'
        : JSON.stringify({ modules: modules.value });
    const result = await api.identity.createPrivacyRequest(
      { type: requestType.value, scopeJson },
      api.http.newIdempotencyKey()
    );
    requests.value = [result, ...requests.value];
    message.value = `请求 #${result.requestId} 已受理，状态 ${result.status}。`;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '请求创建失败';
  } finally {
    busy.value = false;
  }
}

async function refresh(requestId: string): Promise<void> {
  const result = await api.identity.getPrivacyRequest(requestId);
  requests.value = requests.value.map((item) => (item.requestId === requestId ? result : item));
}

async function cancelClosure(requestId: string): Promise<void> {
  try {
    const result = await api.identity.cancelPrivacyRequest(requestId);
    requests.value = requests.value.map((item) => (item.requestId === requestId ? result : item));
    message.value = '注销请求已撤销。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '撤销失败';
  }
}

async function download(requestId: string): Promise<void> {
  try {
    const result = await api.identity.downloadPrivacyExport(requestId);
    exportContent.value = result.content;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '导出下载失败';
  }
}

async function submitAppeal(): Promise<void> {
  try {
    await api.identity.submitAgeAppeal(appealBirthDate.value, appealEvidence.value);
    message.value = '年龄申诉已提交，等待人工审核。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '申诉提交失败';
  }
}

async function createTicket(): Promise<void> {
  try {
    const result = await api.support.createTicket(ticket.value);
    message.value = `工单 ${result.ticketNo} 已创建，状态 ${result.status}。`;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '工单创建失败';
  }
}
</script>

<template>
  <section class="lx-card">
    <h2>隐私权利</h2>
    <p class="lx-muted">
      导出、更正、删除与注销都由服务端统一编排各模块；注销有 7 天冷静期，期间可撤销。
      这些权利不依赖商业开关。
    </p>

    <div class="lx-field">
      <label>请求类型</label>
      <select v-model="requestType" class="lx-select">
        <option v-for="(label, key) in typeLabels" :key="key" :value="key">{{ label }}</option>
      </select>
    </div>

    <div class="lx-field">
      <label>数据范围</label>
      <div class="lx-row scope">
        <label v-for="name in availableModules" :key="name" class="lx-row">
          <input
            type="checkbox"
            :checked="modules.includes(name)"
            :disabled="requestType === 'CLOSE_ACCOUNT'"
            @change="toggleModule(name)"
          />
          {{ name }}
        </label>
      </div>
      <small class="lx-muted">{{ scopeHint }}</small>
    </div>

    <p v-if="message" class="lx-tag">{{ message }}</p>
    <p v-if="errorMessage" class="lx-error">{{ errorMessage }}</p>
    <button class="lx-button" :disabled="busy" @click="createRequest">提交请求</button>
  </section>

  <section class="lx-card privacy__block">
    <h3>我的请求</h3>
    <div v-if="requests.length === 0" class="lx-empty">暂无请求记录（服务端未提供列表接口，仅展示本次会话提交的请求）。</div>
    <ul v-else class="requests">
      <li v-for="item in requests" :key="item.requestId" class="request">
        <div>
          <strong>#{{ item.requestId }} · {{ typeLabels[item.type] }}</strong>
          <div class="lx-muted">
            状态 {{ item.status }} · 进度 {{ item.progress }}% · 截止
            {{ new Date(item.deadline).toLocaleDateString('zh-CN') }}
          </div>
        </div>
        <div class="lx-row">
          <button class="lx-button lx-button--ghost" @click="refresh(item.requestId)">刷新</button>
          <button
            v-if="item.type === 'EXPORT' && item.status === 'COMPLETED'"
            class="lx-button lx-button--ghost"
            @click="download(item.requestId)"
          >
            下载导出
          </button>
          <button
            v-if="item.type === 'CLOSE_ACCOUNT' && item.status !== 'COMPLETED'"
            class="lx-button lx-button--ghost"
            @click="cancelClosure(item.requestId)"
          >
            撤销注销
          </button>
        </div>
      </li>
    </ul>
    <pre v-if="exportContent" class="export">{{ exportContent }}</pre>
  </section>

  <section class="lx-card privacy__block">
    <h3>年龄申诉</h3>
    <p class="lx-muted">对年龄分层有异议时提交已核验证据，由后台人工审核。</p>
    <div class="lx-row">
      <input v-model="appealBirthDate" class="lx-input" type="date" />
      <input v-model="appealEvidence" class="lx-input" placeholder="证据引用编号" />
      <button class="lx-button" @click="submitAppeal">提交申诉</button>
    </div>
  </section>

  <section class="lx-card privacy__block">
    <h3>联系客服</h3>
    <div class="lx-field">
      <label>主题</label>
      <input v-model="ticket.subject" class="lx-input" />
    </div>
    <div class="lx-field">
      <label>问题描述</label>
      <textarea v-model="ticket.description" class="lx-textarea" rows="3" />
    </div>
    <button class="lx-button" @click="createTicket">创建工单</button>
  </section>
</template>

<style scoped>
.privacy__block {
  margin-top: var(--lx-space-5);
}

.scope {
  flex-wrap: wrap;
}

.requests {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-3);
  list-style: none;
  margin: 0;
  padding: 0;
}

.request {
  align-items: center;
  border: 1px solid var(--lx-color-border);
  border-radius: var(--lx-radius-sm);
  display: flex;
  justify-content: space-between;
  padding: var(--lx-space-3);
}

.export {
  background: var(--lx-color-surface-muted);
  border-radius: var(--lx-radius-sm);
  max-height: 320px;
  overflow: auto;
  padding: var(--lx-space-3);
}
</style>
