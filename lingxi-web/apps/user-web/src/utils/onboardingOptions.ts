import type { CommonBlocker, CommunicationStyle, ProactivityLevel } from '@lingxi/api-client';

/**
 * 新手引导基础画像的选项文案（PRD 8.2 ONB-01）。
 *
 * 全部选项都是固定枚举：沟通风格与主动程度会直接改变伙伴的语气与主动提醒强度，
 * 阻塞原因会参与复盘归因，都不适合退化成自由文本。
 */

/** AI 沟通风格选项。 */
export const COMMUNICATION_STYLE_OPTIONS: ReadonlyArray<{
  value: CommunicationStyle;
  label: string;
  hint: string;
}> = [
  { value: 'CONCISE', label: '简洁', hint: '直接给结论和下一步，少铺垫' },
  { value: 'GENTLE', label: '温和', hint: '先接住情绪，再给建议' },
  { value: 'DIRECT', label: '直接', hint: '就事论事，不做情绪缓冲' },
  { value: 'COACHING', label: '教练式', hint: '用提问帮你自己得出结论' }
];

/** AI 主动程度选项；默认「中」。 */
export const PROACTIVITY_OPTIONS: ReadonlyArray<{
  value: ProactivityLevel;
  label: string;
  hint: string;
}> = [
  { value: 'LOW', label: '低', hint: '只在你想聊的时候出现' },
  { value: 'MEDIUM', label: '中（默认）', hint: '按行动提醒主动跟进' },
  { value: 'HIGH', label: '高', hint: '在免打扰时段之外更主动地陪伴与复盘' }
];

/** 常见阻塞原因选项；整项可跳过。 */
export const COMMON_BLOCKER_OPTIONS: ReadonlyArray<{ value: CommonBlocker; label: string }> = [
  { value: 'TIME', label: '时间不够' },
  { value: 'ENERGY', label: '精力不足' },
  { value: 'CLARITY', label: '不知道具体做什么' },
  { value: 'MOTIVATION', label: '知道怎么做但不想开始' },
  { value: 'ENVIRONMENT', label: '缺少场地或设备' },
  { value: 'MOOD', label: '情绪状态影响执行' },
  { value: 'OTHER', label: '其他' }
];
