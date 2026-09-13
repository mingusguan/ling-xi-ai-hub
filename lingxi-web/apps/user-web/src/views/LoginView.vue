<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useSessionStore } from '@/stores/session';
import { assertionProvider, isManualAssertionMode, manualAssertionProvider } from '@/api/assertion';

const session = useSessionStore();
const router = useRouter();
const route = useRoute();

const phoneNumber = ref('');
const birthDate = ref('');
const localError = ref<string | null>(null);
/** 断言登录仅保留给本地联调与后续真实身份桥接层使用。 */
const assertion = ref('');

const providerLabel = computed(() =>
  isManualAssertionMode ? '本地联调：手工粘贴断言' : `身份桥接层：${assertionProvider.name}`
);

/** 错误提示统一走这里：把可能的 HTML 响应体贴成可读文案，避免把 nginx 502 页面原样糊到界面上。 */
function readableError(error: unknown, fallback: string): string {
  const raw = error instanceof Error ? error.message : fallback;
  if (typeof raw !== 'string' || raw.trim() === '') {
    return fallback;
  }
  if (/<\s*(!doctype|html|head|body|center|title)\b/i.test(raw)) {
    const status = raw.match(/(\d{3})\s+([A-Za-z ]+)/);
    if (status) {
      return `服务暂时不可用（${status[1]} ${status[2].trim()}），请稍后重试`;
    }
    return '服务返回了无法识别的响应，请稍后重试';
  }
  return raw.length > 200 ? `${raw.slice(0, 200)}…` : raw;
}

async function loginByPhone(): Promise<void> {
  localError.value = null;
  const phone = phoneNumber.value.trim();
  if (!/^1[3-9]\d{9}$/.test(phone.replace(/[\s-]/g, ''))) {
    localError.value = '请输入有效的中国大陆手机号';
    return;
  }
  try {
    await session.loginWithPhone(phone, birthDate.value === '' ? null : birthDate.value);
    const redirect = (route.query.redirect as string | undefined) ?? '/';
    await router.replace(redirect);
  } catch (error) {
    const message = readableError(error, '登录失败');
    localError.value = message.includes('出生日期')
      ? '该手机号首次登录，请填写出生日期（14 周岁以下无法注册）'
      : message;
  }
}

async function loginByAssertion(): Promise<void> {
  localError.value = null;
  try {
    if (isManualAssertionMode) {
      manualAssertionProvider.submit(assertion.value);
    }
    await session.login({ channel: 'SMS', identifier: phoneNumber.value, requireBirthDate: false });
    await router.replace((route.query.redirect as string | undefined) ?? '/');
  } catch (error) {
    localError.value = readableError(error, '断言登录失败');
  }
}
</script>

<template>
  <div class="login">
    <!-- 左侧品牌区：桌面端展示，窄屏隐藏 -->
    <section class="login__hero">
      <div class="login__hero-inner">
        <div class="login__logo">
          <span class="login__logo-mark">灵</span>
          <span class="login__logo-text">灵犀伴行</span>
        </div>
        <h1 class="login__hero-title">把目标拆成<br />今天就能做到的事</h1>
        <p class="login__hero-desc">
          目标规划、行动提醒、周期复盘与成就记录，由一个懂你节奏的伙伴陪着走完。
        </p>
        <ul class="login__points">
          <li><i>✓</i><span>行动计划按你的时区滚动生成，不会漏掉任何一天</span></li>
          <li><i>✓</i><span>青少年账号默认进入待监护状态，家长可随时查看授权范围</span></li>
          <li><i>✓</i><span>私密对话与目标正文默认不对外可见，导出需二次确认</span></li>
        </ul>
      </div>
    </section>

    <!-- 右侧表单区 -->
    <section class="login__panel">
      <div class="lx-card login__card">
        <header class="login__head">
          <p class="login__eyebrow">LINGXI COMPANION</p>
          <h2>登录 / 注册</h2>
          <p class="lx-muted login__intro">
            手机号首次登录会自动创建账号，需填写出生日期（服务端强制 14 周岁以上准入）。
          </p>
        </header>

        <form class="login__form" @submit.prevent="loginByPhone">
          <div class="lx-field">
            <label for="lx-phone">手机号</label>
            <input
              id="lx-phone"
              v-model="phoneNumber"
              class="lx-input"
              inputmode="numeric"
              autocomplete="tel"
              placeholder="例如 13800000000"
            />
          </div>

          <div class="lx-field">
            <label for="lx-birth">出生日期<span class="login__optional">（已注册可留空）</span></label>
            <input id="lx-birth" v-model="birthDate" class="lx-input" type="date" />
            <small class="lx-muted">14—17 周岁账号将进入待监护状态，需监护人授权后解锁核心功能。</small>
          </div>

          <p v-if="localError || session.errorMessage" class="lx-error" role="alert">
            {{ localError ?? session.errorMessage }}
          </p>

          <button class="lx-button login__submit" type="submit" :disabled="session.busy">
            {{ session.busy ? '处理中…' : '登录 / 注册' }}
          </button>
        </form>

        <details class="login__advanced">
          <summary>使用身份断言登录（身份桥接层联调）</summary>
          <div class="login__advanced-body">
            <p class="lx-muted">{{ providerLabel }}</p>
            <textarea
              v-if="isManualAssertionMode"
              v-model="assertion"
              class="lx-textarea"
              rows="3"
              placeholder="base64url(payload).base64url(hmacSha256(payload))"
            />
            <button
              class="lx-button lx-button--ghost login__submit"
              type="button"
              :disabled="session.busy || !isManualAssertionMode"
              @click="loginByAssertion"
            >
              断言登录
            </button>
          </div>
        </details>
      </div>
      <p class="login__foot lx-muted">Copyright © 2026 灵犀伴行. All Rights Reserved.</p>
    </section>
  </div>
