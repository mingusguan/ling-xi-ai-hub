import preferences from '@ohos.data.preferences';
import common from '@ohos.app.ability.common';
import { AccountStatus, AgeBand, SessionTokens } from '../model/Types';
import { HttpClient } from '../api/HttpClient';
import { LingxiApi } from '../api/LingxiApi';

const STORE_NAME = 'lingxi_session';
const KEY_ACCESS_TOKEN = 'accessToken';
const KEY_REFRESH_TOKEN = 'refreshToken';
const KEY_FAMILY_ID = 'sessionFamilyId';
const KEY_DEVICE_ID = 'deviceId';
const KEY_AGE_BAND = 'ageBand';
const KEY_ACCOUNT_STATUS = 'accountStatus';
const KEY_USER_ID = 'userId';

/**
 * HarmonyOS 会话状态。
 *
 * 与 PC Web 使用同一套服务端会话语义：access 15 分钟、refresh 30 天且刷新后轮换，
 * deviceId 必须在登录时生成并持久化，刷新时原样回传。
 */
export class SessionStore {
  private static store: preferences.Preferences | null = null;
  static readonly http: HttpClient = new HttpClient();
  static readonly api: LingxiApi = new LingxiApi(SessionStore.http);
  static accessToken: string = '';
  static ageBand: AgeBand = 'ADULT';
  static accountStatus: AccountStatus = 'ACTIVE_ADULT';

  /** Ability 启动时调用，恢复本地会话。 */
  static async init(context: common.UIAbilityContext): Promise<void> {
    SessionStore.store = await preferences.getPreferences(context, STORE_NAME);
    SessionStore.accessToken = (await SessionStore.store.get(KEY_ACCESS_TOKEN, '')) as string;
    SessionStore.ageBand = (await SessionStore.store.get(KEY_AGE_BAND, 'ADULT')) as AgeBand;
    SessionStore.accountStatus = (await SessionStore.store.get(
      KEY_ACCOUNT_STATUS,
      'ACTIVE_ADULT'
    )) as AccountStatus;
    SessionStore.http.setAccessToken(SessionStore.accessToken === '' ? null : SessionStore.accessToken);
  }

  static isAuthenticated(): boolean {
    return SessionStore.accessToken !== '';
  }

  /** 读取或生成设备标识；刷新令牌时必须与登录时一致。 */
  static async resolveDeviceId(): Promise<string> {
    const existing = (await SessionStore.store?.get(KEY_DEVICE_ID, '')) as string;
    if (existing !== undefined && existing !== '') {
      return existing;
    }
    const created = `harmony-${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
    await SessionStore.store?.put(KEY_DEVICE_ID, created);
    await SessionStore.store?.flush();
    return created;
  }

  static async registerAndLogin(
    signedAssertion: string,
    timezone: string
  ): Promise<void> {
    const registered = await SessionStore.api.register(
      `reg-${Date.now()}`,
      signedAssertion,
      timezone
    );
    await SessionStore.store?.put(KEY_USER_ID, `${registered.userId}`);
    const tokens = await SessionStore.api.login(signedAssertion, await SessionStore.resolveDeviceId());
    await SessionStore.apply(tokens);
  }

  static async login(signedAssertion: string): Promise<void> {
    const tokens = await SessionStore.api.login(signedAssertion, await SessionStore.resolveDeviceId());
    await SessionStore.apply(tokens);
  }

  /**
   * 手机号登录。
   *
   * 手机号未绑定账号时服务端按出生日期自动建立账号（14+ 准入、14—17 未成年人为待监护状态），
   * 因此首次登录必须传入出生日期；已绑定账号时出生日期可传空字符串。
   */
  static async loginWithPhone(phoneNumber: string, birthDate: string): Promise<void> {
    const tokens = await SessionStore.api.loginWithPhone(
      phoneNumber,
      birthDate,
      await SessionStore.resolveDeviceId()
    );
    await SessionStore.apply(tokens);
  }

  /** 刷新令牌；刷新失败必须清理本地会话，禁止继续使用旧令牌。 */
  static async refresh(): Promise<boolean> {
    const familyId = (await SessionStore.store?.get(KEY_FAMILY_ID, '')) as string;
    const refreshToken = (await SessionStore.store?.get(KEY_REFRESH_TOKEN, '')) as string;
    if (familyId === '' || refreshToken === '') {
      return false;
    }
    try {
      const tokens = await SessionStore.api.refresh(
        familyId,
        refreshToken,
        await SessionStore.resolveDeviceId()
      );
      await SessionStore.apply(tokens);
      return true;
    } catch (error) {
      await SessionStore.logout();
      return false;
    }
  }

  static async logout(): Promise<void> {
    SessionStore.accessToken = '';
    SessionStore.http.setAccessToken(null);
    await SessionStore.store?.delete(KEY_ACCESS_TOKEN);
    await SessionStore.store?.delete(KEY_REFRESH_TOKEN);
    await SessionStore.store?.delete(KEY_FAMILY_ID);
    await SessionStore.store?.flush();
  }

  private static async apply(tokens: SessionTokens): Promise<void> {
    SessionStore.accessToken = tokens.accessToken;
    SessionStore.ageBand = tokens.ageBand;
    SessionStore.accountStatus = tokens.accountStatus;
    SessionStore.http.setAccessToken(tokens.accessToken);
    await SessionStore.store?.put(KEY_ACCESS_TOKEN, tokens.accessToken);
    await SessionStore.store?.put(KEY_REFRESH_TOKEN, tokens.refreshToken);
    await SessionStore.store?.put(KEY_FAMILY_ID, tokens.sessionFamilyId);
    await SessionStore.store?.put(KEY_AGE_BAND, tokens.ageBand);
    await SessionStore.store?.put(KEY_ACCOUNT_STATUS, tokens.accountStatus);
    await SessionStore.store?.flush();
  }
}
