/**
 * 与 lingxi-server 契约一致的领域类型。
 *
 * 枚举取值来自服务端枚举定义；两端（PC Web 与 HarmonyOS）必须保持同一份语义。
 */

export type GoalStatus =
  | 'DRAFT'
  | 'PLANNING'
  | 'PENDING_CONFIRMATION'
  | 'ACTIVE'
  | 'PAUSED'
  | 'COMPLETED'
  | 'ABANDONED'
  | 'ARCHIVED';

/**
 * 行动重复类型，与服务端 com.lingxi.goal.api.RecurrenceType 一致。
 *
 * 按天=DAILY；周与自定义星期=WEEKLY（必须给 weekdays）；工作日=WEEKDAYS；
 * 间隔重复=INTERVAL（必须给 intervalDays）。
 */
export type RecurrenceType = 'ONCE' | 'DAILY' | 'WEEKLY' | 'WEEKDAYS' | 'INTERVAL';

export type OccurrenceStatus =
  | 'SCHEDULED'
  | 'COMPLETED'
  | 'PARTIAL'
  | 'SKIPPED'
  /** 尝试过但没做成，与「主动跳过」语义不同。 */
  | 'FAILED'
  | 'MISSED';

/**
 * 打卡结果。
 *
 * SKIPPED 是用户主动决定这次不做；FAILED 是尝试了但没做成，必须记录失败原因。
 */
export type CheckInResultType = 'COMPLETED' | 'PARTIAL' | 'SKIPPED' | 'FAILED';

/** 打卡时的精力自评。 */
export type EnergyLevel = 'LOW' | 'NORMAL' | 'HIGH';

/**
 * 打卡时的情绪自评，五档中性量表。
 *
 * 仅用于复盘观察趋势与调整节奏，不产出任何诊断结论，也不作为风险判定依据。
 */
export type MoodLevel = 'VERY_LOW' | 'LOW' | 'NEUTRAL' | 'GOOD' | 'VERY_GOOD';

export type AchievementType = 'GOAL_COMPLETED' | 'MILESTONE_COMPLETED' | 'CHECK_IN_STREAK';

export type AgeBand = 'UNDER_14' | 'TEEN' | 'ADULT';

export type AccountStatus =
  | 'PENDING_GUARDIAN'
  | 'ACTIVE_TEEN'
  | 'ACTIVE_ADULT'
  | 'RESTRICTED'
  | 'AGE_TRANSITION'
  | 'CLOSING'
  | 'CLOSED';

export type PrivacyRequestType = 'EXPORT' | 'CORRECTION' | 'DELETE_DATA' | 'CLOSE_ACCOUNT';

/** 统一响应包裹，对应 com.lingxi.kernel.ApiResponse。 */
export interface ApiEnvelope<T> {
  code: string;
  message: string;
  data: T;
  requestId: string;
}

/** 分页结果，对应 com.lingxi.kernel.PageResult。 */
export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface SessionTokens {
  accessToken: string;
  refreshToken: string;
  sessionFamilyId: string;
  accessExpiresAt: string;
  refreshExpiresAt: string;
  ageBand: AgeBand;
  accountStatus: AccountStatus;
  authorizationVersion: number;
}

export interface RegisteredUserResult {
  userId: number;
  publicId: string;
  ageBand: AgeBand;
  status: AccountStatus;
  authorizationVersion: number;
}

/** 目标类型，与 PRD「支持的目标类型」十类一一对应。 */
export type GoalType =
  | 'LEARNING_EXAM'
  | 'CAREER'
  | 'CREATIVE'
  | 'READING'
  | 'HABIT'
  | 'FITNESS'
  | 'TRAVEL'
  | 'COMMUNICATION'
  | 'BUDGET'
  | 'EMOTION';

/** 目标优先级。 */
export type PriorityLevel = 'LOW' | 'NORMAL' | 'HIGH';

/**
 * 目标隐私级别。
 *
 * PARTNER_VISIBLE 只是可见前提，真正对伙伴可见仍需要按目标的最小化授权记录。
 */
export type GoalPrivacyLevel = 'PRIVATE' | 'PARTNER_VISIBLE';

/** 目标生命周期动作。 */
export type GoalTransition = 'PAUSE' | 'RESUME' | 'ABANDON' | 'ARCHIVE';

