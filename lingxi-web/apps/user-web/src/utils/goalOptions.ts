import type {
  ActionDifficulty,
  EnergyLevel,
  GoalPrivacyLevel,
  GoalType,
  MoodLevel,
  PriorityLevel
} from '@lingxi/api-client';

/**
 * 目标与行动定义相关的选项文案。
 *
 * 目标类型取值与 PRD「支持的目标类型」十类一一对应；优先级与难度都参与后端统计与排序，
 * 因此界面必须使用固定选项，不能退化成自由文本输入。
 */

/** 目标类型选项，顺序与 PRD 表格一致。 */
export const GOAL_TYPE_OPTIONS: ReadonlyArray<{ value: GoalType; label: string }> = [
  { value: 'LEARNING_EXAM', label: '学习考试' },
  { value: 'CAREER', label: '职业发展' },
  { value: 'CREATIVE', label: '创作输出' },
  { value: 'READING', label: '阅读与知识' },
  { value: 'HABIT', label: '习惯养成' },
  { value: 'FITNESS', label: '健身与作息' },
  { value: 'TRAVEL', label: '旅行与活动' },
  { value: 'COMMUNICATION', label: '人际沟通' },
  { value: 'BUDGET', label: '个人预算' },
  { value: 'EMOTION', label: '情绪记录' }
];

/** 目标优先级选项。 */
export const PRIORITY_OPTIONS: ReadonlyArray<{ value: PriorityLevel; label: string }> = [
  { value: 'LOW', label: '低' },
  { value: 'NORMAL', label: '常规' },
  { value: 'HIGH', label: '高' }
];

/** 行动难度选项；主观难度与计划难度共用同一把尺子，便于计算偏差。 */
export const ACTION_DIFFICULTY_OPTIONS: ReadonlyArray<{ value: ActionDifficulty; label: string }> = [
  { value: 'EASY', label: '轻松' },
  { value: 'NORMAL', label: '一般' },
  { value: 'HARD', label: '困难' }
];

/** 打卡精力自评选项。 */
export const ENERGY_OPTIONS: ReadonlyArray<{ value: EnergyLevel; label: string }> = [
  { value: 'LOW', label: '精力偏低' },
  { value: 'NORMAL', label: '一般' },
  { value: 'HIGH', label: '精力充沛' }
];

/**
 * 打卡情绪自评选项，五档中性量表。
 *
 * 文案刻意保持中性描述，不出现任何诊断性措辞：这一项只用于观察趋势与调整节奏。
 */
export const MOOD_OPTIONS: ReadonlyArray<{ value: MoodLevel; label: string }> = [
  { value: 'VERY_LOW', label: '明显低落' },
  { value: 'LOW', label: '偏低' },
  { value: 'NEUTRAL', label: '平稳' },
  { value: 'GOOD', label: '不错' },
  { value: 'VERY_GOOD', label: '很好' }
];

/**
 * 目标隐私级别选项。
 *
 * 「允许对伙伴可见」只是可见前提：真正让伙伴看到还需要按目标的最小化授权，
 * 该选项不会自动授权给任何人。
 */
export const GOAL_PRIVACY_OPTIONS: ReadonlyArray<{ value: GoalPrivacyLevel; label: string }> = [
  { value: 'PRIVATE', label: '仅自己（默认）' },
  { value: 'PARTNER_VISIBLE', label: '允许对伙伴可见（仍需按目标授权）' }
];
