import http from '@ohos.net.http';
import { ApiEnvelope } from '../model/Types';

/** 网关基地址：通过 EntryAbility 初始化时注入，生产环境由入口层按同一前缀转发。 */
export class ApiConfig {
  static baseUrl: string = 'http://127.0.0.1:8080';

  static setBaseUrl(value: string): void {
    ApiConfig.baseUrl = value;
  }
}

/** 业务或传输错误。 */
export class ApiError extends Error {
  code: string;
  status: number;

  constructor(code: string, message: string, status: number) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export interface RequestOptions {
  method?: http.RequestMethod;
  query?: Record<string, string>;
  body?: object;
  /** 幂等键；写接口按契约必须携带。 */
  idempotencyKey?: string;
}

/**
 * HarmonyOS HTTP 客户端。
 *
 * 与 PC Web 共用同一契约：成功判定以业务响应码 `code === 'OK'` 为准，不使用 HTTP 状态码推断成功。
 */
export class HttpClient {
  private accessToken: string | null = null;

  setAccessToken(token: string | null): void {
    this.accessToken = token;
  }

  /** 生成幂等键，写接口重试时必须复用同一个值。 */
  newIdempotencyKey(): string {
    return `harmony-${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
  }

  async send<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const envelope = await this.sendEnvelope<T>(path, options);
    return envelope.data;
  }

  async sendEnvelope<T>(path: string, options: RequestOptions = {}): Promise<ApiEnvelope<T>> {
    const request = http.createHttp();
    try {
      const header: Record<string, string> = {
        'Content-Type': 'application/json;charset=UTF-8',
        Accept: 'application/json'
      };
      if (this.accessToken !== null) {
        header['Authorization'] = `Bearer ${this.accessToken}`;
      }
      if (options.idempotencyKey !== undefined) {
        header['Idempotency-Key'] = options.idempotencyKey;
      }
      const response = await request.request(this.buildUrl(path, options.query), {
        method: options.method ?? http.RequestMethod.GET,
        header: header,
        extraData: options.body === undefined ? undefined : JSON.stringify(options.body),
        connectTimeout: 15000,
        readTimeout: 20000,
        expectDataType: http.HttpDataType.STRING
      });
      const payload = JSON.parse(response.result as string) as ApiEnvelope<T>;
      if (payload.code !== 'OK') {
        throw new ApiError(payload.code, payload.message, response.responseCode);
      }
      return payload;
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }
      throw new ApiError('NETWORK_ERROR', `网络请求失败：${JSON.stringify(error)}`, 0);
    } finally {
      request.destroy();
    }
  }

  private buildUrl(path: string, query?: Record<string, string>): string {
    let url = `${ApiConfig.baseUrl}${path}`;
    if (query !== undefined) {
      const parts: string[] = [];
      for (const key of Object.keys(query)) {
        const value = query[key];
        if (value === undefined || value === null || value === '') {
          continue;
        }
        parts.push(`${key}=${encodeURIComponent(value)}`);
      }
      if (parts.length > 0) {
        url = `${url}?${parts.join('&')}`;
      }
    }
    return url;
  }
}