export interface GoalResult {
  goalId: number;
  publicId: string;
  userId: number;
  title: string;
  description: string | null;
  successCriteria: string;
  goalType: GoalType;
  startDate: string | null;
  targetEndDate: string | null;
  priority: PriorityLevel;
  /** 每周可用时间，单位分钟。 */
  weeklyAvailableMinutes: number | null;
  resourceConstraints: string | null;
  verifiableOutcomes: string | null;
  privacyLevel: GoalPrivacyLevel;
  status: GoalStatus;
  currentPlanVersionId: number | null;
  progress: number;
  /** 暂停时的预计恢复日期；仅暂停状态有值。 */
  pauseResumeAt: string | null;
  /** 放弃原因；仅放弃状态有值。 */
  abandonReason: string | null;
  version: number;
}

/** 活跃目标配额结果。 */
export interface GoalQuotaResult {
  allowance: number;
  activeCount: number;
}

export interface PlanMilestoneDraft {
  sequenceNo: number;
  title: string;
  successCriteria: string;
}

export interface PlanActionDraft {
  clientKey: string;
  milestoneSequence: number | null;
  title: string;
  /** 行动说明；可为空。 */
  description: string | null;
  recurrenceType: RecurrenceType;
  weekdays: string[];
  /** 间隔重复的间隔天数；仅 recurrenceType=INTERVAL 时必填。 */
  intervalDays: number | null;
  startDate: string;
  endDate: string | null;
  localTime: string;
  /** 时间段结束时刻；为空表示只安排开始时刻。 */
  endLocalTime: string | null;
  timezone: string;
  priority: PriorityLevel;
  difficulty: ActionDifficulty;
  /** 行动的完成标准；与目标的成功标准分开维护。 */
  completionCriteria: string | null;
  /** 预计时长，单位分钟。 */
  estimatedMinutes: number | null;
  /** 前置行动的客户端标识，必须指向同一份草案中的另一个行动。 */
  prerequisiteClientKey: string | null;
  /** 行动级提醒策略；为空表示沿用账号级偏好。 */
  reminderPolicy: string | null;
}

/** 行动难度。 */
export type ActionDifficulty = 'EASY' | 'NORMAL' | 'HARD';

/** 单次调整类型。 */
export type ActionExceptionType = 'SKIP' | 'RESCHEDULE';

/** 行动定义视图。 */
export interface ActionResult {
  actionId: number;
  goalId: number;
  planVersionId: number;
  milestoneId: number | null;
  clientKey: string;
  title: string;
  description: string | null;
  recurrenceType: RecurrenceType;
  weekdays: string[];
  intervalDays: number | null;
  startDate: string;
  endDate: string | null;
  localTime: string;
  endLocalTime: string | null;
  timezone: string;
  priority: PriorityLevel;
  difficulty: ActionDifficulty;
  completionCriteria: string | null;
  estimatedMinutes: number | null;
  prerequisiteActionId: number | null;
  reminderPolicy: string | null;
  status: 'DRAFT' | 'ACTIVE' | 'CANCELLED';
  version: number;
}

/** 专注会话状态。 */
export type FocusSessionStatus = 'RUNNING' | 'PAUSED' | 'FINISHED' | 'ABANDONED';

/** 专注会话视图。 */
export interface FocusSessionResult {
  sessionId: number;
  userId: number;
  occurrenceId: number | null;
  actionId: number | null;
  status: FocusSessionStatus;
  /** 是否进行中（计时中或已暂停）。 */
  active: boolean;
  /** 已累计专注秒数；计时中时包含正在进行的区间。 */
  accumulatedSeconds: number;
  plannedMinutes: number | null;
  startedAt: string;
  lastResumedAt: string | null;
  endedAt: string | null;
  note: string | null;
  version: number;
}

/** 快速记录视图。 */
export interface QuickNoteResult {
  noteId: number;
  userId: number;
  content: string;
  moodLevel: MoodLevel | null;
  localDate: string;
  createdAt: string;
}

/**
 * 今日工作台里的一条行动。
 *
 * overdue 表示未执行且计划日期早于今天；unlocksOthers 是该行动作为多少个其他行动的前置。
 */
export interface TodayItem {
  occurrenceId: number;
  actionId: number;
  goalId: number;
  goalTitle: string;
  actionTitle: string;
  actionDescription: string | null;
  localDate: string;
  scheduledAt: string;
  localTime: string;
  timezone: string;
  status: OccurrenceStatus;
  overdue: boolean;
  estimatedMinutes: number | null;
  priority: PriorityLevel;
  difficulty: ActionDifficulty;
  completionCriteria: string | null;
  unlocksOthers: number;
  exceptionType: ActionExceptionType | null;
}

/** 今日工作台的提示；source 在接入模型前固定为 RULE。 */
export interface TodaySuggestion {
  type: string;
  source: string;
  title: string;
  basis: string;
  relatedOccurrenceId: number | null;
}

