<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import type { NotificationChannel } from '@lingxi/api-client';
import { useEngagementStore } from '@/stores/engagement';

const engagement = useEngagementStore();
const savingPreference = ref(false);
const preferenceNotice = ref<string | null>(null);

/** 行动提醒场景标识，与 engagement `OccurrenceReminderHandler.SCENE` 一致。 */
const preferenceForm = reactive({
  scene: 'ACTION_REMINDER',
  channels: ['INBOX'] as NotificationChannel[],
  quietStart: '' as string,
  quietEnd: '' as string,
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai'
});

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

function toggleChannel(channel: NotificationChannel): void {
  preferenceForm.channels = preferenceForm.channels.includes(channel)
    ? preferenceForm.channels.filter((item) => item !== channel)
    : [...preferenceForm.channels, channel];
}

/** 保存通知偏好；服务端按 expectedVersion 做乐观并发校验，成功后回写版本。 */
async function savePreference(): Promise<void> {
  preferenceNotice.value = null;
  if (preferenceForm.channels.length === 0) {
    preferenceNotice.value = '至少保留一个通知渠道';
    return;
  }
  if (preferenceForm.quietStart && preferenceForm.quietEnd && preferenceForm.quietStart === preferenceForm.quietEnd) {
    preferenceNotice.value = '免打扰开始与结束时间不能相同';
    return;
  }
  savingPreference.value = true;
  try {
    const saved = await engagement.updatePreference({
      scene: preferenceForm.scene.trim(),
      channels: preferenceForm.channels,
      quietStart: preferenceForm.quietStart ? `${preferenceForm.quietStart}:00` : null,
      quietEnd: preferenceForm.quietEnd ? `${preferenceForm.quietEnd}:00` : null,
      timezone: preferenceForm.timezone
    });
    preferenceNotice.value = saved
      ? `已保存（版本 ${saved.version}）：${saved.channels.join(' / ')}`
      : engagement.errorMessage;
  } finally {
    savingPreference.value = false;
  }
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

  <section class="lx-card preference">
    <h3>通知偏好</h3>
    <p class="lx-muted">
      按场景分别保存渠道与免打扰时段；青少年账号的免打扰由服务端强制为 22:00—07:00，服务端会以实际生效值为准。
      保存采用乐观并发，成功后自动使用服务端返回的新版本。
    </p>
    <div class="preference__grid">
      <label class="lx-muted">
        场景
        <input v-model="preferenceForm.scene" class="lx-input" placeholder="ACTION_REMINDER" />
      </label>
      <label class="lx-muted">
        时区
        <input v-model="preferenceForm.timezone" class="lx-input" placeholder="Asia/Shanghai" />
      </label>
      <label class="lx-muted">
        免打扰开始
        <input v-model="preferenceForm.quietStart" class="lx-input" type="time" />
      </label>
      <label class="lx-muted">
        免打扰结束
        <input v-model="preferenceForm.quietEnd" class="lx-input" type="time" />
      </label>
    </div>
    <div class="lx-row preference__channels">
      <button
        v-for="channel in (['INBOX', 'PUSH'] as NotificationChannel[])"
        :key="channel"
        class="lx-button"
        :class="{ 'lx-button--ghost': !preferenceForm.channels.includes(channel) }"
        @click="toggleChannel(channel)"
      >
        {{ channel === 'INBOX' ? '站内信' : '推送' }}
      </button>
      <button class="lx-button" :disabled="savingPreference" @click="savePreference">
        {{ savingPreference ? '保存中…' : '保存偏好' }}
      </button>
    </div>
    <p v-if="preferenceNotice" class="lx-muted">{{ preferenceNotice }}</p>
    <p v-if="engagement.preference" class="lx-muted">
      当前已保存：{{ engagement.preference.scene }} · {{ engagement.preference.channels.join(' / ') }} ·
      免打扰 {{ engagement.preference.quietStart ?? '未设置' }} — {{ engagement.preference.quietEnd ?? '未设置' }} ·
      版本 {{ engagement.preference.version }}
    </p>
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

.preference {
  margin-top: var(--lx-space-5);
}

.preference__grid {
  display: grid;
  gap: var(--lx-space-3);
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  margin: var(--lx-space-4) 0;
}

.preference__channels {
  flex-wrap: wrap;
}

.sync {
  margin-top: var(--lx-space-5);
}
</style>
