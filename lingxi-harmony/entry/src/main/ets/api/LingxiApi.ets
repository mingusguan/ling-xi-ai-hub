import http from '@ohos.net.http';
import { HttpClient } from './HttpClient';
import {
  AchievementResult,
  ActionDifficulty,
  ActionExceptionResult,
  ActionExceptionType,
  ActionInput,
  ActionResult,
  AgentRunResult,
  CalendarBindingResult,
  CheckInResult,
  CheckInResultType,
  CheckInView,
  ConversationResult,
  EnergyLevel,
  EntitlementResult,
  FileResult,
  FocusSessionResult,
  GoalQuotaResult,
  GoalResult,
  GoalTransition,
  GoalType,
  GuardianInvitationResult,
  GuardianRelationResult,
  MemoryResult,
  MoodLevel,
  NotificationChannel,
  NotificationPreferenceResult,
  NotificationResult,
  OccurrenceResult,
  OrderResult,
  PageResult,
  PartnerGrantResult,
  PartnerRelationResult,
  PlanActionDraft,
  PlanMilestoneDraft,
  PrivacyRequestResult,
  PrivacyRequestType,
  QuickNoteResult,
  TodayResult,
  ProductResult,
  RegisteredUserResult,
  ReviewResult,
  SessionTokens,
  ShareLinkResult,
  SubscriptionResult,
  SupportTicketResult,
  TemplateVersionResult
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

  /**
   * 创建草稿目标。
   *
   * 草稿不计入活跃目标上限；除标题与完成标准外均可省略，
   * 服务端会为目标类型、优先级与隐私级别补齐默认值。
   */
  createGoal(
    title: string,
    successCriteria: string,
    description: string | null = null,
    goalType: GoalType | null = null,
    startDate: string | null = null,
    targetEndDate: string | null = null
  ): Promise<GoalResult> {
    return this.http.send<GoalResult>('/api/v1/goals', {
      method: 'POST',
      body: {
        title: title,
        successCriteria: successCriteria,
        description: description,
        goalType: goalType,
        startDate: startDate,
        targetEndDate: targetEndDate
      },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  /** 查询当前活跃目标配额。 */
  quota(): Promise<GoalQuotaResult> {
    return this.http.send<GoalQuotaResult>('/api/v1/goal-quota');
  }

  /**
   * 目标生命周期流转：暂停、恢复、放弃、归档。
   *
   * 服务端按目标状态幂等，重复提交同一动作不会报错，因此不需要幂等键。
   */
  transitionGoal(
    goalId: number,
    transition: GoalTransition,
    expectedVersion: number,
    expectedResumeDate: string | null = null,
    abandonReason: string | null = null
  ): Promise<GoalResult> {
    return this.http.send<GoalResult>(`/api/v1/goals/${goalId}/transitions`, {
      method: 'POST',
      body: {
        transition: transition,
        expectedVersion: expectedVersion,
        expectedResumeDate: expectedResumeDate,
        abandonReason: abandonReason
      }
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

  /** 查询目标当前生效计划下的全部行动。 */
  listActions(goalId: number): Promise<ActionResult[]> {
    return this.http.send<ActionResult[]>(`/api/v1/goals/${goalId}/actions`);
  }

  /** 计划外新增行动；追加到目标当前生效的计划版本上。 */
  addAction(goalId: number, milestoneSequence: number | null, definition: ActionInput): Promise<ActionResult> {
    return this.http.send<ActionResult>(`/api/v1/goals/${goalId}/actions`, {
      method: 'POST',
      body: { milestoneSequence: milestoneSequence, definition: definition },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  /**
   * 编辑行动定义，「本次及未来」整体生效。
   *
   * 由行动乐观版本号保护，不需要幂等键；`expectedVersion` 必须来自最近一次查询结果。
   */
  editAction(actionId: number, expectedVersion: number, definition: ActionInput): Promise<ActionResult> {
    return this.http.send<ActionResult>(`/api/v1/actions/${actionId}`, {
      method: http.RequestMethod.PUT,
      body: { expectedVersion: expectedVersion, definition: definition }
    });
  }

  /** 取消行动；重复调用是安全的空操作，服务端会同时清掉尚未执行的实例。 */
  cancelAction(actionId: number, expectedVersion: number): Promise<ActionResult> {
    return this.http.send<ActionResult>(
      `/api/v1/actions/${actionId}?expectedVersion=${expectedVersion}`,
      { method: http.RequestMethod.DELETE }
    );
  }

  /** 复制行动为同计划内的新行动。 */
  copyAction(actionId: number, clientKey: string, title: string): Promise<ActionResult> {
    return this.http.send<ActionResult>(`/api/v1/actions/${actionId}/copies`, {
      method: 'POST',
      body: { clientKey: clientKey, title: title },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  /** 平移行动开始日期，「本次及未来」整体生效。 */
  moveAction(actionId: number, expectedVersion: number, newStartDate: string): Promise<ActionResult> {
    return this.http.send<ActionResult>(`/api/v1/actions/${actionId}/moves`, {
      method: 'POST',
      body: { expectedVersion: expectedVersion, newStartDate: newStartDate }
    });
  }

  /**
   * 单次调整：只跳过或改期这一次，不改写行动的重复规则。
   *
   * SKIP 不得携带 targetDate，RESCHEDULE 必须携带，服务端会拒绝自相矛盾的组合。
   */
  adjustOccurrence(
    occurrenceId: number,
    type: ActionExceptionType,
    targetDate: string | null,
    reason: string | null
  ): Promise<OccurrenceResult> {
    return this.http.send<OccurrenceResult>(
      `/api/v1/action-occurrences/${occurrenceId}/adjustments`,
      {
        method: 'POST',
        body: { type: type, targetDate: targetDate, reason: reason },
        idempotencyKey: this.http.newIdempotencyKey()
      }
    );
  }

  /** 查询目标下已登记的单次调整例外。 */
  listActionExceptions(goalId: number): Promise<ActionExceptionResult[]> {
    return this.http.send<ActionExceptionResult[]>(`/api/v1/goals/${goalId}/action-exceptions`);
  }

  /**
   * 今日工作台聚合：今天、逾期、接下来几天分组返回。
   *
   * timezone 决定「今天」的判定口径，必须传用户所在时区。
   */
  today(timezone: string, upcomingDays: number): Promise<TodayResult> {
    return this.http.send<TodayResult>('/api/v1/today', {
      query: { timezone: timezone, upcomingDays: `${upcomingDays}` }
    });
  }

  /** 开始专注会话；已有进行中会话时服务端会拒绝。 */
  startFocus(occurrenceId: number | null, plannedMinutes: number | null): Promise<FocusSessionResult> {
    return this.http.send<FocusSessionResult>('/api/v1/focus-sessions', {
      method: 'POST',
      body: { occurrenceId: occurrenceId, plannedMinutes: plannedMinutes },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  /** 查询当前进行中的专注会话；空闲时 data 为 null。 */
  activeFocus(): Promise<FocusSessionResult | null> {
    return this.http.send<FocusSessionResult | null>('/api/v1/focus-sessions/active');
  }

  /** 专注计时流转：暂停、恢复、结束、作废；由会话乐观版本号保护。 */
  transitionFocus(
    sessionId: number,
    action: string,
    expectedVersion: number,
    note: string | null
  ): Promise<FocusSessionResult> {
    return this.http.send<FocusSessionResult>(`/api/v1/focus-sessions/${sessionId}/transitions`, {
      method: 'POST',
      body: { action: action, expectedVersion: expectedVersion, note: note }
    });
  }

  /** 新建快速记录；不绑定行动。 */
  createQuickNote(content: string, moodLevel: MoodLevel | null, timezone: string): Promise<QuickNoteResult> {
    return this.http.send<QuickNoteResult>('/api/v1/quick-notes', {
      method: 'POST',
      body: { content: content, moodLevel: moodLevel, timezone: timezone },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  /** 查询最近的快速记录，按创建时间倒序。 */
  listQuickNotes(limit: number): Promise<QuickNoteResult[]> {
    return this.http.send<QuickNoteResult[]>('/api/v1/quick-notes', { query: { limit: `${limit}` } });
  }

  /** 删除快速记录；服务端逻辑删除。 */
  deleteQuickNote(noteId: number): Promise<void> {
    return this.http.send<void>(`/api/v1/quick-notes/${noteId}`, {
      method: http.RequestMethod.DELETE
    });
  }

  /**
   * 打卡或更正记录。
   *
   * FAILED 必须给出 failureReason；非 FAILED 不允许携带，服务端会拒绝自相矛盾的组合。
   * correction 为 true 时不覆盖原记录：旧记录被标记失效并保留为历史。
   */
  checkIn(
    occurrenceId: number,
    result: CheckInResultType,
    note: string | null,
    actualMinutes: number | null,
    perceivedDifficulty: ActionDifficulty | null,
    energyLevel: EnergyLevel | null,
    moodLevel: MoodLevel | null,
    failureReason: string | null,
    correction: boolean
  ): Promise<CheckInResult> {
    return this.http.send<CheckInResult>(`/api/v1/action-occurrences/${occurrenceId}/check-ins`, {
      method: http.RequestMethod.PUT,
      body: {
        result: result,
        note: note,
        evidenceReference: null,
        actualMinutes: actualMinutes,
        perceivedDifficulty: perceivedDifficulty,
        energyLevel: energyLevel,
        moodLevel: moodLevel,
        failureReason: failureReason,
        correction: correction
      },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  /** 查询当前有效打卡记录，供更正入口预填；未打卡时 data 为 null。 */
  getEffectiveCheckIn(occurrenceId: number): Promise<CheckInView | null> {
    return this.http.send<CheckInView | null>(
      `/api/v1/action-occurrences/${occurrenceId}/check-in`
    );
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

  /** 查询本人全部日历绑定。 */
  listCalendarBindings(): Promise<CalendarBindingResult[]> {
    return this.http.send<CalendarBindingResult[]>('/api/v1/calendar/bindings');
  }

  /** 绑定外部日历；需要服务端近期认证。 */
  bindCalendar(provider: string, credentialReference: string): Promise<CalendarBindingResult> {
    return this.http.send<CalendarBindingResult>('/api/v1/calendar/bindings', {
      method: http.RequestMethod.POST,
      body: { provider: provider, credentialReference: credentialReference },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  /** 撤销外部日历绑定；deleteCreatedEvents 决定是否同时删除已投影事件。 */
  revokeCalendar(bindingId: string, deleteCreatedEvents: boolean): Promise<CalendarBindingResult> {
    return this.http.send<CalendarBindingResult>(
      `/api/v1/calendar/bindings/${bindingId}?deleteCreatedEvents=${deleteCreatedEvents}`,
      { method: http.RequestMethod.DELETE }
    );
  }

  /** 分页查询本人订单。 */
  listOrders(page: number, pageSize: number): Promise<PageResult<OrderResult>> {
    return this.http.send<PageResult<OrderResult>>('/api/v1/orders', {
      query: { page: `${page}`, pageSize: `${pageSize}` }
    });
  }

  /** 分页查询本人订阅（含已取消与已过期）。 */
  listSubscriptions(page: number, pageSize: number): Promise<PageResult<SubscriptionResult>> {
    return this.http.send<PageResult<SubscriptionResult>>('/api/v1/subscriptions', {
      query: { page: `${page}`, pageSize: `${pageSize}` }
    });
  }

  /** 查询本人权益余额总览。 */
  listEntitlements(): Promise<EntitlementResult[]> {
    return this.http.send<EntitlementResult[]>('/api/v1/entitlements');
  }

  /** 查询当前场景可购买商品；服务端按年龄策略过滤。 */
  listProducts(scene: string): Promise<ProductResult[]> {
    return this.http.send<ProductResult[]>('/api/v1/products', { query: { scene: scene } });
  }

  /** 分页查询本人文件。 */
  listMyFiles(page: number, pageSize: number): Promise<PageResult<FileResult>> {
    return this.http.send<PageResult<FileResult>>('/api/v1/files', {
      query: { page: `${page}`, pageSize: `${pageSize}` }
    });
  }

  /** 查询已发布内容模板；服务端按账号年龄范围过滤。 */
  listTemplates(): Promise<TemplateVersionResult[]> {
    return this.http.send<TemplateVersionResult[]>('/api/v1/templates');
  }

  /** 分页查询与本人相关的伙伴关系。 */
  listPartnerRelations(page: number, pageSize: number): Promise<PageResult<PartnerRelationResult>> {
    return this.http.send<PageResult<PartnerRelationResult>>('/api/v1/relationships/partners', {
      query: { page: `${page}`, pageSize: `${pageSize}` }
    });
  }

  /** 接受伙伴邀请。 */
  acceptPartnerRelation(relationId: string, expectedVersion: string): Promise<PartnerRelationResult> {
    return this.http.send<PartnerRelationResult>(
      `/api/v1/relationships/partners/${relationId}/accept`,
      {
        method: http.RequestMethod.POST,
        body: { expectedVersion: expectedVersion }
      }
    );
  }

  /** 查询本人作为授权方发出的目标授权。 */
  listPartnerGrants(): Promise<PartnerGrantResult[]> {
    return this.http.send<PartnerGrantResult[]>('/api/v1/relationships/grants');
  }

  /** 分页查询本人创建的分享链接；列表不回显明文 token。 */
  listShares(page: number, pageSize: number): Promise<PageResult<ShareLinkResult>> {
    return this.http.send<PageResult<ShareLinkResult>>('/api/v1/shares', {
      query: { page: `${page}`, pageSize: `${pageSize}` }
    });
  }

  /**
   * 创建受控分享链接。
   *
   * `expiresAt` 为必填且必须晚于当前时刻，服务端会拒绝空值或过期时间。
   */
  createShare(
    resourceType: string,
    resourceId: string,
    fields: string[],
    expiresAt: string,
    visitLimit: number | null,
    password: string | null
  ): Promise<ShareLinkResult> {
    return this.http.send<ShareLinkResult>('/api/v1/shares', {
      method: http.RequestMethod.POST,
      body: {
        resourceType: resourceType,
        resourceId: resourceId,
        fields: fields,
        expiresAt: expiresAt,
        visitLimit: visitLimit,
        password: password
      },
      idempotencyKey: this.http.newIdempotencyKey()
    });
  }

  /** 撤销分享链接。 */
  revokeShare(shareId: string, expectedVersion: string): Promise<void> {
    return this.http.send<void>(
      `/api/v1/shares/${shareId}?expectedVersion=${expectedVersion}`,
      { method: http.RequestMethod.DELETE }
    );
  }

  /** 列出长期记忆。 */
  listMemories(): Promise<MemoryResult[]> {
    return this.http.send<MemoryResult[]>('/api/v1/memories');
  }

  /** 删除长期记忆；服务端异步传播删除。 */
  deleteMemory(memoryId: string, expectedVersion: string): Promise<MemoryResult> {
    return this.http.send<MemoryResult>(
      `/api/v1/memories/${memoryId}?expectedVersion=${expectedVersion}`,
      { method: http.RequestMethod.DELETE }
    );
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
