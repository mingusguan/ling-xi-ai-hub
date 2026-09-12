<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import type { AgentRunResult, ConversationResult, MemoryResult } from '@lingxi/api-client';
import { parseAgentEvent } from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

const session = useSessionStore();
const conversation = ref<ConversationResult | null>(null);
const run = ref<AgentRunResult | null>(null);
const events = ref<{ type: string; payload: string }[]>([]);
const memories = ref<MemoryResult[]>([]);
const errorMessage = ref<string | null>(null);
const busy = ref(false);
const input = ref('');
const conversationForm = reactive({ scene: 'PLANNING', title: '目标规划' });

onMounted(async () => {
  await session.ensureFreshToken();
  try {
    memories.value = await api.companion.listMemories();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '记忆加载失败';
  }
});

async function createConversation(): Promise<void> {
  busy.value = true;
  errorMessage.value = null;
  try {
    conversation.value = await api.companion.createConversation(
      { scene: conversationForm.scene, title: conversationForm.title },
      api.http.newIdempotencyKey()
    );
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '会话创建失败';
  } finally {
    busy.value = false;
  }
}

async function startRun(): Promise<void> {
  if (!conversation.value || !input.value.trim()) {
    return;
  }
  busy.value = true;
  errorMessage.value = null;
  events.value = [];
  try {
    run.value = await api.companion.startRun(
      {
        conversationId: conversation.value.id,
        scene: conversationForm.scene,
        input: input.value.trim(),
        attachmentFileIds: []
      },
      api.http.newIdempotencyKey()
    );
    input.value = '';
    void streamRun();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '运行启动失败';
  } finally {
    busy.value = false;
  }
}

async function streamRun(): Promise<void> {
  if (!run.value) {
    return;
  }
  await api.companion.streamRunEvents(run.value.id, (event) => {
    const parsed = parseAgentEvent(event);
    events.value = [
      ...events.value,
      { type: event.event ?? parsed?.eventType ?? 'EVENT', payload: parsed?.safePayloadJson ?? event.data }
    ];
  });
}

async function decide(decision: 'ACCEPTED' | 'REJECTED'): Promise<void> {
  if (!run.value?.proposal) {
    return;
  }
  busy.value = true;
  try {
    run.value = await api.companion.confirmProposal(run.value.id, {
      proposalId: run.value.proposal.id,
      decision,
      digest: run.value.proposal.digest,
      expectedVersion: run.value.proposal.version
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '提案处理失败';
  } finally {
    busy.value = false;
  }
}

async function removeMemory(memory: MemoryResult): Promise<void> {
  await api.companion.deleteMemory(memory.id, memory.version);
  memories.value = await api.companion.listMemories();
}
</script>

<template>
  <section class="lx-card">
    <h2>灵犀对话</h2>
    <p class="lx-muted">
      服务端当前只提供受控 Agent 内核：真实模型与领域工具适配由后续接入，未配置模型时运行会失败关闭。
    </p>
    <div class="lx-row">
      <input v-model="conversationForm.title" class="lx-input" placeholder="会话标题" />
      <select v-model="conversationForm.scene" class="lx-select">
        <option value="PLANNING">目标规划</option>
        <option value="REVIEW">复盘陪伴</option>
        <option value="GENERAL">日常对话</option>
      </select>
      <button class="lx-button" :disabled="busy" @click="createConversation">创建会话</button>
    </div>
    <p v-if="conversation" class="lx-muted">当前会话 #{{ conversation.id }} · {{ conversation.title }}</p>

    <div class="lx-field companion__input">
      <label>输入内容</label>
      <textarea v-model="input" class="lx-textarea" rows="3" placeholder="描述你的目标或困惑" />
    </div>
    <button class="lx-button" :disabled="busy || !conversation" @click="startRun">发送并启动运行</button>
    <p v-if="errorMessage" class="lx-error">{{ errorMessage }}</p>
  </section>

  <section v-if="run" class="lx-card companion__run">
    <header class="lx-row run__head">
      <strong>运行 #{{ run.id }}</strong>
      <span class="lx-tag">{{ run.status }}</span>
    </header>
    <p v-if="run.resultText">{{ run.resultText }}</p>
    <p v-if="run.errorCode" class="lx-error">错误码：{{ run.errorCode }}</p>

    <div v-if="run.proposal" class="proposal">
      <strong>待确认提案：{{ run.proposal.toolName }}（{{ run.proposal.riskLevel }}）</strong>
      <pre>{{ run.proposal.argumentsJson }}</pre>
      <div class="lx-row">
        <button class="lx-button" :disabled="busy" @click="decide('ACCEPTED')">确认执行</button>
        <button class="lx-button lx-button--ghost" :disabled="busy" @click="decide('REJECTED')">
          拒绝
        </button>
      </div>
    </div>

    <h3>执行事件</h3>
    <div v-if="events.length === 0" class="lx-empty">等待事件流…</div>
    <ul v-else class="events">
      <li v-for="(event, index) in events" :key="index">
        <span class="lx-tag">{{ event.type }}</span> <code>{{ event.payload }}</code>
      </li>
    </ul>
  </section>

  <section class="lx-card companion__memory">
    <h3>长期记忆</h3>
    <div v-if="memories.length === 0" class="lx-empty">暂无记忆条目。</div>
    <ul v-else class="events">
      <li v-for="memory in memories" :key="memory.id">
        <span class="lx-tag">{{ memory.status }}</span> {{ memory.contentText }}
        <button class="lx-button lx-button--ghost" @click="removeMemory(memory)">删除</button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.companion__input,
.companion__run,
.companion__memory {
  margin-top: var(--lx-space-4);
}

.run__head {
  justify-content: space-between;
}

.proposal {
  background: var(--lx-color-surface-muted);
  border-radius: var(--lx-radius-sm);
  padding: var(--lx-space-3);
}

.proposal pre {
  margin: var(--lx-space-2) 0;
  overflow-x: auto;
}

.events {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-2);
  list-style: none;
  margin: 0;
  padding: 0;
}

.events code {
  word-break: break-all;
}
</style>
