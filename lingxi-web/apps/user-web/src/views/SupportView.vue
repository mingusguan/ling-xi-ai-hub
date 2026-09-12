<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRecordStore } from '@/stores/record';
import { api } from '@/api/client';

const recordStore = useRecordStore();
const submitting = ref(false);
const notice = ref<string | null>(null);

const form = reactive({
  category: 'PRODUCT_FEEDBACK',
  subject: '',
  description: '',
  priority: 'NORMAL' as 'URGENT' | 'HIGH' | 'NORMAL' | 'LOW'
});

const categories = [
  { value: 'PRODUCT_FEEDBACK', label: '产品建议' },
  { value: 'BUG_REPORT', label: '功能异常' },
  { value: 'ACCOUNT_ISSUE', label: '账号问题' },
  { value: 'PRIVACY_CORRECTION', label: '隐私更正' },
  { value: 'ACCOUNT_CLOSURE_TRANSACTION', label: '账号注销' }
];

const statusLabels: Record<string, string> = {
  OPEN: '待处理',
  ASSIGNED: '已受理',
  IN_PROGRESS: '处理中',
  WAITING_USER: '待你补充',
  RESOLVED: '已解决',
  CLOSED: '已关闭'
};

const selectedTicket = computed(() => recordStore.currentTicket);

onMounted(() => {
  void recordStore.loadTickets();
});

function formatTime(value: string): string {
  return new Date(value).toLocaleString('zh-CN');
}

async function submit(): Promise<void> {
  if (!form.subject.trim() || !form.description.trim()) {
    return;
  }
  submitting.value = true;
  notice.value = null;
  try {
    const created = await api.support.createTicket({
      category: form.category,
      subject: form.subject.trim(),
      description: form.description.trim(),
      priority: form.priority
    });
    notice.value = `工单已提交，编号 ${created.ticketNo}`;
    form.subject = '';
    form.description = '';
    await recordStore.loadTickets();
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '工单提交失败';
  } finally {
    submitting.value = false;
  }
}

async function openTicket(ticketId: string): Promise<void> {
  await recordStore.loadTicket(ticketId);
}
</script>

<template>
  <section class="lx-card">
    <header class="lx-row support__head">
      <div>
        <h2>客服工单</h2>
        <p class="lx-muted">
          工单提交后由人工受理；状态与处理进度以服务端返回为准，客户端不提供状态修改入口。
        </p>
      </div>
      <span class="lx-tag">共 {{ recordStore.ticketTotal }} 条</span>
    </header>

    <form class="lx-card support__form" @submit.prevent="submit">
      <h3>提交新工单</h3>
      <label class="lx-muted">
        问题类型
        <select v-model="form.category" class="lx-input">
          <option v-for="item in categories" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>
      <label class="lx-muted">
        优先级
        <select v-model="form.priority" class="lx-input">
          <option value="URGENT">紧急</option>
          <option value="HIGH">高</option>
          <option value="NORMAL">普通</option>
          <option value="LOW">低</option>
        </select>
      </label>
      <label class="lx-muted">
        标题
        <input v-model="form.subject" class="lx-input" maxlength="120" placeholder="一句话描述问题" />
      </label>
      <label class="lx-muted">
        详细描述
        <textarea v-model="form.description" class="lx-input" rows="4" placeholder="补充复现步骤或诉求" />
      </label>
      <div class="lx-row">
        <button class="lx-button" type="submit" :disabled="submitting">提交工单</button>
        <span v-if="notice" class="lx-muted">{{ notice }}</span>
      </div>
    </form>

    <p v-if="recordStore.errorMessage" class="lx-error">{{ recordStore.errorMessage }}</p>
    <div v-if="recordStore.tickets.length === 0" class="lx-empty">还没有工单记录。</div>
    <table v-else class="lx-table">
      <thead>
        <tr>
          <th>工单号</th>
          <th>标题</th>
          <th>类型</th>
          <th>状态</th>
          <th>优先级</th>
          <th>提交时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="ticket in recordStore.tickets" :key="ticket.ticketId">
          <td>{{ ticket.ticketNo }}</td>
          <td>{{ ticket.subject }}</td>
          <td>{{ ticket.category }}</td>
          <td>
            <span class="lx-tag" :class="{ 'lx-tag--warn': ticket.status !== 'RESOLVED' }">
              {{ statusLabels[ticket.status] ?? ticket.status }}
            </span>
          </td>
          <td>{{ ticket.priority }}</td>
          <td>{{ formatTime(ticket.createdAt) }}</td>
          <td>
            <button class="lx-button lx-button--ghost" @click="openTicket(ticket.ticketId)">
              查看
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <article v-if="selectedTicket" class="lx-card support__detail">
      <h3>{{ selectedTicket.ticketNo }} · {{ selectedTicket.subject }}</h3>
      <p class="lx-muted">
        状态 {{ statusLabels[selectedTicket.status] ?? selectedTicket.status }} ·
        优先级 {{ selectedTicket.priority }} · 提交时间 {{ formatTime(selectedTicket.createdAt) }}
      </p>
    </article>
  </section>
</template>

<style scoped>
.support__head {
  justify-content: space-between;
}

.support__form {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-3);
  margin: var(--lx-space-4) 0;
}

.support__detail {
  margin-top: var(--lx-space-4);
}
</style>