/** 今日工作台聚合结果。 */
export interface TodayResult {
  localDate: string;
  today: TodayItem[];
  overdue: TodayItem[];
  upcoming: TodayItem[];
  suggestions: TodaySuggestion[];
  activeFocus: FocusSessionResult | null;
  quickNotes: QuickNoteResult[];
  todayTotal: number;
  todayFinished: number;
  overdueTotal: number;
  windowStart: string;
  generatedAt: string;
}

/** 单次调整例外视图。 */
export interface ActionExceptionResult {
  exceptionId: number;
  actionId: number;
  /** 原计划日期。 */
  localDate: string;
  type: ActionExceptionType;
  /** 改期后的日期；跳过时为 null。 */
  rescheduledDate: string | null;
  reason: string | null;
}

/** 行动定义入参，新增与编辑共用；对应服务端 ActionInput。 */
export interface ActionInput {
  title: string;
  description: string | null;
  recurrenceType: RecurrenceType;
  weekdays: string[];
  intervalDays: number | null;
  startDate: string;
  endDate: string | null;
  localTime: string;
  endLocalTime: string | null;
  timezone: string;
  priority: PriorityLevel;
  difficulty: ActionDifficulty;
  completionCriteria: string | null;
  estimatedMinutes: number | null;
  prerequisiteActionId: number | null;
  reminderPolicy: string | null;
}

export interface OccurrenceResult {
  occurrenceId: number;
  actionId: number;
  actionTitle: string;
  scheduledAt: string;
  localDate: string;
  timezone: string;
  status: OccurrenceStatus;
}

export interface AchievementResult {
  achievementId: number;
  goalId: number | null;
  type: AchievementType;
  title: string;
  description: string | null;
  achievedAt: string;
}

export interface CheckInResult {
  checkInId: number;
  occurrenceId: number;
  result: CheckInResultType;
  occurrenceStatus: OccurrenceStatus;
  goalProgress: number;
  recordedAt: string;
  note: string | null;
  /** 实际耗时，单位分钟。 */
  actualMinutes: number | null;
  perceivedDifficulty: ActionDifficulty | null;
  energyLevel: EnergyLevel | null;
  moodLevel: MoodLevel | null;
  /** 失败原因；仅结果为失败时非空。 */
  failureReason: string | null;
  /** 本次是否为修正：为 true 时原记录已被标记失效并保留为历史。 */
  corrected: boolean;
  newAchievements: AchievementResult[];
}

/** 当前有效打卡记录视图；用于「更正」入口预填。 */
export interface CheckInView {
  checkInId: number;
  occurrenceId: number;
  result: CheckInResultType;
  note: string | null;
  evidenceReference: string | null;
  actualMinutes: number | null;
  perceivedDifficulty: ActionDifficulty | null;
  energyLevel: EnergyLevel | null;
  moodLevel: MoodLevel | null;
  failureReason: string | null;
  recordedAt: string;
}

export interface NotificationResult {
  id: number;
  type: string;
  resourceType: string;
  resourceId: string;
  summary: string;
  status: string;
  createdAt: string;
  readAt: string | null;
  cursor: number;
}

export interface ConversationResult {
  id: number;
  publicId: string;
  userId: number;
  scene: string;
  title: string;
  status: string;
  createdAt: string;
}

export interface ProposalResult {
  id: number;
  publicId: string;
  toolName: string;
  riskLevel: string;
  argumentsJson: string;
  digest: string;
  expiresAt: string;
  status: string;
  version: number;
}

export interface AgentRunResult {
  id: number;
  publicId: string;
  conversationId: number;
  userId: number;
  scene: string;
  status: string;
  resultText: string | null;
  errorCode: string | null;
  version: number;
  proposal: ProposalResult | null;
  createdAt: string;
}

export interface GuardianInvitationResult {
  relationId: number;
  invitationToken: string | null;
  status: string;
  expiresAt: string;
  permissions: string[];
}

export interface PrivacyRequestResult {
  requestId: number;
  type: PrivacyRequestType;
  status: string;
  progress: number;
  deadline: string;
  resultReference: string | null;
}

/** 长期记忆视图；`id`/`userId`/`version` 在响应中为字符串。 */
export interface MemoryResult {
  id: string;
  userId: string;
  purpose: string;
  contentText: string;
  sourceRef: string;
  sensitivity: string;
  status: string;
  version: string;
  updatedAt: string;
}

