import type { ApiClient, LongId, StreamEvent } from '../http';
import type {
  CalendarBindingResult,
  NotificationChannel,
  NotificationPreferenceResult,
  NotificationResult,
  OfflineCommandResult,
  SyncPageResult
} from '../types';

/** 通知偏好请求体，对应 EngagementController.PreferenceBody。 */
export interface PreferenceBody {
  scene: string;
  channels: NotificationChannel[];
  quietStart: string | null;
  quietEnd: string | null;
  timezone: string;
  expectedVersion: LongId;
}

/** 日历绑定请求体，对应 EngagementController.CalendarBody。 */
export interface CalendarBody {
  provider: string;
  credentialReference: string;
}

/** 离线命令请求体，对应 EngagementController.CommandBody。 */
export interface CommandBody {
  clientCommandId: string;
  commandType: string;
  payloadJson: string;
  baseVersion: number;
}

/** 同步、通知与日历接口客户端。 */
export class EngagementApi {
  constructor(private readonly client: ApiClient) {}

  /** 按游标读取站内消息。 */
  listNotifications(cursor = 0, limit = 50): Promise<NotificationResult[]> {
    return this.client.send<NotificationResult[]>('/api/v1/notifications', {
      query: { cursor, limit }
    });
  }

  /** 标记站内消息已读。 */
  markNotificationRead(id: LongId): Promise<void> {
    return this.client.send<void>(`/api/v1/notifications/${id}/read`, { method: 'PUT' });
  }

  /** 更新通知偏好。 */
  updatePreference(body: PreferenceBody): Promise<NotificationPreferenceResult> {
    return this.client.send<NotificationPreferenceResult>('/api/v1/notification-preferences', {
      method: 'PUT',
      body
    });
  }

  /** 增量拉取跨端变更；fullResyncRequired 为真时客户端需要全量刷新。 */
  getChanges(cursor: LongId, domains: string[] = [], limit = 100): Promise<SyncPageResult> {
    return this.client.send<SyncPageResult>('/api/v1/sync/changes', {
      query: { cursor, domains, limit }
    });
  }

  /** 提交离线命令，服务端按键幂等重放。 */
  applyOfflineCommand(body: CommandBody): Promise<OfflineCommandResult> {
    return this.client.send<OfflineCommandResult>('/api/v1/sync/commands', {
      method: 'POST',
      body
    });
  }

  /** 查询本人全部日历绑定，供页面展示当前绑定与撤销入口。 */
  listCalendarBindings(): Promise<CalendarBindingResult[]> {
    return this.client.send<CalendarBindingResult[]>('/api/v1/calendar/bindings');
  }

  /** 绑定日历渠道，需要近期认证。 */
  bindCalendar(body: CalendarBody, idempotencyKey: string): Promise<CalendarBindingResult> {
    return this.client.send<CalendarBindingResult>('/api/v1/calendar/bindings', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 撤销日历绑定。 */
  revokeCalendar(bindingId: LongId, deleteCreatedEvents: boolean): Promise<CalendarBindingResult> {
    return this.client.send<CalendarBindingResult>(`/api/v1/calendar/bindings/${bindingId}`, {
      method: 'DELETE',
      query: { deleteCreatedEvents }
    });
  }

  /** 订阅实时同步事件流；断线重连时传 lastEventId 续传。 */
  streamEvents(
    onEvent: (event: StreamEvent) => void,
    options: { lastEventId?: LongId; signal?: AbortSignal } = {}
  ): Promise<void> {
    return this.client.stream('/api/v1/sync/events', onEvent, {
      lastEventId: options.lastEventId,
      signal: options.signal
    });
  }
}
