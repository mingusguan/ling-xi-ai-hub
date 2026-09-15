<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { StarterGoalTemplate } from '@lingxi/api-client';
import { api } from '@/api/client';
import { useOnboardingStore } from '@/stores/onboarding';
import { useGoalStore } from '@/stores/goal';
import {
  COMMON_BLOCKER_OPTIONS,
  COMMUNICATION_STYLE_OPTIONS,
  PROACTIVITY_OPTIONS
} from '@/utils/onboardingOptions';

/**
 * 新手引导：ONB-01 基础画像（三步）+ ONB-02 首目标与首行动（第四步）。
 *
 * PRD 要求每一步都可留空、可跳过，且禁止强制收集真实姓名、身份证、职业单位、精确位置
 * 等核心功能非必需的信息，因此界面上没有任何必填项，只有「完成」和「跳过」两个出口。
 * 「跳过」同样会把引导标记为已结束，否则用户每次进入都会被拦住。
 *
 * 第四步的模板入口是 PRD ONB-02 的三种入口之一（文字与语音由后续迭代补），
 * 语音依赖 ASR，平台侧未接入。
 */
const route = useRoute();
const router = useRouter();
const onboarding = useOnboardingStore();
const goalStore = useGoalStore();

const step = ref(1);
const submitted = ref(false);

/** 总步数与每步标题；前 3 步对应 ONB-01，第 4 步对应 ONB-02。 */
const totalSteps = 4;
const stepTitles = ['怎么称呼你', '你的节奏', '希望我怎么陪你', '先做一件小事'];

const templates = ref<StarterGoalTemplate[]>([]);
const selectedTemplateKey = ref<string | null>(null);
const selectedActionIndex = ref(0);
const templateLoading = ref(false);
const templateError = ref<string | null>(null);

const errorMessage = computed(() => onboarding.errorMessage ?? templateError.value);

const selectedTemplate = computed(
  () => templates.value.find((item) => item.templateKey === selectedTemplateKey.value) ?? null
);

const selectedAction = computed(
  () => selectedTemplate.value?.firstActions[selectedActionIndex.value] ?? null
);

onMounted(async () => {
  try {
    await onboarding.load();
    if (onboarding.completed) {
      if (route.query.intent === 'redo') {
        await onboarding.reopen();
      } else {
        await router.replace({ name: 'home' });
        return;
      }
    }
  } catch {
    // 读取失败时保留在页面上展示错误，不阻塞用户重试。
  }
  await loadTemplates();
});

/** 读取模板目录；失败不阻塞用户，仍可走「跳过引导」。 */
async function loadTemplates(): Promise<void> {
  templateLoading.value = true;
  templateError.value = null;
  try {
    templates.value = await api.goals.listStarterTemplates();
  } catch (error) {
    templateError.value = error instanceof Error ? error.message : '读取入门模板失败';
  } finally {
    templateLoading.value = false;
  }
}

function next(): void {
  if (step.value < totalSteps) {
    step.value += 1;
  }
}

function back(): void {
  if (step.value > 1) {
    step.value -= 1;
  }
}

/** 完成或跳过都写同一次请求，区别只在 complete 恒为 true 与是否有画像内容。 */
async function finish(): Promise<void> {
  submitted.value = true;
  try {
    await onboarding.save(true);
    await router.replace({ name: 'home' });
  } catch {
    submitted.value = false;
  }
}

/** 跳过引导：不替用户填任何选项，只把引导标记为已结束。 */
async function skip(): Promise<void> {
  await finish();
}

/**
 * 用模板创建目标并生成首行动，然后结束引导。
 *
 * 顺序必须是「建目标 → 确认计划 → 结束引导」：首行动是通过确认计划写入的，
 * 而 PRD 要求「创建成功后直接生成一个 5—30 分钟的首行动」，
 * 所以不能只建目标就当作引导完成。
 */