/**
 * 复盘视图。
 *
 * 注意：服务端把 64 位标识序列化为 JSON 字符串（`JacksonLongAsStringConfiguration`），
 * 因此 `reviewId`/`goalId` 在真实响应中是字符串，需要按字符串处理并原样回传。
 */
export interface ReviewResult {
  reviewId: string;
  goalId: string;
  periodKey: string;
  status: string;
  conclusionJson: string | null;
  completedAt: string | null;
  inputSnapshotJson: string | null;
  createdAt: string | null;
}

/** 客服工单视图；`ticketId`/`userId`/`version` 在响应中为字符串。 */
export interface SupportTicketResult {
  ticketId: string;
  ticketNo: string;
  userId: string;
  category: string;
  subject: string;
  status: string;
  priority: string;
  assigneeAdminId: string | null;
  version: string;
  createdAt: string;
}

export type GuardianPermissionType =
  | 'VIEW_GOAL_PROGRESS'
  | 'MANAGE_SCHEDULE'
  | 'RECEIVE_SAFETY_ALERTS';

/** 监护关系视图；标识字段在响应中为字符串。 */
export interface GuardianRelationResult {
  relationId: string;
  teenUserId: string;
  guardianUserId: string | null;
  status: string;
  permissions: GuardianPermissionType[];
  effectiveAt: string | null;
}

export type NotificationChannel = 'INBOX' | 'PUSH';

/** 通知偏好视图；`userId`/`version` 在响应中为字符串。 */
export interface NotificationPreferenceResult {
  userId: string;
  scene: string;
  channels: NotificationChannel[];
  quietStart: string | null;
  quietEnd: string | null;
  timezone: string;
  version: string;
}

/** 日历绑定视图；`bindingId`/`version` 在响应中为字符串。 */
export interface CalendarBindingResult {
  bindingId: string;
  provider: string;
  status: string;
  version: string;
}

/** 会员商品与价格版本；标识字段在响应中为字符串。 */
export interface ProductResult {
  productId: string;
  productKey: string;
  name: string;
  scene: string;
  priceId: string;
  priceVersion: number;
  amountMinor: string;
  currency: string;
  billingPeriod: string;
  agePolicy: string;
}

/** 订单视图；金额与标识字段在响应中为字符串。 */
export interface OrderResult {
  orderId: string;
  orderNo: string;
  userId: string;
  productId: string;
  amountMinor: string;
  currency: string;
  status: string;
  paymentReference: string | null;
  refundedMinor: string;
  version: string;
  createdAt: string;
}

/** 订阅视图。 */
export interface SubscriptionResult {
  subscriptionId: string;
  userId: string;
  productId: string;
  status: string;
  periodEnd: string | null;
  cancelMode: string | null;
  version: string;
}

/** 权益余额视图。 */
export interface EntitlementResult {
  userId: string;
  resourceKey: string;
  balance: string;
  expiresAt: string | null;
  version: string;
}

/** 私有文件视图。 */
export interface FileResult {
  fileId: string;
  publicId: string;
  ownerUserId: string;
  purpose: string;
  originalName: string;
  sizeBytes: string;
  mimeType: string;
  contentHash: string;
  sensitivity: string;
  status: string;
  scanResult: string | null;
  version: string;
}

/** 内容模板版本视图，客户端只读。 */
export interface TemplateVersionResult {
  versionId: string;
  templateId: string;
  versionNo: number;
  ageScope: string;
  contentSnapshot: string;
  status: string;
  reviewerUserId: string | null;
  reviewReason: string | null;
  publishedAt: string | null;
  version: string;
}

/** 伙伴关系视图。 */
export interface PartnerRelationResult {
  relationId: string;
  inviterUserId: string;
  inviteeUserId: string;
  status: string;
  version: string;
}

/** 伙伴授权权限，与服务端 PartnerPermission 一致。 */
export type PartnerPermission = 'VIEW_PROGRESS' | 'ENCOURAGE' | 'COMMENT' | 'CO_CHECK_IN';

/** 伙伴目标授权视图。 */
export interface PartnerGrantResult {
  grantId: string;
  relationId: string;
  ownerUserId: string;
  goalId: string;
  permissions: PartnerPermission[];
  expiresAt: string | null;
  status: string;
  version: string;
}

/** 受控分享链接视图；rawToken 只在创建时返回一次，列表查询为 null。 */
export interface ShareLinkResult {
  shareId: string;
  rawToken: string | null;
  resourceType: string;
  resourceId: string;
  fields: string[];
  snapshotJson: string | null;
  status: string;
  visitCount: number;
  visitLimit: number | null;
  expiresAt: string | null;
  version: string;
}
