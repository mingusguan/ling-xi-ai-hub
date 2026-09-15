/**
 * 与后端契约一致的领域类型。
 *
 * 枚举取值来自服务端枚举定义，新增取值时保持同步：
 * - GoalStatus / RecurrenceType / CheckInResultType / AchievementType：com.lingxi.goal.api
 * - PlanVersionStatus / OccurrenceStatus / ActionStatus：com.lingxi.goal.domain
 *
 * 标识与游标：服务端 long/Long 统一序列化为 JSON 字符串（见 lingxi-boot
 * JacksonLongAsStringConfiguration），客户端必须按字符串原样保留与回传，禁止 Number 转换。
 */

import type { LongId } from './http';

/** 目标状态。 */
export type GoalStatus =
  | 'DRAFT'
  | 'PLANNING'
  | 'PENDING_CONFIRMATION'
  | 'ACTIVE'
  | 'PAUSED'
  | 'COMPLETED'
  | 'ABANDONED'
  | 'ARCHIVED';

/** 计划版本状态。 */
export type PlanVersionStatus = 'DRAFT' | 'PENDING_CONFIRMATION' | 'ACTIVE' | 'SUPERSEDED' | 'REJECTED';

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

/** 目标优先级，三档最小集合。 */
export type PriorityLevel = 'LOW' | 'NORMAL' | 'HIGH';

/**
 * 目标隐私级别。
 *
 * PARTNER_VISIBLE 只能作为可见的「前提」，真正对伙伴可见仍需要按目标的最小化授权记录，
 * 该字段本身不会授予可见性。
 */
export type GoalPrivacyLevel = 'PRIVATE' | 'PARTNER_VISIBLE';

/** 目标生命周期动作。 */
export type GoalTransition = 'PAUSE' | 'RESUME' | 'ABANDON' | 'ARCHIVE';


/**
 * 行动重复类型。
 *
 * 与 PRD「按天、周、工作日、自定义星期、间隔重复」五种形式对应：
 * 按天=DAILY，周与自定义星期=WEEKLY（必须给 weekdays），工作日=WEEKDAYS，
 * 间隔重复=INTERVAL（必须给 intervalDays）。
 */
export type RecurrenceType = 'ONCE' | 'DAILY' | 'WEEKLY' | 'WEEKDAYS' | 'INTERVAL';

/** 行动实例状态。 */
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


/** 成就类型。 */
export type AchievementType = 'GOAL_COMPLETED' | 'MILESTONE_COMPLETED' | 'CHECK_IN_STREAK';

/** 目标查询与写入结果。 */
export interface GoalResult {
  goalId: LongId;
  publicId: string;
  userId: LongId;
  title: string;
  /** 目标描述；可为空。 */
  description: string | null;
  successCriteria: string;
  goalType: GoalType;
  /** 开始日期；可为空，格式 YYYY-MM-DD。 */
  startDate: string | null;
  /** 期望完成日期；可为空，格式 YYYY-MM-DD。 */
  targetEndDate: string | null;
  priority: PriorityLevel;
  /** 每周可用时间，单位分钟；可为空。 */
  weeklyAvailableMinutes: number | null;
  resourceConstraints: string | null;
  verifiableOutcomes: string | null;
  privacyLevel: GoalPrivacyLevel;
  status: GoalStatus;
  currentPlanVersionId: LongId | null;
  /** 进度为 int，服务端仍以数字输出。 */
  progress: number;
  /** 暂停时的预计恢复日期；仅暂停状态有值。 */
  pauseResumeAt: string | null;
  /** 放弃原因；仅放弃状态有值。 */
  abandonReason: string | null;
  /** 首目标引导澄清阶段（PRD ONB-02）；null 表示不处于引导中。 */
  clarificationStage: GoalClarificationStage | null;
  version: LongId;
}

/** 首目标引导澄清阶段（PRD 8.2 ONB-02）；每次只推进一个阶段。 */
export type GoalClarificationStage =
  | 'AWAITING_GOAL'
  | 'AWAITING_CRITERIA'
  | 'AWAITING_FIRST_ACTION'
  | 'COMPLETE';

/** 入门模板里的首行动候选；时长恒在 5—30 分钟内。 */
export interface StarterFirstAction {
  title: string;
  estimatedMinutes: number;
  priority: PriorityLevel;
  difficulty: ActionDifficulty;
  completionCriteria: string | null;
}

