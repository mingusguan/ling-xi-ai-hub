/**
 * 灵犀伴行用户端 HTTP 传输层。
 *
 * 与后端 com.lingxi.kernel.ApiResponse 保持一致：成功响应固定为
 * `{ code: 'OK', message, data, requestId }`；失败响应携带业务错误码。
 */

/** 后端统一响应包裹。 */
export interface ApiEnvelope<T> {
  code: string;
  message: string;
  data: T;
  requestId: string;
}

/**
 * 服务端 64 位标识与游标的 JSON 类型。
 *
 * 服务端把 long/Long 序列化为字符串（见 JacksonLongAsStringConfiguration），
 * 因为其取值超出 JavaScript 安全整数范围；客户端必须按字符串原样回传，禁止做 Number 转换。
 */
export type LongId = string;

/** 分页结果，对应后端 com.lingxi.kernel.PageResult，页码从 1 开始。 */
export interface PageResult<T> {
  items: T[];
  /** 总数，服务端为 long，序列化为字符串。 */
  total: LongId;
  page: number;
  pageSize: number;
}

/** 业务或传输错误。 */
export class ApiError extends Error {
  readonly code: string;
  readonly status: number;
  readonly requestId: string | null;

  constructor(code: string, message: string, status: number, requestId: string | null = null) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
    this.requestId = requestId;
  }

  /** 是否为需要重新登录的鉴权错误。 */
  get isUnauthorized(): boolean {
    return this.status === 401 || this.status === 403;
  }
}

/** 服务端推送事件（SSE）。 */
export interface StreamEvent {
  id: string | null;
  event: string | null;
  data: string;
}

export interface ApiClientOptions {
  /** 反向代理后的接口前缀，例如 `/api`。 */
  baseUrl: string;
  /** 访问令牌提供者；返回 null 表示匿名请求。 */
  accessToken?: () => string | null;
  /** 收到鉴权失败时的回调，用于触发刷新或跳转登录。 */
  onUnauthorized?: () => void;
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  query?: Record<string, unknown>;
  body?: unknown;
  /** 幂等键；写接口需要显式传入。 */
  idempotencyKey?: string;
  headers?: Record<string, string>;
  signal?: AbortSignal;
  /** 跳过 Authorization 头，用于登录与注册。 */
  anonymous?: boolean;
}

export interface StreamOptions {
  query?: Record<string, unknown>;
  headers?: Record<string, string>;
  signal?: AbortSignal;
  /** 断线续传游标，对应 Last-Event-ID。 */
  lastEventId?: string;
}

/** 依据后端接口契约发送请求。 */
export class ApiClient {
  private readonly options: ApiClientOptions;

  constructor(options: ApiClientOptions) {
    this.options = options;
  }

