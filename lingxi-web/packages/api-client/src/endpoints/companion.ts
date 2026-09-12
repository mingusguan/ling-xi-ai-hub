import type { ApiClient, LongId, StreamEvent } from '../http';
import type {
  AgentEventResult,
  AgentRunResult,
  ConversationResult,
  MemoryResult
} from '../types';

/** 创建会话请求体，对应 CompanionController.ConversationBody。 */
export interface ConversationBody {
  scene: string;
  title: string;
}

/** 启动 Agent 运行请求体，对应 CompanionController.RunBody。 */
export interface RunBody {
  conversationId: LongId;
  scene: string;
  /** 输入内容，服务端限制 20000 字符。 */
  input: string;
  /** 附件文件标识，最多 10 个且必须已完成授权。 */
  attachmentFileIds: LongId[];
}

/** 提案确认请求体，对应 CompanionController.ConfirmationBody。 */
export interface ConfirmationBody {
  proposalId: LongId;
  /** 只允许 ACCEPTED 或 REJECTED。 */
  decision: 'ACCEPTED' | 'REJECTED';
  /** 必须来自提案响应的 digest。 */
  digest: string;
  expectedVersion: LongId;
}

/** 会话、Agent 运行与长期记忆接口客户端。 */
export class CompanionApi {
  constructor(private readonly client: ApiClient) {}

  /** 创建对话会话。 */
  createConversation(body: ConversationBody, idempotencyKey: string): Promise<ConversationResult> {
    return this.client.send<ConversationResult>('/api/v1/conversations', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 启动一次 Agent 运行。 */
  startRun(body: RunBody, idempotencyKey: string): Promise<AgentRunResult> {
    return this.client.send<AgentRunResult>('/api/v1/agent-runs', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 查询运行状态与最终结果。 */
  getRun(runId: LongId): Promise<AgentRunResult> {
    return this.client.send<AgentRunResult>(`/api/v1/agent-runs/${runId}`);
  }

  /** 确认或拒绝高风险提案；T3 提案需要近期认证。 */
  confirmProposal(runId: LongId, body: ConfirmationBody): Promise<AgentRunResult> {
    return this.client.send<AgentRunResult>(`/api/v1/agent-runs/${runId}/confirmations`, {
      method: 'POST',
      body
    });
  }

  /** 订阅运行事件流，按 Last-Event-ID 断线续传。 */
  streamRunEvents(
    runId: LongId,
    onEvent: (event: StreamEvent) => void,
    options: { lastEventId?: LongId; signal?: AbortSignal } = {}
  ): Promise<void> {
    return this.client.stream(`/api/v1/agent-runs/${runId}/events`, onEvent, {
      lastEventId: options.lastEventId,
      signal: options.signal
    });
  }

  /** 列出长期记忆。 */
  listMemories(): Promise<MemoryResult[]> {
    return this.client.send<MemoryResult[]>('/api/v1/memories');
  }

  /** 更新记忆内容。 */
  updateMemory(memoryId: LongId, contentText: string, expectedVersion: LongId): Promise<MemoryResult> {
    return this.client.send<MemoryResult>(`/api/v1/memories/${memoryId}`, {
      method: 'PUT',
      body: { contentText, expectedVersion }
    });
  }

  /** 删除记忆；服务端异步传播删除。 */
  deleteMemory(memoryId: LongId, expectedVersion: LongId): Promise<MemoryResult> {
    return this.client.send<MemoryResult>(`/api/v1/memories/${memoryId}`, {
      method: 'DELETE',
      query: { expectedVersion }
    });
  }
}

/** 解析 Agent 事件载荷，解析失败返回原文本。 */
export function parseAgentEvent(event: StreamEvent): AgentEventResult | null {
  try {
    return JSON.parse(event.data) as AgentEventResult;
  } catch {
    return null;
  }
}