async function createFromTemplate(): Promise<void> {
  const template = selectedTemplate.value;
  const action = selectedAction.value;
  if (!template || !action) {
    return;
  }
  submitted.value = true;
  templateError.value = null;
  try {
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai';
    const today = new Date().toISOString().slice(0, 10);
    const goal = await goalStore.createGoal({
      title: template.name,
      description: template.summary,
      successCriteria: template.defaultSuccessCriteria ?? '完成这个目标',
      goalType: template.goalType,
      startDate: today,
      priority: 'NORMAL',
      privacyLevel: 'PRIVATE'
    });
    if (!goal) {
      throw new Error('目标创建失败，请稍后重试');
    }
    // 首行动必须是一次性行动且时长在 5—30 分钟之间，服务端会强制校验。
    const confirmed = await goalStore.confirmPlan(
      goal,
      [],
      [
        {
          clientKey: 'first-action',
          milestoneSequence: null,
          title: action.title,
          description: template.summary,
          recurrenceType: 'ONCE',
          weekdays: [],
          intervalDays: null,
          startDate: today,
          endDate: null,
          localTime: '20:00',
          endLocalTime: null,
          timezone,
          priority: action.priority,
          difficulty: action.difficulty,
          completionCriteria: action.completionCriteria,
          estimatedMinutes: action.estimatedMinutes,
          prerequisiteClientKey: null,
          reminderPolicy: null,
          first: true
        }
      ],
      null
    );
    if (!confirmed) {
      throw new Error('首行动创建失败，请稍后重试');
    }
    await api.goals.advanceClarification(confirmed.goalId, 'COMPLETE', confirmed.version);
    // 用户已经建好目标和首行动，引导就算走完了；这里必须传 true，
    // 否则下次进入还会被引导页拦住，看起来像「刚才做的没被记住」。
    await onboarding.save(true);
    await router.replace({ name: 'home' });
  } catch (error) {
    templateError.value = error instanceof Error ? error.message : '创建首目标失败';
    submitted.value = false;
  }
}

function minutesHint(): string {
  const value = onboarding.profile.weeklyAvailableMinutes;
  if (value === null || Number.isNaN(value)) {
    return '留空也可以，之后在目标里还能单独设置每个目标的可用时间。';
  }
  return `每周约 ${(value / 60).toFixed(1)} 小时，用来判断计划排得下多少事。`;
}
</script>

