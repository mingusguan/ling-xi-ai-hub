import type { RecurrenceType } from '@lingxi/api-client';

/**
 * 行动重复规则的唯一前端实现。
 *
 * 与服务端 `com.lingxi.goal.domain.Action` 的校验保持一致：WEEKLY 必须给出星期集合，
 * INTERVAL 必须给出范围内的间隔天数。PC Web 与后续鸿蒙端都必须复用这里的规则，
 * 避免再次出现「界面给了选项、提交时字段恒为空」的历史缺陷。
 */

/** 星期取值与后端 java.time.DayOfWeek 名称一致，顺序固定为周一到周日。 */
export const WEEKDAY_OPTIONS = [
  { value: 'MONDAY', label: '一' },
  { value: 'TUESDAY', label: '二' },
  { value: 'WEDNESDAY', label: '三' },
  { value: 'THURSDAY', label: '四' },
  { value: 'FRIDAY', label: '五' },
  { value: 'SATURDAY', label: '六' },
  { value: 'SUNDAY', label: '日' }
] as const;

/** 五种重复形式，文案与 PRD「行动与任务」一致。 */
export const RECURRENCE_OPTIONS: ReadonlyArray<{ value: RecurrenceType; label: string }> = [
  { value: 'ONCE', label: '单次' },
  { value: 'DAILY', label: '每天' },
  { value: 'WEEKLY', label: '每周（自选星期）' },
  { value: 'WEEKDAYS', label: '工作日（周一至周五）' },
  { value: 'INTERVAL', label: '间隔重复' }
];

/** 间隔天数范围，与服务端 Action 的 MIN/MAX_INTERVAL_DAYS 对齐。 */
export const MIN_INTERVAL_DAYS = 1;
export const MAX_INTERVAL_DAYS = 365;

/** 重复规则的表单形态。 */
export interface RecurrenceForm {
  recurrenceType: RecurrenceType;
  weekdays: string[];
  intervalDays: number | null;
}

/** 重复规则在提交给服务端时的形状。 */
export interface NormalizedRecurrence {
  recurrenceType: RecurrenceType;
  weekdays: string[];
  intervalDays: number | null;
}

/** 校验重复规则，返回可直接展示给用户的中文提示；合法时返回 null。 */
export function validateRecurrence(form: RecurrenceForm): string | null {
  if (form.recurrenceType === 'WEEKLY' && form.weekdays.length === 0) {
    return '选择「每周（自选星期）」时必须至少勾选一个星期';
  }
  if (form.recurrenceType === 'INTERVAL') {
    const days = form.intervalDays;
    if (days === null || Number.isNaN(days)) {
      return '选择「间隔重复」时必须填写间隔天数';
    }
    if (!Number.isInteger(days) || days < MIN_INTERVAL_DAYS || days > MAX_INTERVAL_DAYS) {
      return `间隔天数必须是 ${MIN_INTERVAL_DAYS} 到 ${MAX_INTERVAL_DAYS} 之间的整数`;
    }
  }
  return null;
}

/** 整理重复规则：非当前形式的多余字段一律置空，保证前后端只有一套事实。 */
export function normalizeRecurrence(form: RecurrenceForm): NormalizedRecurrence {
  return {
    recurrenceType: form.recurrenceType,
    weekdays: form.recurrenceType === 'WEEKLY' ? [...form.weekdays] : [],
    intervalDays: form.recurrenceType === 'INTERVAL' ? form.intervalDays : null
  };
}

/** 重复规则的中文摘要，用于表单回显与列表展示。 */
export function recurrenceLabel(form: RecurrenceForm): string {
  switch (form.recurrenceType) {
    case 'ONCE':
      return '单次';
    case 'DAILY':
      return '每天';
    case 'WEEKDAYS':
      return '工作日';
    case 'INTERVAL':
      return `每 ${form.intervalDays ?? '?'} 天`;
    case 'WEEKLY': {
      if (form.weekdays.length === 0) {
        return '每周（未选星期）';
      }
      const order: readonly string[] = WEEKDAY_OPTIONS.map((item) => item.value);
      const labels = [...form.weekdays]
        .sort((a, b) => order.indexOf(a) - order.indexOf(b))
        .map((value) => WEEKDAY_OPTIONS.find((item) => item.value === value)?.label ?? value);
      return `每周 ${labels.join('、')}`;
    }
    default:
      return String(form.recurrenceType);
  }
}
