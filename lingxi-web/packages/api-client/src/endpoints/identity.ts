import type { ApiClient, LongId } from '../http';
import type {
  CommonBlocker,
  CommunicationStyle,
  OnboardingProfileResult,
  PrivacyExportResult,
  PrivacyRequestResult,
  PrivacyRequestType,
  ProactivityLevel,
  RegisteredUserResult,
  SessionTokens
} from '../types';

/** 注册请求体，对应 IdentityController.RegisterRequest。 */
export interface RegisterBody {
  requestKey: string;
  /** 外部身份桥接层签发的 HMAC 断言，客户端不持有签发密钥。 */
  signedAssertion: string;
  timezone: string;
}

/** 登录请求体，对应 IdentityController.LoginRequest。 */
export interface LoginBody {
  requestKey: string;
  signedAssertion: string;
  /** 设备标识；刷新令牌时必须与登录时一致。 */
  deviceId: string;
}

/** 刷新请求体，对应 IdentityController.RefreshRequest。 */
export interface RefreshBody {
  sessionFamilyId: string;
  refreshToken: string;
  deviceId: string;
}

/**
 * 手机号登录请求体，对应 IdentityController.PhoneLoginBody。
 *
 * 手机号尚未绑定账号时服务端会按 birthDate 自动建立账号，因此首次登录必须提供出生日期。
 */
export interface PhoneLoginBody {
  phoneNumber: string;
  /** 出生日期（YYYY-MM-DD）；仅首次登录必填。 */
  birthDate: string | null;
  deviceId: string;
  timezone: string;
}

/** 隐私请求体，对应 IdentityController.PrivacyRequestBody。 */
export interface PrivacyRequestBody {
  type: PrivacyRequestType;
  /** 仅允许 modules 字段，例如 {"modules":["identity","goal"]}；CLOSE_ACCOUNT 必须为全模块。 */
  scopeJson: string;
}

/**
 * 新手引导画像更新请求体，对应 OnboardingController.SaveProfileBody。
 *
 * 所有画像字段都可为空：PRD 要求引导可跳过，且禁止强制收集真实姓名、职业单位等
 * 完成核心功能非必需的信息。`complete` 传 true 即表示「填完了」或「跳过」。
 */
export interface SaveOnboardingProfileBody {
  /** 用户自定义称呼（昵称），不是实名。 */
  nickname: string | null;
  /** 通常入睡 / 起床时刻（HH:mm:ss），本地时间。 */
  sleepTime: string | null;
  wakeTime: string | null;
  /** 画像级别的每周可用时间（分钟）。 */
  weeklyAvailableMinutes: number | null;
  remindWindowStart: string | null;
  remindWindowEnd: string | null;
  quietHoursStart: string | null;
  quietHoursEnd: string | null;
  communicationStyle: CommunicationStyle | null;
  proactivityLevel: ProactivityLevel | null;
  commonBlockers: CommonBlocker[];
  /** 是否同时结束引导；点「跳过」时同样传 true。 */
  complete: boolean;
  /** 客户端持有的账号版本；不一致时服务端返回 IDENTITY_VERSION_CONFLICT。 */
  expectedVersion: LongId;
}

/** 身份、会话与隐私权利接口客户端。 */
export class IdentityApi {
  constructor(private readonly client: ApiClient) {}

  /** 注册；服务端只返回用户信息，需随后登录获取令牌。 */
  register(body: RegisterBody): Promise<RegisteredUserResult> {
    return this.client.send<RegisteredUserResult>('/api/v1/auth/registration-sessions', {
      method: 'POST',
      body,
      anonymous: true
    });
  }

  /** 使用身份断言登录并获取 Opaque 访问令牌与刷新令牌。 */
  login(body: LoginBody): Promise<SessionTokens> {
    return this.client.send<SessionTokens>('/api/v1/auth/login', {
      method: 'POST',
      body,
      anonymous: true
    });
  }

  /** 手机号登录；手机号未绑定账号时服务端按出生日期自动建立账号。 */
  loginWithPhone(body: PhoneLoginBody): Promise<SessionTokens> {
    return this.client.send<SessionTokens>('/api/v1/auth/phone-sessions', {
      method: 'POST',
      body,
      anonymous: true
    });
  }

  /** 刷新令牌；成功后必须同时替换 accessToken 与 refreshToken。 */
  refresh(body: RefreshBody): Promise<SessionTokens> {
    return this.client.send<SessionTokens>('/api/v1/auth/refresh', {
      method: 'POST',
      body,
      anonymous: true
    });
  }

  /** 创建隐私权利请求，需要近期认证。 */
  createPrivacyRequest(body: PrivacyRequestBody, idempotencyKey: string): Promise<PrivacyRequestResult> {
    return this.client.send<PrivacyRequestResult>('/api/v1/privacy/requests', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 查询隐私请求进度。 */
  getPrivacyRequest(requestId: LongId): Promise<PrivacyRequestResult> {
    return this.client.send<PrivacyRequestResult>(`/api/v1/privacy/requests/${requestId}`);
  }

  /** 在冷静期内撤销注销请求。 */
  cancelPrivacyRequest(requestId: LongId): Promise<PrivacyRequestResult> {
    return this.client.send<PrivacyRequestResult>(`/api/v1/privacy/requests/${requestId}/cancel`, {
      method: 'POST'
    });
  }

  /** 下载隐私导出包内容。 */
  downloadPrivacyExport(requestId: LongId): Promise<PrivacyExportResult> {
    return this.client.send<PrivacyExportResult>(`/api/v1/privacy/exports/${requestId}`);
  }

  /** 提交年龄申诉。 */
  submitAgeAppeal(claimedBirthDate: string, evidenceRef: string): Promise<unknown> {
    return this.client.send<unknown>('/api/v1/age-appeals', {
      method: 'POST',
      body: { claimedBirthDate, evidenceRef }
    });
  }

  /** 读取新手引导状态与已填画像。 */
  getOnboardingProfile(): Promise<OnboardingProfileResult> {
    return this.client.send<OnboardingProfileResult>('/api/v1/onboarding/profile');
  }

  /**
   * 覆盖写入新手引导画像，可选同时结束引导。
   *
   * 由账号乐观版本号保护，因此不要求幂等键：同一份画像重复提交结果相同。
   */
  saveOnboardingProfile(body: SaveOnboardingProfileBody): Promise<OnboardingProfileResult> {
    return this.client.send<OnboardingProfileResult>('/api/v1/onboarding/profile', {
      method: 'PUT',
      body
    });
  }

  /** 重新进入新手引导（用户在设置里主动重做）；保留已填画像。 */
  reopenOnboarding(expectedVersion: LongId): Promise<OnboardingProfileResult> {
    return this.client.send<OnboardingProfileResult>('/api/v1/onboarding/reopen', {
      method: 'POST',
      body: { expectedVersion }
    });
  }
}