<template>
  <section class="lx-card onboarding">
    <header class="onboarding__head">
      <h2>先认识一下</h2>
      <p class="lx-muted">
        这一步全部可以留空，也可以直接跳过；引导只影响伙伴的语气与提醒方式，
        不影响任何功能是否可用。我们不会要求真实姓名、身份证、单位或精确位置。
      </p>
    </header>

    <ol class="steps">
      <li v-for="(title, index) in stepTitles" :key="title" :class="['step', { 'step--on': step === index + 1 }]">
        <span class="step__no">{{ index + 1 }}</span>
        <span class="step__title">{{ title }}</span>
      </li>
    </ol>

    <!-- 第一步：称呼。昵称即可，「跳过」不写入任何默认值。 -->
    <div v-if="step === 1" class="lx-field">
      <label for="onb-nickname">怎么称呼你</label>
      <input
        id="onb-nickname"
        v-model="onboarding.profile.nickname"
        class="lx-input"
        maxlength="24"
        placeholder="昵称即可，例如「小灵」；留空就按「你」称呼"
      />
      <small class="lx-muted">仅用于伙伴称呼你，不要求实名。</small>
    </div>

    <!-- 第二步：作息与每周可用时间。 -->
    <template v-if="step === 2">
      <div class="lx-row onb-row">
        <div class="lx-field">
          <label for="onb-sleep">通常几点睡</label>
          <input id="onb-sleep" v-model="onboarding.profile.sleepTime" class="lx-input" type="time" />
        </div>
        <div class="lx-field">
          <label for="onb-wake">通常几点起</label>
          <input id="onb-wake" v-model="onboarding.profile.wakeTime" class="lx-input" type="time" />
        </div>
      </div>
      <div class="lx-field">
        <label for="onb-weekly">每周大概有多少可用时间（分钟）</label>
        <input
          id="onb-weekly"
          v-model.number="onboarding.profile.weeklyAvailableMinutes"
          class="lx-input"
          type="number"
          min="1"
          placeholder="例如 600"
        />
        <small class="lx-muted">{{ minutesHint() }}</small>
      </div>
    </template>

    <!-- 第三步：提醒时段、免打扰时段、沟通风格、主动程度与常见阻塞。 -->
    <template v-if="step === 3">
      <div class="lx-row onb-row">
        <div class="lx-field">
          <label for="onb-remind-start">偏好提醒时段</label>
          <div class="lx-row">
            <input id="onb-remind-start" v-model="onboarding.profile.remindWindowStart" class="lx-input" type="time" />
            <span class="lx-muted">到</span>
            <input v-model="onboarding.profile.remindWindowEnd" class="lx-input" type="time" />
          </div>
          <small class="lx-muted">伙伴会尽量在这个时段内找你；两项必须同时填写。</small>
        </div>
      </div>

      <div class="lx-field">
        <label for="onb-quiet-start">免打扰时段</label>
        <div class="lx-row">
          <input id="onb-quiet-start" v-model="onboarding.profile.quietHoursStart" class="lx-input" type="time" />
          <span class="lx-muted">到</span>
          <input v-model="onboarding.profile.quietHoursEnd" class="lx-input" type="time" />
        </div>
        <small class="lx-muted">支持跨天，例如 23:00 到次日 07:00；这段时间不会有任何主动打扰。</small>
      </div>

      <div class="lx-field">
        <label>希望我用什么方式说话</label>
        <div class="option-grid">
          <button
            v-for="item in COMMUNICATION_STYLE_OPTIONS"
            :key="item.value"
            type="button"
            :class="['lx-button', 'lx-button--ghost', 'option', { 'option--on': onboarding.profile.communicationStyle === item.value }]"
            @click="onboarding.profile.communicationStyle = onboarding.profile.communicationStyle === item.value ? null : item.value"
          >
            <strong>{{ item.label }}</strong>
            <span class="lx-muted">{{ item.hint }}</span>
          </button>
        </div>
      </div>

      <div class="lx-field">
        <label>希望你多主动</label>
        <div class="option-grid">
          <button
            v-for="item in PROACTIVITY_OPTIONS"
            :key="item.value"
            type="button"
            :class="['lx-button', 'lx-button--ghost', 'option', { 'option--on': onboarding.profile.proactivityLevel === item.value }]"
            @click="onboarding.profile.proactivityLevel = onboarding.profile.proactivityLevel === item.value ? null : item.value"
          >
            <strong>{{ item.label }}</strong>
            <span class="lx-muted">{{ item.hint }}</span>
          </button>
        </div>
      </div>

      <div class="lx-field">
        <label>你平时最容易卡在哪（可多选，也可不选）</label>
        <div class="blockers">
          <button
            v-for="item in COMMON_BLOCKER_OPTIONS"
            :key="item.value"
            type="button"
            :class="['lx-button', 'lx-button--ghost', onboarding.profile.commonBlockers.includes(item.value) ? 'blocker--on' : '']"
            @click="onboarding.toggleBlocker(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
        <small class="lx-muted">选择后，复盘会优先按这些原因帮你定位阻塞，而不是笼统地说「没坚持住」。</small>
      </div>
    </template>

    <p v-if="errorMessage" class="lx-error">{{ errorMessage }}</p>

    <!-- 第四步：首目标与首行动（PRD ONB-02）。模板入口，创建后立刻生成 5—30 分钟的首行动。 -->
    <template v-if="step === 4">
      <p class="lx-muted">
        选一个和你最接近的模板，我会直接给你一个 5—30 分钟就能做完的第一件事。
        做完它就等于目标已经开始了；也可以直接跳过，之后在「我的目标」里自己编排。
      </p>

      <div v-if="templateLoading" class="lx-empty">正在读取模板…</div>
      <div v-else-if="templates.length === 0" class="lx-empty">
        暂时没有可用的入门模板，可以直接跳过引导，在「我的目标」里手动创建。
      </div>

      <div v-else class="template-grid">
        <button
          v-for="item in templates"
          :key="item.templateKey"
          type="button"
          :class="['lx-button', 'lx-button--ghost', 'option', { 'option--on': selectedTemplateKey === item.templateKey }]"
          @click="selectedTemplateKey = selectedTemplateKey === item.templateKey ? null : item.templateKey; selectedActionIndex = 0"
        >
          <strong>{{ item.name }}</strong>
          <span class="lx-muted">{{ item.summary }}</span>
        </button>
      </div>

      <div v-if="selectedTemplate" class="lx-field first-action">
        <label>先做哪件小事</label>
        <div class="lx-row action-picks">
          <button
            v-for="(action, index) in selectedTemplate.firstActions"
            :key="action.title"
            type="button"
            :class="['lx-button', 'lx-button--ghost', selectedActionIndex === index ? 'blocker--on' : '']"
            @click="selectedActionIndex = index"
          >
            {{ action.title }} · {{ action.estimatedMinutes }} 分钟
          </button>
        </div>
        <small v-if="selectedAction" class="lx-muted">
          完成标准：{{ selectedAction.completionCriteria ?? '做完这一件事' }}
        </small>
      </div>
    </template>

    <footer class="onboarding__foot">
      <div class="lx-row">
        <button v-if="step > 1" class="lx-button lx-button--ghost" :disabled="onboarding.saving" @click="back">
          上一步
        </button>
        <button v-if="step < totalSteps" class="lx-button" :disabled="onboarding.saving" @click="next">
          下一步
        </button>
        <button
          v-else
          class="lx-button"
          :disabled="onboarding.saving || submitted || !selectedTemplate || !selectedAction"
          @click="createFromTemplate"
        >
          就从这个开始
        </button>
        <button class="lx-button lx-button--ghost" :disabled="onboarding.saving || submitted" @click="skip">
          跳过引导
        </button>
      </div>
      <small class="lx-muted">
        「跳过」只会把引导标记为已完成，不会替你填任何选项；之后随时可以从侧边栏「账号 → 新手引导」重做。
      </small>
    </footer>
  </section>