</template>

<style scoped>
.login {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  min-height: 100vh;
}

/* --------------------------------------------------------------------------
   左侧品牌区
   -------------------------------------------------------------------------- */
.login__hero {
  display: flex;
  align-items: center;
  padding: 64px 72px;
  border-right: 1px solid var(--lx-color-border);
  background:
    radial-gradient(ellipse 620px 420px at 18% 12%, rgba(52, 192, 141, 0.2), transparent 66%),
    linear-gradient(160deg, rgba(22, 30, 46, 0.9) 0%, rgba(11, 18, 32, 0.95) 100%);
}

.login__hero-inner {
  max-width: 520px;
}

.login__logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
}

.login__logo-mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 13px;
  background: linear-gradient(135deg, #34C08D 0%, #1E9E73 100%);
  color: #07130E;
  font-size: 21px;
  font-weight: 700;
  box-shadow: 0 8px 22px rgba(52, 192, 141, 0.34);
}

.login__logo-text {
  color: var(--lx-color-text);
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 0.6px;
}

.login__hero-title {
  margin: 0 0 18px;
  font-size: 40px;
  font-weight: 700;
  line-height: 1.28;
  letter-spacing: 0.4px;
}

.login__hero-desc {
  margin: 0 0 34px;
  max-width: 460px;
  color: var(--lx-color-text-muted);
  font-size: 15px;
  line-height: 1.8;
}

.login__points {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.login__points li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  color: var(--lx-color-text);
  font-size: 14.5px;
  line-height: 1.6;
}

.login__points i {
  flex-shrink: 0;
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  margin-top: 2px;
  border-radius: 50%;
  background: var(--lx-color-primary-soft);
  border: 1px solid var(--lx-color-primary-border);
  color: var(--lx-color-primary-text);
  font-size: 12px;
  font-style: normal;
}

/* --------------------------------------------------------------------------
   右侧表单区
   -------------------------------------------------------------------------- */
.login__panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding: 48px 40px;
}

.login__card {
  width: 100%;
  max-width: 412px;
  box-shadow: var(--lx-shadow-raised);
}

.login__head {
  margin-bottom: 24px;
}

.login__eyebrow {
  margin: 0 0 8px;
  color: var(--lx-color-primary-text);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2px;
}

.login__head h2 {
  margin: 0 0 10px;
  font-size: 25px;
  font-weight: 700;
}

.login__intro {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.7;
}

.login__form {
  margin: 0;
}

.login__optional {
  color: #8496AD;
  font-weight: 400;
}

.login__submit {
  width: 100%;
  margin-top: 4px;
  padding: 11px 18px;
  font-size: 15px;
}

.login__advanced {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--lx-color-border);
}

.login__advanced summary {
  color: var(--lx-color-text-muted);
  cursor: pointer;
  font-size: 13px;
  list-style: none;
}

.login__advanced summary::-webkit-details-marker {
  display: none;
}

.login__advanced summary::before {
  content: '▸';
  display: inline-block;
  margin-right: 8px;
  color: var(--lx-color-primary-text);
  transition: transform 0.15s ease;
}

.login__advanced[open] summary::before {
  transform: rotate(90deg);
}

.login__advanced-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.login__advanced-body .lx-button {
  margin-top: 0;
}

.login__foot {
  margin: 0;
  font-size: 12.5px;
}

/* 窄屏：隐藏品牌区，表单占满 */
@media (max-width: 980px) {
  .login {
    grid-template-columns: 1fr;
  }

  .login__hero {
    display: none;
  }

  .login__panel {
    padding: 40px 20px;
  }
}
</style>