/** 入门目标模板（PRD ONB-02 的模板入口）；由服务端随版本提供，对所有用户一致。 */
export interface StarterGoalTemplate {
  templateKey: string;
  name: string;
  summary: string | null;
  goalType: GoalType;
  defaultSuccessCriteria: string | null;
  tags: string[];
  firstActions: StarterFirstAction[];
}

/** 活跃目标配额结果。 */
export interface GoalQuotaResult {
  allowance: number;
  activeCount: number;
}

/** 计划草案结果。 */
export interface PlanDraftResult {
  planVersionId: LongId;
  goalId: LongId;
  versionNo: number;
  status: PlanVersionStatus;
  source: string;
}

/** 计划里程碑草案。 */
export interface PlanMilestoneDraft {
  sequenceNo: number;
  title: string;
  successCriteria: string;
}

/** 计划行动草案。 */
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
  /**
   * 是否为新手引导的首行动（PRD ONB-02）。
   *
   * 为 true 时服务端强制三条约束：必须是一次性行动、必须给出预计时长、
   * 且时长落在 5—30 分钟内。普通行动留空即可。
   */
  first?: boolean;
}

/** 行动难度。 */
export type ActionDifficulty = 'EASY' | 'NORMAL' | 'HARD';

/** 单次调整类型。 */
export type ActionExceptionType = 'SKIP' | 'RESCHEDULE';

/** 行动定义视图。 */
export interface ActionResult {
  actionId: LongId;
  goalId: LongId;
  planVersionId: LongId;
  milestoneId: LongId | null;
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
  prerequisiteActionId: LongId | null;
  reminderPolicy: string | null;
  status: 'DRAFT' | 'ACTIVE' | 'CANCELLED';
  version: LongId;
}

/** 单次调整例外视图。 */
export interface ActionExceptionResult {
  exceptionId: LongId;
  actionId: LongId;
  /** 原计划日期。 */
  localDate: string;
  type: ActionExceptionType;
  /** 改期后的日期；跳过时为 null。 */
  rescheduledDate: string | null;
  reason: string | null;
}

/** 行动定义入参，新增与编辑共用；对应 ActionInput。 */
export interface ActionInput {
  title: string;
  description?: string | null;
  recurrenceType: RecurrenceType;
  weekdays?: string[] | null;
  intervalDays?: number | null;
  startDate: string;
  endDate?: string | null;
  localTime: string;
  endLocalTime?: string | null;
  timezone: string;
  priority?: PriorityLevel | null;
  difficulty?: ActionDifficulty | null;
  completionCriteria?: string | null;
  estimatedMinutes?: number | null;
  prerequisiteActionId?: LongId | null;
  reminderPolicy?: string | null;
}

/** 行动实例视图。 */
export interface OccurrenceResult {
  occurrenceId: LongId;
  actionId: LongId;
  actionTitle: string;
  scheduledAt: string;
  localDate: string;
  timezone: string;
  status: OccurrenceStatus;
}

