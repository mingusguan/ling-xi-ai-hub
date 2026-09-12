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

/** 行动重复类型。 */
export type RecurrenceType = 'ONCE' | 'DAILY' | 'WEEKLY';

/** 行动实例状态。 */
export type OccurrenceStatus = 'SCHEDULED' | 'COMPLETED' | 'PARTIAL' | 'SKIPPED' | 'MISSED';

/** 打卡结果。 */
export type CheckInResultType = 'COMPLETED' | 'PARTIAL' | 'SKIPPED';

/** 成就类型。 */
export type AchievementType = 'GOAL_COMPLETED' | 'MILESTONE_COMPLETED' | 'CHECK_IN_STREAK';

/** 目标查询与写入结果。 */
export interface GoalResult {
  goalId: LongId;
  publicId: string;
  userId: LongId;
  title: string;
  successCriteria: string;
  status: GoalStatus;
  currentPlanVersionId: LongId | null;
  /** 进度为 int，服务端仍以数字输出。 */
  progress: number;
  version: LongId;
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
  recurrenceType: RecurrenceType;
  weekdays: string[];
  startDate: string;
  endDate: string | null;
  localTime: string;
  timezone: string;
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

/** 打卡结果视图。 */
export interface CheckInResult {
  checkInId: LongId;
  occurrenceId: LongId;
  result: CheckInResultType;
  occurrenceStatus: OccurrenceStatus;
  goalProgress: number;
  recordedAt: string;
  newAchievements: AchievementResult[];
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
