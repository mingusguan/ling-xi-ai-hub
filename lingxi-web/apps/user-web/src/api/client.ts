import { ApiClient, createLingxiApi, type LingxiApi } from '@lingxi/api-client';

/** 令牌与设备标识的本地存储键；与刷新流程一一对应。 */
export const STORAGE_KEYS = {
  accessToken: 'lingxi.session.accessToken',
  refreshToken: 'lingxi.session.refreshToken',
  sessionFamilyId: 'lingxi.session.familyId',
  deviceId: 'lingxi.device.id',
  profile: 'lingxi.session.profile'
} as const;

type UnauthorizedHandler = () => void;

let unauthorizedHandler: UnauthorizedHandler | null = null;

/** 注册鉴权失效回调；会话仓库用它触发刷新或跳转登录。 */
export function onUnauthorized(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler;
}

/** 读取本地访问令牌；服务端只识别 Authorization: Bearer <opaqueToken>。 */
export function readAccessToken(): string | null {
  return window.localStorage.getItem(STORAGE_KEYS.accessToken);
}

/** 读取或生成稳定的设备标识；刷新令牌时必须与登录时一致。 */
export function resolveDeviceId(): string {
  const existing = window.localStorage.getItem(STORAGE_KEYS.deviceId);
  if (existing) {
    return existing;
  }
  const created =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `web-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  window.localStorage.setItem(STORAGE_KEYS.deviceId, created);
  return created;
}

export const api: LingxiApi = createLingxiApi(
  new ApiClient({
    // 开发环境由 Vite 代理 /api，生产环境由入口层按同一前缀转发。
    baseUrl: '',
    accessToken: readAccessToken,
    onUnauthorized: () => unauthorizedHandler?.()
  })
);