/** 打卡结果视图；一并回显本次记录的内容，客户端不必再发一次查询。 */
export interface CheckInResult {
  checkInId: LongId;
  occurrenceId: LongId;
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
  checkInId: LongId;
  occurrenceId: LongId;
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

/** 复盘结果视图。 */
export interface ReviewResult {
  reviewId: LongId;
  goalId: LongId;
  periodKey: string;
  status: string;
  conclusionJson: string | null;
  completedAt: string | null;
  /** 生成复盘时的输入快照，用于展示当时的进度与打卡统计。 */
  inputSnapshotJson: string | null;
  createdAt: string | null;
}

/** 成就视图。 */
export interface AchievementResult {
  achievementId: LongId;
  goalId: LongId | null;
  type: AchievementType;
  title: string;
  description: string | null;
  achievedAt: string;
}

/** 通知渠道。 */
export type NotificationChannel = 'INBOX' | 'PUSH';

/** 专注会话状态。 */
export type FocusSessionStatus = 'RUNNING' | 'PAUSED' | 'FINISHED' | 'ABANDONED';

/** 专注会话视图。 */
export interface FocusSessionResult {
  sessionId: LongId;
  userId: LongId;
  occurrenceId: LongId | null;
  actionId: LongId | null;
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
  version: LongId;
}

/** 快速记录视图。 */
export interface QuickNoteResult {
  noteId: LongId;
  userId: LongId;
  content: string;
  moodLevel: MoodLevel | null;
  localDate: string;
  createdAt: string;
}

/**
 * 今日工作台里的一条行动。
 *
 * overdue 为 true 表示未执行且计划日期早于今天；unlocksOthers 是该行动作为多少个其他行动的前置，
 * 用于展示「为什么建议先做」。
 */
export interface TodayItem {
  occurrenceId: LongId;
  actionId: LongId;
  goalId: LongId;
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

/**
 * 今日工作台的提示。
 *
 * source 在接入真实模型前固定为 RULE：界面据此展示「规则提示」而不是「AI 建议」。
 */
export interface TodaySuggestion {
  type: string;
  source: 'RULE' | 'MODEL';
  title: string;
  basis: string;
  relatedOccurrenceId: LongId | null;
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

/** 站内消息视图。 */
export interface NotificationResult {
  id: LongId;
  type: string;
  resourceType: string;
  resourceId: string;
  summary: string;
  status: string;
  createdAt: string;
  readAt: string | null;
  cursor: LongId;
}

/** 通知偏好视图。 */
export interface NotificationPreferenceResult {
  userId: LongId;
  scene: string;
  channels: NotificationChannel[];
  quietStart: string | null;
  quietEnd: string | null;
  timezone: string;
  version: LongId;
}

/** 跨端增量变更项。 */
export interface SyncChangeResult {
  sequence: LongId;
  domain: string;
  resourceType: string;
  resourceId: string;
  resourceVersion: LongId;
  operation: string;
  snapshotJson: string | null;
  occurredAt: string;
}

/** 增量同步分页结果。 */
export interface SyncPageResult {
  changes: SyncChangeResult[];
  nextCursor: LongId;
  fullResyncRequired: boolean;
}

/** 离线命令提交结果。 */
export interface OfflineCommandResult {
  clientCommandId: string;
  status: string;
  resultJson: string | null;
  errorCode: string | null;
}

/** 日历绑定结果。 */
export interface CalendarBindingResult {
  bindingId: LongId;
  provider: string;
  status: string;
  version: LongId;
}

/** 年龄分层。 */
export type AgeBand = 'UNDER_14' | 'TEEN' | 'ADULT';

/** 账号状态。 */
export type AccountStatus =
  | 'PENDING_GUARDIAN'
  | 'ACTIVE_TEEN'
  | 'ACTIVE_ADULT'
  | 'RESTRICTED'
  | 'AGE_TRANSITION'
  | 'CLOSING'
  | 'CLOSED';

/** 会话令牌；access 15 分钟过期，refresh 30 天且刷新后轮换。 */
export interface SessionTokens {
  accessToken: string;
  refreshToken: string;
  sessionFamilyId: string;
  accessExpiresAt: string;
  refreshExpiresAt: string;
  ageBand: AgeBand;
  accountStatus: AccountStatus;
  authorizationVersion: LongId;
}

/** 注册结果，不包含令牌，需要随后登录。 */
export interface RegisteredUserResult {
  userId: LongId;
  publicId: string;
  ageBand: AgeBand;
  status: AccountStatus;
  authorizationVersion: LongId;
}

/** 隐私请求类型。 */
export type PrivacyRequestType = 'EXPORT' | 'CORRECTION' | 'DELETE_DATA' | 'CLOSE_ACCOUNT';

/** AI 沟通风格偏好（PRD ONB-01）。 */
export type CommunicationStyle = 'CONCISE' | 'GENTLE' | 'DIRECT' | 'COACHING';

/** AI 主动程度偏好（PRD ONB-01）；未选择时服务端按 MEDIUM 兜底。 */
export type ProactivityLevel = 'LOW' | 'MEDIUM' | 'HIGH';

/** 常见阻塞原因选项（PRD ONB-01）；整项可跳过，跳过时为空数组。 */
export type CommonBlocker =
  | 'TIME'
  | 'ENERGY'
  | 'CLARITY'
  | 'MOTIVATION'
  | 'ENVIRONMENT'
  | 'MOOD'
  | 'OTHER';

/**
 * 新手引导画像；所有字段可空。
 *
 * 空值表示「用户没有填写」，不是「服务端默认值」——默认值只在
 * `effectiveCommunicationStyle` / `effectiveProactivityLevel` 上体现。
 */
export interface OnboardingProfile {
  /** 用户自定义称呼（昵称），非实名。 */
  nickname: string | null;
  /** 通常入睡时刻（HH:mm:ss），本地时间。 */
  sleepTime: string | null;
  /** 通常起床时刻（HH:mm:ss），本地时间。 */
  wakeTime: string | null;
  /** 画像级别的每周可用时间（分钟）；目标级别有各自独立的同名字段。 */
  weeklyAvailableMinutes: number | null;
  remindWindowStart: string | null;
  remindWindowEnd: string | null;
  quietHoursStart: string | null;
  quietHoursEnd: string | null;
  communicationStyle: CommunicationStyle | null;
  proactivityLevel: ProactivityLevel | null;
  commonBlockers: CommonBlocker[];
}

/** 新手引导状态与画像读取结果，对应 OnboardingController 的两个接口。 */
export interface OnboardingProfileResult {
  /** 是否已经结束引导（含「跳过」）。 */
  completed: boolean;
  completedAt: string | null;
  profile: OnboardingProfile;
  /** 生效沟通风格（未选择时为 CONCISE）。 */
  effectiveCommunicationStyle: CommunicationStyle;
  /** 生效主动程度（未选择时为 MEDIUM）。 */
  effectiveProactivityLevel: ProactivityLevel;
  /** 账号当前版本号，回写画像时必须原样回传。 */
  version: LongId;
}

/** 隐私请求状态。 */
export type PrivacyRequestStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'WAITING_MANUAL'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

/** 隐私请求结果视图。 */
export interface PrivacyRequestResult {
  requestId: LongId;
  type: PrivacyRequestType;
  status: PrivacyRequestStatus;
  progress: number;
  deadline: string;
  resultReference: string | null;
}

/** 隐私导出包内容，7 天内有效。 */
export interface PrivacyExportResult {
  requestId: LongId;
  contentType: string;
  content: string;
  expiresAt: string;
}

/** 监护人权限类型。 */
export type GuardianPermissionType =
  | 'VIEW_GOAL_PROGRESS'
  | 'MANAGE_SCHEDULE'
  | 'RECEIVE_SAFETY_ALERTS';

/** 监护邀请结果；invitationToken 只在首次创建时返回。 */
export interface GuardianInvitationResult {
  relationId: LongId;
  invitationToken: string | null;
  status: string;
  expiresAt: string;
  permissions: GuardianPermissionType[];
}

/** 监护关系视图。 */
export interface GuardianRelationResult {
  relationId: LongId;
  teenUserId: LongId;
  guardianUserId: LongId | null;
  status: string;
  permissions: GuardianPermissionType[];
  effectiveAt: string | null;
}

/** 会话视图。 */
export interface ConversationResult {
  id: LongId;
  publicId: string;
  userId: LongId;
  scene: string;
  title: string;
  status: string;
  createdAt: string;
}

/** Agent 待确认提案。 */
export interface ProposalResult {
  id: LongId;
  publicId: string;
  toolName: string;
  riskLevel: string;
  argumentsJson: string;
  digest: string;
  expiresAt: string;
  status: string;
  version: LongId;
}

/** Agent 运行视图。 */
export interface AgentRunResult {
  id: LongId;
  publicId: string;
  conversationId: LongId;
  userId: LongId;
  scene: string;
  status: string;
  resultText: string | null;
  errorCode: string | null;
  version: LongId;
  proposal: ProposalResult | null;
  createdAt: string;
}

/** Agent 流式事件。 */
export interface AgentEventResult {
  eventId: LongId;
  eventType: string;
  safePayloadJson: string;
  createdAt: string;
}

/** 长期记忆视图。 */
export interface MemoryResult {
  id: LongId;
  userId: LongId;
  purpose: string;
  contentText: string;
  sourceRef: string;
  sensitivity: string;
  status: string;
  version: LongId;
  updatedAt: string;
}

/** 客户端兼容性策略。 */
export interface UpgradePolicy {
  upgradeAvailable: boolean;
  forceUpgrade: boolean;
  versionName: string;
  versionCode: LongId;
  minimumVersionCode: LongId;
  releaseNotes: string | null;
}

/** 合规文档条目。 */
export interface ComplianceDocument {
  documentType: string;
  versionNo: string;
  title: string;
  contentRef: string;
  contentDigest: string;
  effectiveAt: string;
}

/** PC Web 与 HarmonyOS 共用的运行时启动配置。 */
export interface ClientBootstrapResult {
  upgrade: UpgradePolicy;
  documents: ComplianceDocument[];
  featureFlags: Record<string, boolean>;
  experiments: string[];
  generatedAt: string;
}

/** 客服工单视图。 */
export interface SupportTicketResult {
  ticketId: LongId;
  ticketNo: string;
  userId: LongId;
  category: string;
  subject: string;
  status: string;
  priority: string;
  assigneeAdminId: LongId | null;
  version: LongId;
  createdAt: string;
}

/** 会员商品与价格版本，对应 ProductResult。 */
export interface ProductResult {
  productId: LongId;
  productKey: string;
  name: string;
  scene: string;
  priceId: LongId;
  priceVersion: number;
  amountMinor: LongId;
  currency: string;
  billingPeriod: string;
  agePolicy: string;
}

/** 订单视图，对应 OrderResult。 */
export interface OrderResult {
  orderId: LongId;
  orderNo: string;
  userId: LongId;
  productId: LongId;
  amountMinor: LongId;
  currency: string;
  status: string;
  paymentReference: string | null;
  refundedMinor: LongId;
  version: LongId;
  createdAt: string;
}

/** 订阅视图，对应 SubscriptionResult。 */
export interface SubscriptionResult {
  subscriptionId: LongId;
  userId: LongId;
  productId: LongId;
  status: string;
  periodEnd: string | null;
  cancelMode: string | null;
  version: LongId;
}

/** 权益余额视图，对应 EntitlementResult。 */
export interface EntitlementResult {
  userId: LongId;
  resourceKey: string;
  balance: LongId;
  expiresAt: string | null;
  version: LongId;
}

/** 私有文件视图，对应 FileResult。 */
export interface FileResult {
  fileId: LongId;
  publicId: string;
  ownerUserId: LongId;
  purpose: string;
  originalName: string;
  sizeBytes: LongId;
  mimeType: string;
  contentHash: string;
  sensitivity: string;
  status: string;
  scanResult: string | null;
  version: LongId;
}

/** 内容模板版本视图，对应 TemplateVersionResult。 */
export interface TemplateVersionResult {
  versionId: LongId;
  templateId: LongId;
  versionNo: number;
  ageScope: string;
  contentSnapshot: string;
  status: string;
  reviewerUserId: LongId | null;
  reviewReason: string | null;
  publishedAt: string | null;
  version: LongId;
}

/** 导入导出任务视图，对应 TransferJobResult。 */
export interface TransferJobResult {
  jobId: LongId;
  type: string;
  userId: LongId;
  status: string;
  previewJson: string | null;
  errorJson: string | null;
  resultFileId: LongId | null;
  expiresAt: string | null;
  version: LongId;
}

/** 伙伴关系视图，对应 PartnerRelationResult。 */
export interface PartnerRelationResult {
  relationId: LongId;
  inviterUserId: LongId;
  inviteeUserId: LongId;
  status: string;
  version: LongId;
}

/** 伙伴授权权限，对应 PartnerPermission。 */
export type PartnerPermission = 'VIEW_PROGRESS' | 'ENCOURAGE' | 'COMMENT' | 'CO_CHECK_IN';

/** 伙伴目标授权视图，对应 PartnerGrantResult。 */
export interface PartnerGrantResult {
  grantId: LongId;
  relationId: LongId;
  ownerUserId: LongId;
  goalId: LongId;
  permissions: PartnerPermission[];
  expiresAt: string | null;
  status: string;
  version: LongId;
}

/** 受控分享链接视图；rawToken 只在创建时返回一次，列表查询为 null。 */
export interface ShareLinkResult {
  shareId: LongId;
  rawToken: string | null;
  resourceType: string;
  resourceId: string;
  fields: string[];
  snapshotJson: string | null;
  status: string;
  visitCount: number;
  visitLimit: number | null;
  expiresAt: string | null;
  version: LongId;
}
