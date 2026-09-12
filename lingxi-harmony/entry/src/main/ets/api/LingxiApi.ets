import http from '@ohos.net.http';
import { HttpClient } from './HttpClient';
import {
  AchievementResult,
  AgentRunResult,
  CheckInResult,
  CheckInResultType,
  ConversationResult,
  GoalResult,
  GuardianInvitationResult,
  GuardianRelationResult,
  NotificationChannel,
  NotificationPreferenceResult,
  NotificationResult,
  OccurrenceResult,
  PageResult,
  PlanActionDraft,
  PlanMilestoneDraft,
  PrivacyRequestResult,
  PrivacyRequestType,
  RegisteredUserResult,
  ReviewResult,
  SessionTokens,
  SupportTicketResult
} from '../model/Types';

/** 与 PC Web 完全一致的接口封装；两端不得出现业务能力差异。 */
export class LingxiApi {
  readonly http: HttpClient;

  constructor(http: HttpClient) {
    this.http = http;
  }

  /** 注册；服务端不返回令牌，需随后登录。 */
  register(requestKey: string, signedAssertion: string, timezone: string): Promise<RegisteredUserResult> {
    return this.http.send<RegisteredUserResult>('/api/v1/auth/registration-sessions', {
      method: 'POST',
      body: { requestKey: requestKey, signedAssertion: signedAssertion, timezone: timezone }
    });
  }

  login(signedAssertion: string, deviceId: string): Promise<SessionTokens> {
    return this.http.send<SessionTokens>('/api/v1/auth/login', {
      method: 'POST',
      body: {
        requestKey: `login-${Date.now()}`,
        signedAssertion: signedAssertion,
        deviceId: deviceId
      }
    });
  }

  /** 手机号登录；首次登录时服务端按出生日期自动建立账号。 */
  loginWithPhone(phoneNumber: string, birthDate: string, deviceId: string): Promise<SessionTokens> {
    return this.http.send<SessionTokens>('/api/v1/auth/phone-sessions', {
      method: 'POST',
      body: {
        phoneNumber: phoneNumber,
        birthDate: birthDate,
        deviceId: deviceId,
        timezone: 'Asia/Shanghai'
      }
    });
  }

  /** 刷新令牌；成功后必须同时替换 access 与 refresh。 */
  refresh(sessionFamilyId: string, refreshToken: string, deviceId: string): Promise<SessionTokens> {
    return this.http.send<SessionTokens>('/api/v1/auth/refresh', {
      method: 'POST',
      body: {
        sessionFamilyId: sessionFamilyId,
        refreshToken: refreshToken,
        deviceId: deviceId
      }
    });
  }

  /** 运行时启动配置：升级策略、合规文档与功能开关，与 PC Web 共用。 */
  bootstrap(platform: string): Promise<object> {
    return this.http.send<object>('/api/v1/runtime/bootstrap', {
      query: { platform: platform, currentVersionCode: '0' }
    });
  }

  listGoals(): Promise<GoalResult[]> {
    return this.http.send<GoalResult[]>('/api/v1/goals');
  }

