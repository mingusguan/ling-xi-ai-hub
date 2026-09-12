<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import type { CalendarBindingResult } from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

const session = useSessionStore();
const bindings = ref<CalendarBindingResult[]>([]);
const loading = ref(false);
const errorMessage = ref<string | null>(null);
const notice = ref<string | null>(null);

const bindForm = reactive({
  provider: 'HARMONY_CALENDAR',
  credentialReference: ''
});

const providerLabels: Record<string, string> = {
  HARMONY_CALENDAR: '华为日历',
  GOOGLE_CALENDAR: 'Google 日历',
  APPLE_CALENDAR: 'Apple 日历'
};

onMounted(load);

async function load(): Promise<void> {
  loading.value = true;
  errorMessage.value = null;
  try {
    await session.ensureFreshToken();
    bindings.value = await api.engagement.listCalendarBindings();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '日历绑定加载失败';
  } finally {
    loading.value = false;
  }
}

/** 绑定日历；需要近期认证，凭据只以引用形式提交，服务端不存明文。 */
async function bind(): Promise<void> {
  notice.value = null;
  if (!bindForm.credentialReference.trim()) {
    notice.value = '请填写授权凭据引用（credentialReference）';
    return;
  }
  try {
    await session.ensureFreshToken();
    const binding = await api.engagement.bindCalendar(
      { provider: bindForm.provider, credentialReference: bindForm.credentialReference.trim() },
      api.http.newIdempotencyKey()
    );
    notice.value = `日历绑定状态 ${binding.status}`;
    bindForm.credentialReference = '';
    await load();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '日历绑定失败';
  }
}

/** 撤销日历绑定；deleteCreatedEvents 决定是否同时删除已投影事件。 */
async function revoke(bindingId: string, deleteCreatedEvents: boolean): Promise<void> {
  notice.value = null;
  try {
    await session.ensureFreshToken();
    const binding = await api.engagement.revokeCalendar(bindingId, deleteCreatedEvents);
    notice.value = `日历绑定状态 ${binding.status}`;
    await load();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '撤销失败';
  }
}

/** 重新授权需要新的凭据引用：把渠道填回表单，由用户重新提交凭据。 */
function prepareReauthorize(provider: string): void {
  bindForm.provider = provider;
  bindForm.credentialReference = '';
  notice.value = `请在“授权凭据引用”中填入 ${providerLabels[provider] ?? provider} 的新凭据后点击“绑定日历”完成重新授权`;
}
</script>

<template>
  <section class="lx-card">
    <header class="lx-row calendar__head">
      <div>
        <h2>日历同步</h2>
        <p class="lx-muted">
          方向固定为“灵犀行动 → 外部日历”，外部日历的修改不会反向覆盖行动计划。
          绑定与撤销都需要近期认证；撤销时可选择是否同时删除已投影的事件。
        </p>
      </div>
      <span class="lx-tag">{{ bindings.length }} 个绑定</span>
    </header>

    <p v-if="errorMessage" class="lx-error">{{ errorMessage }}</p>
    <p v-if="notice" class="lx-muted">{{ notice }}</p>

    <div class="calendar__form">
      <label class="lx-muted">
        渠道
        <select v-model="bindForm.provider" class="lx-input">
          <option value="HARMONY_CALENDAR">华为日历</option>
          <option value="GOOGLE_CALENDAR">Google 日历</option>
          <option value="APPLE_CALENDAR">Apple 日历</option>
        </select>
      </label>
      <label class="lx-muted">
        授权凭据引用
        <input
          v-model="bindForm.credentialReference"
          class="lx-input"
          placeholder="服务端凭据引用，不提交明文令牌"
        />
      </label>
      <button class="lx-button" :disabled="loading" @click="bind">绑定日历</button>
      <button class="lx-button lx-button--ghost" :disabled="loading" @click="load">刷新</button>
    </div>

    <div v-if="bindings.length === 0" class="lx-empty">还没有绑定任何外部日历。</div>
    <table v-else class="lx-table">
      <thead>
        <tr>
          <th>绑定</th>
          <th>渠道</th>
          <th>状态</th>
          <th>版本</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="binding in bindings" :key="binding.bindingId">
          <td>{{ binding.bindingId }}</td>
          <td>{{ providerLabels[binding.provider] ?? binding.provider }}</td>
          <td>
            <span class="lx-tag" :class="{ 'lx-tag--warn': binding.status !== 'ACTIVE' }">
              {{ binding.status }}
            </span>
          </td>
          <td>{{ binding.version }}</td>
          <td class="lx-row">
            <button
              v-if="binding.status === 'ACTIVE'"
              class="lx-button lx-button--ghost"
              @click="revoke(binding.bindingId, false)"
            >
              仅撤销授权
            </button>
            <button
              v-if="binding.status === 'ACTIVE'"
              class="lx-button lx-button--ghost"
              @click="revoke(binding.bindingId, true)"
            >
              撤销并删除事件
            </button>
            <button
              v-if="binding.status !== 'ACTIVE'"
              class="lx-button lx-button--ghost"
              @click="prepareReauthorize(binding.provider)"
            >
              重新授权
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.calendar__head {
  justify-content: space-between;
}

.calendar__form {
  align-items: flex-end;
  display: grid;
  gap: var(--lx-space-3);
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  margin: var(--lx-space-4) 0;
}
</style>