</template>

<style scoped>
.onboarding {
  max-width: 720px;
}

.onboarding__head h2 {
  margin-bottom: var(--lx-space-2);
}

.steps {
  display: flex;
  gap: var(--lx-space-3);
  list-style: none;
  margin: var(--lx-space-4) 0;
  padding: 0;
}

.step {
  align-items: center;
  color: var(--lx-color-text-muted);
  display: flex;
  flex: 1;
  gap: var(--lx-space-2);
}

.step--on {
  color: var(--lx-color-text);
  font-weight: 600;
}

.step__no {
  align-items: center;
  border: 1px solid var(--lx-color-border);
  border-radius: 50%;
  display: inline-flex;
  height: 22px;
  justify-content: center;
  width: 22px;
}

.step--on .step__no {
  background: var(--lx-color-primary);
  border-color: var(--lx-color-primary);
  color: #fff;
}

.onb-row {
  align-items: flex-start;
  flex-wrap: wrap;
  gap: var(--lx-space-3);
}

.option-grid {
  display: grid;
  gap: var(--lx-space-2);
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

.option {
  align-items: flex-start;
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: left;
  white-space: normal;
}

.option--on,
.blocker--on {
  border-color: var(--lx-color-primary);
  box-shadow: inset 0 0 0 1px var(--lx-color-primary);
}

.blockers {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lx-space-2);
}

.template-grid {
  display: grid;
  gap: var(--lx-space-2);
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  margin: var(--lx-space-4) 0;
}

.first-action {
  border-top: 1px solid var(--lx-color-border);
  padding-top: var(--lx-space-4);
}

.action-picks {
  flex-wrap: wrap;
}

.onboarding__foot {
  border-top: 1px solid var(--lx-color-border);
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-2);
  margin-top: var(--lx-space-4);
  padding-top: var(--lx-space-4);
}
</style>
