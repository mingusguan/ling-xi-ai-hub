import { ApiError } from '@lingxi/api-client';

/**
 * 身份断言获取适配器。
 *
 * 服务端只接受由受信身份桥接层签发、5 分钟内有效、使用服务端密钥做 HMAC-SHA256 的断言
 * （见 lingxi-server `HmacIdentityAssertionVerifier`）。客户端不持有签发密钥，因此
 * 断言统一由外部身份桥接层提供：
 * - 配置 `VITE_ASSERTION_PROVIDER_URL` 时，向该地址提交渠道与账号标识换取断言；
 * - 未配置时退回手工粘贴断言，便于本地联调。
 */

export interface AssertionRequest {
  /** 身份渠道，例如 SMS、HUAWEI。 */
  channel: string;
  /** 渠道账号标识，例如手机号或华为账号标识。 */
  identifier: string;
  /** 注册场景需要服务端返回已核验出生日期。 */
  requireBirthDate: boolean;
}

export interface AssertionProvider {
  readonly name: string;
  acquire(request: AssertionRequest): Promise<string>;
}

/** 通过外部身份桥接层换取断言。 */
export class HttpAssertionProvider implements AssertionProvider {
  readonly name = 'http-bridge';

  constructor(private readonly endpoint: string) {}

  async acquire(request: AssertionRequest): Promise<string> {
    const response = await fetch(this.endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    });
    if (!response.ok) {
      throw new ApiError('ASSERTION_PROVIDER_FAILED', '身份验证服务暂时不可用，请稍后重试', response.status);
    }
    const payload = (await response.json()) as { signedAssertion?: string };
    if (!payload.signedAssertion) {
      throw new ApiError('ASSERTION_PROVIDER_INVALID', '身份验证服务未返回有效凭证', response.status);
    }
    return payload.signedAssertion;
  }
}

/** 未配置桥接层时由用户手工提供断言，仅用于本地联调。 */
export class ManualAssertionProvider implements AssertionProvider {
  readonly name = 'manual';

  private pending: string | null = null;

  /** 由登录页收集用户粘贴的断言。 */
  submit(signedAssertion: string): void {
    this.pending = signedAssertion.trim();
  }

  async acquire(): Promise<string> {
    if (!this.pending) {
      throw new ApiError('ASSERTION_REQUIRED', '请先提供身份断言', 400);
    }
    const value = this.pending;
    this.pending = null;
    return value;
  }
}

const providerUrl = import.meta.env.VITE_ASSERTION_PROVIDER_URL as string | undefined;

/** 当前生效的断言获取方式。 */
export const manualAssertionProvider = new ManualAssertionProvider();

export const assertionProvider: AssertionProvider = providerUrl
  ? new HttpAssertionProvider(providerUrl)
  : manualAssertionProvider;

/** 是否处于“手工粘贴断言”的本地联调模式。 */
export const isManualAssertionMode = !providerUrl;
