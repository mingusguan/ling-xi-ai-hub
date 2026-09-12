/**
 * 灵犀伴行用户端 API 客户端。
 *
 * 契约来源：`lingxi-server` 各模块 Controller 与 api 包中的 record 定义；
 * 成功响应固定 `code === 'OK'`，写接口按契约要求携带 `Idempotency-Key`。
 */
export { ApiClient, ApiError } from './http';
export type {
  ApiEnvelope,
  ApiClientOptions,
  PageResult,
  RequestOptions,
  StreamEvent,
  StreamOptions
} from './http';
export * from './types';
export { GoalApi } from './endpoints/goals';
export type {
  CheckInBody,
  ConfirmDraftBody,
  ConfirmPlanBody,
  CreateGoalBody,
  PlanDraftBody,
  ReviewBody
} from './endpoints/goals';
export { EngagementApi } from './endpoints/engagement';
export type { CalendarBody, CommandBody, PreferenceBody } from './endpoints/engagement';
export { IdentityApi } from './endpoints/identity';
export type {
  LoginBody,
  PhoneLoginBody,
  PrivacyRequestBody,
  RefreshBody,
  RegisterBody
} from './endpoints/identity';
export { CompanionApi, parseAgentEvent } from './endpoints/companion';
export type { ConfirmationBody, ConversationBody, RunBody } from './endpoints/companion';
export { GuardianApi, RuntimeApi, SupportApi } from './endpoints/support';

import { ApiClient } from './http';
import { CompanionApi } from './endpoints/companion';
import { EngagementApi } from './endpoints/engagement';
import { GoalApi } from './endpoints/goals';
import { IdentityApi } from './endpoints/identity';
import { GuardianApi, RuntimeApi, SupportApi } from './endpoints/support';

/** 聚合客户端，用户端所有接口统一从这里访问。 */
export interface LingxiApi {
  http: ApiClient;
  identity: IdentityApi;
  goals: GoalApi;
  achievements: AchievementApiView;
  engagement: EngagementApi;
  companion: CompanionApi;
  guardian: GuardianApi;
  runtime: RuntimeApi;
  support: SupportApi;
}

/** 成就查询入口（与目标接口同属 goal 模块）。 */
export interface AchievementApiView {
  list(
    goalId: import('./http').LongId | null,
    page?: number,
    pageSize?: number
  ): Promise<import('./http').PageResult<import('./types').AchievementResult>>;
}

export function createLingxiApi(client: ApiClient): LingxiApi {
  const goals = new GoalApi(client);
  return {
    http: client,
    identity: new IdentityApi(client),
    goals,
    achievements: { list: (goalId, page, pageSize) => goals.listAchievements(goalId, page, pageSize) },
    engagement: new EngagementApi(client),
    companion: new CompanionApi(client),
    guardian: new GuardianApi(client),
    runtime: new RuntimeApi(client),
    support: new SupportApi(client)
  };
}
