import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type { AccountStatus, AgeBand, SessionTokens } from '@lingxi/api-client';
import { api, onUnauthorized, readAccessToken, resolveDeviceId, STORAGE_KEYS } from '@/api/client';
import { assertionProvider, type AssertionRequest } from '@/api/assertion';

/** 本地保存的会话摘要，用于判断是否需要重新登录。 */
export interface SessionProfile {
  /** 用户标识；服务端 long 以字符串输出；登录接口不返回 userId，仅在注册后本地记录。 */
  userId: string | null;
  ageBand: AgeBand;
  accountStatus: AccountStatus;
  authorizationVersion: string;
  refreshExpiresAt: string;
}

/** 访问令牌剩余有效期低于该值时主动刷新。 */
const REFRESH_AHEAD_MS = 60_000;

function readProfile(): SessionProfile | null {
  const raw = window.localStorage.getItem(STORAGE_KEYS.profile);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as SessionProfile;
  } catch {
    return null;
  }
}

/**
 * 会话状态：注册、登录、刷新与登出。
 *
 * accessToken 有效期 15 分钟、refreshToken 30 天且刷新后轮换，因此刷新成功后必须同时替换两者，
 * 并始终携带登录时保存的 deviceId。
 */
export const useSessionStore = defineStore('session', () => {
  const profile = ref<SessionProfile | null>(readProfile());
  /** 访问令牌以响应式状态保存，保证路由守卫与界面在登录/登出后立即更新。 */
  const accessToken = ref<string | null>(readAccessToken());
  const accessExpiresAt = ref<string | null>(window.localStorage.getItem('lingxi.session.accessExpiresAt'));
  const busy = ref(false);
  const errorMessage = ref<string | null>(null);

  const isAuthenticated = computed(() => accessToken.value !== null && profile.value !== null);
  const isTeenager = computed(() => profile.value?.ageBand === 'TEEN');
  const needsGuardian = computed(() => profile.value?.accountStatus === 'PENDING_GUARDIAN');

  function store(tokens: SessionTokens, userId: string | null): void {
    window.localStorage.setItem(STORAGE_KEYS.accessToken, tokens.accessToken);
    window.localStorage.setItem(STORAGE_KEYS.refreshToken, tokens.refreshToken);
    window.localStorage.setItem(STORAGE_KEYS.sessionFamilyId, tokens.sessionFamilyId);
    window.localStorage.setItem('lingxi.session.accessExpiresAt', tokens.accessExpiresAt);
    // 会话接口不返回 userId；注册成功后记录一次供后续展示使用。
    if (userId !== null) {
      window.localStorage.setItem('lingxi.session.userId', userId);
    }
    accessToken.value = tokens.accessToken;
    accessExpiresAt.value = tokens.accessExpiresAt;
    profile.value = {
      userId: userId ?? window.localStorage.getItem('lingxi.session.userId'),
      ageBand: tokens.ageBand,
      accountStatus: tokens.accountStatus,
      authorizationVersion: tokens.authorizationVersion,
      refreshExpiresAt: tokens.refreshExpiresAt
    };
    window.localStorage.setItem(STORAGE_KEYS.profile, JSON.stringify(profile.value));
  }

  /**
   * 手机号登录。
   *
   * 手机号未绑定账号时服务端按出生日期自动建立账号（14+ 准入、14—17 未成年人为待监护状态），
   * 因此首次登录必须填写出生日期；已绑定账号时出生日期可留空。
   */
  async function loginWithPhone(phoneNumber: string, birthDate: string | null): Promise<void> {
    busy.value = true;
    errorMessage.value = null;
    try {
      const tokens = await api.identity.loginWithPhone({
        phoneNumber,
        birthDate,
        deviceId: resolveDeviceId(),
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai'
      });
      store(tokens, window.localStorage.getItem('lingxi.session.userId'));
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '登录失败';
      throw error;
    } finally {
      busy.value = false;
    }
  }

  /** 注册并立即登录；服务端注册接口不返回令牌。 */
  async function registerAndLogin(request: AssertionRequest, timezone: string): Promise<void> {
    busy.value = true;
    errorMessage.value = null;
    try {
      const signedAssertion = await assertionProvider.acquire({ ...request, requireBirthDate: true });
      const registered = await api.identity.register({
        requestKey: crypto.randomUUID(),
        signedAssertion,
        timezone
      });
      const tokens = await api.identity.login({
        requestKey: crypto.randomUUID(),
        signedAssertion,
        deviceId: resolveDeviceId()
      });
      store(tokens, registered.userId);
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '注册失败';
      throw error;
    } finally {
      busy.value = false;
    }
  }

  /** 使用身份断言登录。 */
  async function login(request: AssertionRequest): Promise<void> {
    busy.value = true;
    errorMessage.value = null;
    try {
      const signedAssertion = await assertionProvider.acquire({ ...request, requireBirthDate: false });
      const tokens = await api.identity.login({
        requestKey: crypto.randomUUID(),
        signedAssertion,
        deviceId: resolveDeviceId()
      });
      profile.value = {
        userId: window.localStorage.getItem('lingxi.session.userId'),
        ageBand: tokens.ageBand,
        accountStatus: tokens.accountStatus,
        authorizationVersion: tokens.authorizationVersion,
        refreshExpiresAt: tokens.refreshExpiresAt
      };
      window.localStorage.setItem(STORAGE_KEYS.accessToken, tokens.accessToken);
      window.localStorage.setItem(STORAGE_KEYS.refreshToken, tokens.refreshToken);
      window.localStorage.setItem(STORAGE_KEYS.sessionFamilyId, tokens.sessionFamilyId);
      window.localStorage.setItem('lingxi.session.accessExpiresAt', tokens.accessExpiresAt);
      window.localStorage.setItem(STORAGE_KEYS.profile, JSON.stringify(profile.value));
      accessToken.value = tokens.accessToken;
      accessExpiresAt.value = tokens.accessExpiresAt;
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '登录失败';
      throw error;
    } finally {
      busy.value = false;
    }
  }

  /** 刷新令牌；重放或设备不一致会导致整个会话族失效，此时必须重新登录。 */
  async function refreshTokens(): Promise<boolean> {
    const refreshToken = window.localStorage.getItem(STORAGE_KEYS.refreshToken);
    const sessionFamilyId = window.localStorage.getItem(STORAGE_KEYS.sessionFamilyId);
    if (!refreshToken || !sessionFamilyId) {
      return false;
    }
    try {
      const tokens = await api.identity.refresh({
        sessionFamilyId,
        refreshToken,
        deviceId: resolveDeviceId()
      });
      window.localStorage.setItem(STORAGE_KEYS.accessToken, tokens.accessToken);
      window.localStorage.setItem(STORAGE_KEYS.refreshToken, tokens.refreshToken);
      window.localStorage.setItem('lingxi.session.accessExpiresAt', tokens.accessExpiresAt);
      accessToken.value = tokens.accessToken;
      accessExpiresAt.value = tokens.accessExpiresAt;
      if (profile.value) {
        profile.value = {
          ...profile.value,
          ageBand: tokens.ageBand,
          accountStatus: tokens.accountStatus,
          authorizationVersion: tokens.authorizationVersion,
          refreshExpiresAt: tokens.refreshExpiresAt
        };
        window.localStorage.setItem(STORAGE_KEYS.profile, JSON.stringify(profile.value));
      }
      return true;
    } catch {
      logout();
      return false;
    }
  }

  /** 请求前保证令牌可用：临近过期时先刷新。 */
  async function ensureFreshToken(): Promise<void> {
    if (!accessExpiresAt.value) {
      return;
    }
    const expiresAt = new Date(accessExpiresAt.value).getTime();
    if (Number.isFinite(expiresAt) && expiresAt - Date.now() < REFRESH_AHEAD_MS) {
      await refreshTokens();
    }
  }

  function logout(): void {
    window.localStorage.removeItem(STORAGE_KEYS.accessToken);
    window.localStorage.removeItem(STORAGE_KEYS.refreshToken);
    window.localStorage.removeItem(STORAGE_KEYS.sessionFamilyId);
    window.localStorage.removeItem('lingxi.session.accessExpiresAt');
    window.localStorage.removeItem(STORAGE_KEYS.profile);
    profile.value = null;
    accessToken.value = null;
    accessExpiresAt.value = null;
  }

  /** 鉴权失效时先尝试刷新，失败则清理会话回到登录页。 */
  async function handleUnauthorized(): Promise<void> {
    const refreshed = await refreshTokens();
    if (!refreshed) {
      logout();
    }
  }

  onUnauthorized(() => {
    void handleUnauthorized();
  });

  return {
    profile,
    busy,
    errorMessage,
    isAuthenticated,
    isTeenager,
    needsGuardian,
    loginWithPhone,
    registerAndLogin,
    login,
    refreshTokens,
    ensureFreshToken,
    logout
  };
});
