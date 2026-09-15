import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type {
  CommonBlocker,
  CommunicationStyle,
  OnboardingProfile,
  OnboardingProfileResult,
  ProactivityLevel
} from '@lingxi/api-client';
import { api } from '@/api/client';

/** 尚未填写过的空画像；与服务端「用户没填过」的表示保持一致。 */
export function emptyProfile(): OnboardingProfile {
  return {
    nickname: null,
    sleepTime: null,
    wakeTime: null,
    weeklyAvailableMinutes: null,
    remindWindowStart: null,
    remindWindowEnd: null,
    quietHoursStart: null,
    quietHoursEnd: null,
    communicationStyle: null,
    proactivityLevel: null,
    commonBlockers: []
  };
}

/**
 * 新手引导基础画像状态（PRD 8.2 ONB-01）。
 *
 * 画像全部字段可空，提交时把空字符串统一转成 null：界面上的空输入框表达的是
 * 「用户没填」，不是「填了一个空字符串」，服务端也因此不会被写入任何兜底默认值。
 */
export const useOnboardingStore = defineStore('onboarding', () => {
  const profile = ref<OnboardingProfile>(emptyProfile());
  const completed = ref(false);
  const completedAt = ref<string | null>(null);
  const effectiveCommunicationStyle = ref<CommunicationStyle>('CONCISE');
  const effectiveProactivityLevel = ref<ProactivityLevel>('MEDIUM');
  const version = ref<string>('0');
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref<string | null>(null);

  /** 是否至少填过一项；用于区分「跳过」和「认真填过」。 */
  const hasAnyInput = computed(() => {
    const p = profile.value;
    return (
      p.nickname !== null ||
      p.sleepTime !== null ||
      p.wakeTime !== null ||
      p.weeklyAvailableMinutes !== null ||
      p.remindWindowStart !== null ||
      p.quietHoursStart !== null ||
      p.communicationStyle !== null ||
      p.proactivityLevel !== null ||
      p.commonBlockers.length > 0
    );
  });

  function apply(result: OnboardingProfileResult): void {
    profile.value = {
      ...result.profile,
      commonBlockers: [...result.profile.commonBlockers]
    };
    completed.value = result.completed;
    completedAt.value = result.completedAt;
    effectiveCommunicationStyle.value = result.effectiveCommunicationStyle;
    effectiveProactivityLevel.value = result.effectiveProactivityLevel;
    version.value = result.version;
  }

  async function load(): Promise<void> {
    loading.value = true;
    errorMessage.value = null;
    try {
      apply(await api.identity.getOnboardingProfile());
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '读取引导信息失败';
      throw error;
    } finally {
      loading.value = false;
    }
  }

  /** 覆盖写入画像；complete 为 true 表示用户点了「完成」或「跳过」。 */
  async function save(complete: boolean): Promise<void> {
    saving.value = true;
    errorMessage.value = null;
    try {
      const p = profile.value;
      apply(
        await api.identity.saveOnboardingProfile({
          nickname: normalize(p.nickname),
          sleepTime: normalize(p.sleepTime),
          wakeTime: normalize(p.wakeTime),
          weeklyAvailableMinutes: p.weeklyAvailableMinutes,
          remindWindowStart: normalize(p.remindWindowStart),
          remindWindowEnd: normalize(p.remindWindowEnd),
          quietHoursStart: normalize(p.quietHoursStart),
          quietHoursEnd: normalize(p.quietHoursEnd),
          communicationStyle: p.communicationStyle,
          proactivityLevel: p.proactivityLevel,
          commonBlockers: [...p.commonBlockers],
          complete,
          expectedVersion: version.value
        })
      );
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '保存引导信息失败';
      throw error;
    } finally {
      saving.value = false;
    }
  }

  /** 重新进入引导（设置页主动重做）；保留已填画像。 */
  async function reopen(): Promise<void> {
    saving.value = true;
    errorMessage.value = null;
    try {
      apply(await api.identity.reopenOnboarding(version.value));
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '重新进入引导失败';
      throw error;
    } finally {
      saving.value = false;
    }
  }

  function toggleBlocker(value: CommonBlocker): void {
    const current = profile.value.commonBlockers;
    profile.value.commonBlockers = current.includes(value)
      ? current.filter((item) => item !== value)
      : [...current, value];
  }

  return {
    profile,
    completed,
    completedAt,
    effectiveCommunicationStyle,
    effectiveProactivityLevel,
    version,
    loading,
    saving,
    errorMessage,
    hasAnyInput,
    load,
    save,
    reopen,
    toggleBlocker
  };
});

function normalize(value: string | null): string | null {
  if (value === null) {
    return null;
  }
  const trimmed = value.trim();
  return trimmed === '' ? null : trimmed;
}