  /** 发送请求并解包 data。 */
  async send<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const envelope = await this.sendEnvelope<T>(path, options);
    return envelope.data;
  }

  /** 发送请求并保留响应包裹，用于读取 requestId。 */
  async sendEnvelope<T>(path: string, options: RequestOptions = {}): Promise<ApiEnvelope<T>> {
    const response = await fetch(this.buildUrl(path, options.query), {
      method: options.method ?? 'GET',
      headers: this.buildHeaders(options),
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal
    });
    const requestId = response.headers.get('X-Request-Id');
    const payload = await this.readJson(response);
    if (!response.ok) {
      this.handleUnauthorized(response.status);
      throw this.toError(payload, response.status, requestId);
    }
    const envelope = payload as ApiEnvelope<T>;
    // 成功判定以业务响应码为准，不依赖 HTTP 状态码。
    if (envelope.code !== 'OK') {
      throw this.toError(payload, response.status, requestId);
    }
    return envelope;
  }

  /** 建立 SSE 连接，按事件流逐条回调；连接结束或中断后返回。 */
  async stream(
    path: string,
    onEvent: (event: StreamEvent) => void,
    options: StreamOptions = {}
  ): Promise<void> {
    const headers = this.buildHeaders({
      headers: { Accept: 'text/event-stream', ...options.headers },
      signal: options.signal
    });
    if (options.lastEventId) {
      headers['Last-Event-ID'] = options.lastEventId;
    }
    const response = await fetch(this.buildUrl(path, options.query), {
      method: 'GET',
      headers,
      signal: options.signal
    });
    if (!response.ok) {
      this.handleUnauthorized(response.status);
      throw new ApiError('STREAM_FAILED', '实时通道建立失败', response.status);
    }
    if (!response.body) {
      throw new ApiError('STREAM_UNSUPPORTED', '当前浏览器不支持实时通道', response.status);
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    for (;;) {
      const { value, done } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      buffer = this.drainEvents(buffer, onEvent);
    }
  }

  /** 生成幂等键，供写接口复用同一摘要。 */
  newIdempotencyKey(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `idem-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }

  private buildUrl(path: string, query?: Record<string, unknown>): string {
    const base = this.options.baseUrl.replace(/\/$/, '');
    const url = new URL(`${base}${path}`, window.location.origin);
    if (query) {
      for (const [key, value] of Object.entries(query)) {
        if (value === undefined || value === null || value === '') {
          continue;
        }
        if (Array.isArray(value)) {
          for (const item of value) {
            url.searchParams.append(key, String(item));
          }
          continue;
        }
        url.searchParams.append(key, String(value));
      }
    }
    return url.toString();
  }

  private buildHeaders(options: RequestOptions): Record<string, string> {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (options.body !== undefined) {
      headers['Content-Type'] = 'application/json;charset=UTF-8';
    }
    if (options.idempotencyKey) {
      headers['Idempotency-Key'] = options.idempotencyKey;
    }
    if (!options.anonymous) {
      // 服务端只识别 Authorization: Bearer <opaqueToken>；设备标识通过登录请求体传递。
      const token = this.options.accessToken?.() ?? null;
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
    }
    return { ...headers, ...options.headers };
  }

  private async readJson(response: Response): Promise<unknown> {
    const text = await response.text();
    if (!text) {
      return { code: 'EMPTY', message: response.statusText, data: null, requestId: '' };
    }
    try {
      return JSON.parse(text);
    } catch {
      return { code: 'INVALID_JSON', message: text.slice(0, 200), data: null, requestId: '' };
    }
  }

  private toError(payload: unknown, status: number, headerRequestId: string | null): ApiError {
    const envelope = payload as Partial<ApiEnvelope<unknown>> | null;
    const code = envelope?.code && envelope.code !== '' ? envelope.code : `HTTP_${status}`;
    const message = envelope?.message ?? '请求失败';
    const requestId = envelope?.requestId ?? headerRequestId;
    return new ApiError(code, message, status, requestId ?? null);
  }

  private handleUnauthorized(status: number): void {
    if (status === 401 || status === 403) {
      this.options.onUnauthorized?.();
    }
  }

  private drainEvents(buffer: string, onEvent: (event: StreamEvent) => void): string {
    let rest = buffer;
    let boundary = rest.indexOf('\n\n');
    while (boundary >= 0) {
      const raw = rest.slice(0, boundary);
      rest = rest.slice(boundary + 2);
      const event = this.parseEvent(raw);
      if (event) {
        onEvent(event);
      }
      boundary = rest.indexOf('\n\n');
    }
    return rest;
  }

  private parseEvent(raw: string): StreamEvent | null {
    const event: StreamEvent = { id: null, event: null, data: '' };
    for (const line of raw.split('\n')) {
      if (line.startsWith(':')) {
        continue;
      }
      const separator = line.indexOf(':');
      const field = separator < 0 ? line : line.slice(0, separator);
      const value = separator < 0 ? '' : line.slice(separator + 1).trimStart();
      if (field === 'id') {
        event.id = value;
      } else if (field === 'event') {
        event.event = value;
      } else if (field === 'data') {
        event.data = event.data === '' ? value : `${event.data}\n${value}`;
      }
    }
    return event.data === '' && event.event === null && event.id === null ? null : event;
  }
}
