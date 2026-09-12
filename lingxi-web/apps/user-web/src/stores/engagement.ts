import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type { NotificationResult, StreamEvent, SyncChangeResult } from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

/** 跨端同步游标与站内消息状态。 */
export const useEngagementStore = defineStore('engagement', () => {
  const CURSOR_KEY = 'lingxi.sync.cursor';
  const notifications = ref<NotificationResult[]>([]);
  const changes = ref<SyncChangeResult[]>([]);
  const cursor = ref<string>(window.localStorage.getItem(CURSOR_KEY) ?? '0');
  const streaming = ref(false);
  const errorMessage = ref<string | null>(null);
  let controller: AbortController | null = null;

  const unreadCount = computed(() => notifications.value.filter((item) => item.readAt === null).length);

  async function loadNotifications(): Promise<void> {
    try {
      notifications.value = await api.engagement.listNotifications(0, 50);
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '消息加载失败';
    }
  }

  async function markRead(id: string): Promise<void> {
    await api.engagement.markNotificationRead(id);
    notifications.value = notifications.value.map((item) =>
      item.id === id ? { ...item, readAt: new Date().toISOString() } : item
    );
  }

  /** 拉取增量变更并把游标前移。 */
  async function pullChanges(): Promise<void> {
    const page = await api.engagement.getChanges(cursor.value, [], 100);
    if (page.changes.length > 0) {
      changes.value = [...page.changes, ...changes.value].slice(0, 200);
    }
    if (page.nextCursor !== cursor.value) {
      cursor.value = page.nextCursor;
      window.localStorage.setItem(CURSOR_KEY, String(page.nextCursor));
    }
  }

  /** 订阅实时变更流；断线时以 lastEventId 续传。 */
  async function startStream(): Promise<void> {
    if (streaming.value) {
      return;
    }
    const session = useSessionStore();
    await session.ensureFreshToken();
    streaming.value = true;
    controller = new AbortController();
    try {
      await api.engagement.streamEvents(
        (event: StreamEvent) => {
          if (event.id) {
            // SSE 的 id 就是同步游标，服务端为 64 位序列号，必须按字符串原样回传。
            cursor.value = event.id;
            window.localStorage.setItem(CURSOR_KEY, event.id);
          }
          try {
            changes.value = [JSON.parse(event.data) as SyncChangeResult, ...changes.value].slice(0, 200);
          } catch {
            // 心跳等注释行没有 data，忽略即可。
          }
        },
        { lastEventId: String(cursor.value), signal: controller.signal }
      );
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '实时通道已断开';
    } finally {
      streaming.value = false;
      controller = null;
    }
  }

  function stopStream(): void {
    controller?.abort();
    streaming.value = false;
    controller = null;
  }

  /** 提交离线命令，服务端按 clientCommandId 幂等重放。 */
  async function submitOfflineCommand(
    commandType: 'GOAL_CREATE' | 'CHECK_IN' | 'MARK_NOTIFICATION_READ',
    payload: Record<string, unknown>,
    baseVersion: number
  ) {
    return api.engagement.applyOfflineCommand({
      clientCommandId: crypto.randomUUID(),
      commandType,
      payloadJson: JSON.stringify(payload),
      baseVersion
    });
  }

  return {
    notifications,
    unreadCount,
    changes,
    cursor,
    streaming,
    errorMessage,
    loadNotifications,
    markRead,
    pullChanges,
    startStream,
    stopStream,
    submitOfflineCommand
  };
});
