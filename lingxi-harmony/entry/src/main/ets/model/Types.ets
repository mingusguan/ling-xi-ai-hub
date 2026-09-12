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

export type RecurrenceType = 'ONCE' | 'DAILY' | 'WEEKLY';

export type OccurrenceStatus = 'SCHEDULED' | 'COMPLETED' | 'PARTIAL' | 'SKIPPED' | 'MISSED';

export type CheckInResultType = 'COMPLETED' | 'PARTIAL' | 'SKIPPED';

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

export interface GoalResult {
  goalId: number;
  publicId: string;
  userId: number;
  title: string;
  successCriteria: string;
  status: GoalStatus;
  currentPlanVersionId: number | null;
  progress: number;
  version: number;
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
  recurrenceType: RecurrenceType;
  weekdays: string[];
  startDate: string;
  endDate: string | null;
  localTime: string;
  timezone: string;
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
  newAchievements: AchievementResult[];
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
