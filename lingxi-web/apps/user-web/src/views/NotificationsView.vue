<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { useEngagementStore } from '@/stores/engagement';

const engagement = useEngagementStore();

onMounted(async () => {
  await engagement.loadNotifications();
  await engagement.pullChanges();
});

onUnmounted(() => {
  engagement.stopStream();
});

function formatDate(value: string): string {
  return new Date(value).toLocaleString('zh-CN');
}
</script>

<template>
  <section class="lx-card">
    <header class="lx-row notifications__head">
      <div>
        <h2>站内消息</h2>
        <p class="lx-muted">未读 {{ engagement.unreadCount }} 条；游标 {{ engagement.cursor }}</p>
      </div>
      <div class="lx-row">
        <button class="lx-button" :disabled="engagement.streaming" @click="engagement.startStream()">
          {{ engagement.streaming ? '实时通道已连接' : '连接实时通道' }}
        </button>
        <button class="lx-button lx-button--ghost" @click="engagement.stopStream()">断开</button>
      </div>
    </header>

    <p v-if="engagement.errorMessage" class="lx-error">{{ engagement.errorMessage }}</p>
    <div v-if="engagement.notifications.length === 0" class="lx-empty">暂无消息。</div>
    <ul v-else class="messages">
      <li v-for="item in engagement.notifications" :key="item.id" class="message">
        <div>
          <strong>{{ item.summary }}</strong>
          <div class="lx-muted">
            {{ item.type }} · {{ item.resourceType }}/{{ item.resourceId }} · {{ formatDate(item.createdAt) }}
          </div>
        </div>
        <div class="lx-row">
          <span class="lx-tag" :class="{ 'lx-tag--warn': item.readAt === null }">
            {{ item.readAt === null ? '未读' : '已读' }}
          </span>
          <button
            v-if="item.readAt === null"
            class="lx-button lx-button--ghost"
            @click="engagement.markRead(item.id)"
          >
            标记已读
          </button>
        </div>
      </li>
    </ul>
  </section>

  <section class="lx-card sync">
    <h3>跨端增量变更</h3>
    <p class="lx-muted">
      事件流断开后可用游标续传；服务端在 fullResyncRequired 为真时要求客户端全量重同步（当前实现恒为 false）。
    </p>
    <button class="lx-button lx-button--ghost" @click="engagement.pullChanges()">拉取增量</button>
    <div v-if="engagement.changes.length === 0" class="lx-empty">暂无变更记录。</div>
    <ul v-else class="messages">
      <li v-for="change in engagement.changes" :key="change.sequence" class="message">
        <div>
          <strong>{{ change.domain }} · {{ change.operation }}</strong>
          <div class="lx-muted">
            {{ change.resourceType }}/{{ change.resourceId }} · v{{ change.resourceVersion }} ·
            {{ formatDate(change.occurredAt) }}
          </div>
        </div>
        <span class="lx-tag">#{{ change.sequence }}</span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.notifications__head {
  justify-content: space-between;
}

.messages {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-3);
  list-style: none;
  margin: var(--lx-space-4) 0 0;
  padding: 0;
}

.message {
  align-items: center;
  border: 1px solid var(--lx-color-border);
  border-radius: var(--lx-radius-sm);
  display: flex;
  justify-content: space-between;
  padding: var(--lx-space-3);
}

.sync {
  margin-top: var(--lx-space-5);
}
</style>