  createGoal(title: string, successCriteria: string): Promise<GoalResult> {
    return this.http.send<GoalResult>('/api/v1/goals', {
      method: 'POST',
      body: { title: title, successCriteria: successCriteria },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  confirmPlan(
    goal: GoalResult,
    milestones: PlanMilestoneDraft[],
    actions: PlanActionDraft[]
  ): Promise<GoalResult> {
    return this.http.send<GoalResult>(`/api/v1/goals/${goal.goalId}/plan-confirmations`, {
      method: 'POST',
      body: {
        expectedGoalVersion: goal.version,
        planSnapshotJson: JSON.stringify({ source: 'HARMONY' }),
        adjustmentReason: null,
        milestones: milestones,
        actions: actions
      },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  listOccurrences(fromDate: string, toDate: string): Promise<OccurrenceResult[]> {
    return this.http.send<OccurrenceResult[]>('/api/v1/action-occurrences', {
      query: { from: fromDate, to: toDate }
    });
  }

  checkIn(
    occurrenceId: number,
    result: CheckInResultType,
    note: string | null,
    correction: boolean
  ): Promise<CheckInResult> {
    return this.http.send<CheckInResult>(`/api/v1/action-occurrences/${occurrenceId}/check-ins`, {
      method: http.RequestMethod.PUT,
      body: {
        result: result,
        note: note,
        evidenceReference: null,
        correction: correction
      },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  listAchievements(goalId: number | null, page: number, pageSize: number): Promise<PageResult<AchievementResult>> {
    const query: Record<string, string> = { page: `${page}`, pageSize: `${pageSize}` };
    if (goalId !== null) {
      query['goalId'] = `${goalId}`;
    }
    return this.http.send<PageResult<AchievementResult>>('/api/v1/achievements', { query: query });
  }

  /** 分页查询本人复盘，可按目标过滤。 */
  listReviews(goalId: string | null, page: number, pageSize: number): Promise<PageResult<ReviewResult>> {
    const query: Record<string, string> = { page: `${page}`, pageSize: `${pageSize}` };
    if (goalId !== null) {
      query['goalId'] = goalId;
    }
    return this.http.send<PageResult<ReviewResult>>('/api/v1/reviews', { query: query });
  }

  /** 查询单条复盘详情。 */
  getReview(reviewId: string): Promise<ReviewResult> {
    return this.http.send<ReviewResult>(`/api/v1/reviews/${reviewId}`);
  }

  /** 分页查询本人客服工单，按创建时间倒序。 */
  listSupportTickets(page: number, pageSize: number): Promise<PageResult<SupportTicketResult>> {
    return this.http.send<PageResult<SupportTicketResult>>('/api/v1/support-tickets', {
      query: { page: `${page}`, pageSize: `${pageSize}` }
    });
  }

  /** 分页查询与本人相关的监护关系（青少年侧与监护人侧）。 */
  listGuardianRelations(page: number, pageSize: number): Promise<PageResult<GuardianRelationResult>> {
    return this.http.send<PageResult<GuardianRelationResult>>('/api/v1/guardian-relations', {
      query: { page: `${page}`, pageSize: `${pageSize}` }
    });
  }

  /**
   * 保存通知偏好。
   *
   * `expectedVersion` 首次传 `'0'`，之后必须回传上次响应中的 `version`，否则服务端按乐观并发返回 409。
   */
  updateNotificationPreference(
    scene: string,
    channels: NotificationChannel[],
    quietStart: string | null,
    quietEnd: string | null,
    timezone: string,
    expectedVersion: string
  ): Promise<NotificationPreferenceResult> {
    return this.http.send<NotificationPreferenceResult>('/api/v1/notification-preferences', {
      method: http.RequestMethod.PUT,
      body: {
        scene: scene,
        channels: channels,
        quietStart: quietStart,
        quietEnd: quietEnd,
        timezone: timezone,
        expectedVersion: expectedVersion
      }
    });
  }

  listNotifications(cursor: number, limit: number): Promise<NotificationResult[]> {
    return this.http.send<NotificationResult[]>('/api/v1/notifications', {
      query: { cursor: `${cursor}`, limit: `${limit}` }
    });
  }

  markNotificationRead(id: number): Promise<void> {
    return this.http.send<void>(`/api/v1/notifications/${id}/read`, {
      method: http.RequestMethod.PUT
    });
  }

  createConversation(scene: string, title: string): Promise<ConversationResult> {
    return this.http.send<ConversationResult>('/api/v1/conversations', {
      method: 'POST',
      body: { scene: scene, title: title },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  startRun(conversationId: number, scene: string, input: string): Promise<AgentRunResult> {
    return this.http.send<AgentRunResult>('/api/v1/agent-runs', {
      method: 'POST',
      body: {
        conversationId: conversationId,
        scene: scene,
        input: input,
        attachmentFileIds: []
      },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  confirmProposal(agentRun: AgentRunResult, decision: string): Promise<AgentRunResult> {
    return this.http.send<AgentRunResult>(`/api/v1/agent-runs/${agentRun.id}/confirmations`, {
      method: 'POST',
      body: {
        proposalId: agentRun.proposal?.id ?? 0,
        decision: decision,
        digest: agentRun.proposal?.digest ?? '',
        expectedVersion: agentRun.proposal?.version ?? 0
      }
    });
  }

  inviteGuardian(permissions: string[]): Promise<GuardianInvitationResult> {
    return this.http.send<GuardianInvitationResult>('/api/v1/guardian-relations/invitations', {
      method: 'POST',
      body: { permissions: permissions },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  createPrivacyRequest(type: PrivacyRequestType, scopeJson: string): Promise<PrivacyRequestResult> {
    return this.http.send<PrivacyRequestResult>('/api/v1/privacy/requests', {
      method: 'POST',
      body: { type: type, scopeJson: scopeJson },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  getPrivacyRequest(requestId: number): Promise<PrivacyRequestResult> {
    return this.http.send<PrivacyRequestResult>(`/api/v1/privacy/requests/${requestId}`);
  }
}
