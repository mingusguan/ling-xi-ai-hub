<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { MemoryResult } from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

const session = useSessionStore();
const memories = ref<MemoryResult[]>([]);
const editingId = ref<string | null>(null);
const draft = ref('');
const loading = ref(false);
const errorMessage = ref<string | null>(null);
const notice = ref<string | null>(null);

const sensitivityLabels: Record<string, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高'
};

onMounted(load);

async function load(): Promise<void> {
  loading.value = true;
  errorMessage.value = null;
  try {
    await session.ensureFreshToken();
    memories.value = await api.companion.listMemories();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '记忆加载失败';
  } finally {
    loading.value = false;
  }
}

function startEdit(memory: MemoryResult): void {
  editingId.value = memory.id;
  draft.value = memory.contentText;
  notice.value = null;
}

function cancelEdit(): void {
  editingId.value = null;
  draft.value = '';
}

/** 保存记忆更正；expectedVersion 必须来自当前记录，服务端按乐观并发校验。 */
async function save(memoryId: string): Promise<void> {
  const memory = memories.value.find((item) => item.id === memoryId);
  if (!memory || !draft.value.trim()) {
    return;
  }
  notice.value = null;
  try {
    await session.ensureFreshToken();
    const updated = await api.companion.updateMemory(memoryId, draft.value.trim(), memory.version);
    memories.value = memories.value.map((item) => (item.id === updated.id ? updated : item));
    editingId.value = null;
    notice.value = '记忆已更新';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '记忆更新失败';
  }
}

/** 删除记忆；服务端异步传播删除，列表以服务端返回状态为准。 */
async function remove(memoryId: string): Promise<void> {
  const memory = memories.value.find((item) => item.id === memoryId);
  if (!memory) {
    return;
  }
  notice.value = null;
  try {
    await session.ensureFreshToken();
    const updated = await api.companion.deleteMemory(memoryId, memory.version);
    memories.value = memories.value.map((item) => (item.id === updated.id ? updated : item));
    notice.value = '删除请求已受理，服务端会异步传播';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '记忆删除失败';
  }
}

function formatTime(value: string): string {
  return new Date(value).toLocaleString('zh-CN');
}
</script>

<template>
  <section class="lx-card">
    <header class="lx-row memory__head">
      <div>
        <h2>记忆管理</h2>
        <p class="lx-muted">
          长期记忆由灵犀在对话中沉淀；你可以查看、更正或删除。删除会传播到相关会话与派生数据，
          敏感度较高的记忆默认不参与跨场景引用。
        </p>
      </div>
      <div class="lx-row">
        <span class="lx-tag">共 {{ memories.length }} 条</span>
        <button class="lx-button lx-button--ghost" :disabled="loading" @click="load">刷新</button>
      </div>
    </header>

    <p v-if="errorMessage" class="lx-error">{{ errorMessage }}</p>
    <p v-if="notice" class="lx-muted">{{ notice }}</p>
    <div v-if="memories.length === 0" class="lx-empty">还没有长期记忆。</div>

    <ul v-else class="memory__list">
      <li v-for="memory in memories" :key="memory.id" class="lx-card memory__item">
        <div class="memory__meta">
          <span class="lx-tag">{{ memory.purpose }}</span>
          <span class="lx-tag" :class="{ 'lx-tag--warn': memory.sensitivity === 'HIGH' }">
            敏感度 {{ sensitivityLabels[memory.sensitivity] ?? memory.sensitivity }}
          </span>
          <span class="lx-tag" :class="{ 'lx-tag--warn': memory.status !== 'ACTIVE' }">
            {{ memory.status }}
          </span>
          <span class="lx-muted">来源 {{ memory.sourceRef }}</span>
          <span class="lx-muted">更新 {{ formatTime(memory.updatedAt) }}</span>
        </div>

        <template v-if="editingId === memory.id">
          <textarea v-model="draft" class="lx-input" rows="3" />
          <div class="lx-row">
            <button class="lx-button" @click="save(memory.id)">保存</button>
            <button class="lx-button lx-button--ghost" @click="cancelEdit">取消</button>
          </div>
        </template>
        <template v-else>
          <p class="memory__content">{{ memory.contentText }}</p>
          <div class="lx-row">
            <button
              class="lx-button lx-button--ghost"
              :disabled="memory.status !== 'ACTIVE'"
              @click="startEdit(memory)"
            >
              更正
            </button>
            <button
              class="lx-button lx-button--ghost"
              :disabled="memory.status !== 'ACTIVE'"
              @click="remove(memory.id)"
            >
              删除
            </button>
          </div>
        </template>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.memory__head {
  justify-content: space-between;
}

.memory__list {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-3);
  list-style: none;
  margin: var(--lx-space-4) 0 0;
  padding: 0;
}

.memory__item {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-3);
}

.memory__meta {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--lx-space-3);
}

.memory__content {
  margin: 0;
  white-space: pre-wrap;
}
</style>
