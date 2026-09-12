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
const showAssertionLogin = ref(false);
const assertion = ref('');

const providerLabel = computed(() =>
  isManualAssertionMode ? '本地联调：手工粘贴断言' : `身份桥接层：${assertionProvider.name}`
);

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
    const message = error instanceof Error ? error.message : '登录失败';
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
    localError.value = error instanceof Error ? error.message : '断言登录失败';
  }
}
</script>

<template>
  <div class="login">
    <div class="lx-card login__card">
      <h1>灵犀伴行</h1>
      <p class="lx-muted">
        使用手机号登录；首次登录会自动创建账号，需要填写出生日期（服务端强制 14 周岁以上准入）。
      </p>

      <div class="lx-field">
        <label>手机号</label>
        <input v-model="phoneNumber" class="lx-input" inputmode="numeric" placeholder="例如 13800000000" />
      </div>
      <div class="lx-field">
        <label>出生日期</label>
        <input v-model="birthDate" class="lx-input" type="date" />
        <small class="lx-muted">已注册过的手机号可以留空；14—17 周岁账号将进入待监护状态。</small>
      </div>

      <p v-if="localError || session.errorMessage" class="lx-error">
        {{ localError ?? session.errorMessage }}
      </p>

      <button class="lx-button login__submit" :disabled="session.busy" @click="loginByPhone">
        {{ session.busy ? '处理中…' : '登录 / 注册' }}
      </button>

      <details class="login__advanced" @toggle="showAssertionLogin = !showAssertionLogin">
        <summary>使用身份断言登录（身份桥接层联调）</summary>
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
          :disabled="session.busy || !showAssertionLogin"
          @click="loginByAssertion"
        >
          断言登录
        </button>
      </details>
    </div>
  </div>
</template>

<style scoped>
.login {
  align-items: center;
  display: flex;
  justify-content: center;
  min-height: 100vh;
  padding: var(--lx-space-5);
}

.login__card {
  max-width: 420px;
  width: 100%;
}

.login__submit {
  width: 100%;
}

.login__advanced {
  margin-top: var(--lx-space-4);
}

.login__advanced summary {
  color: var(--lx-color-text-muted);
  cursor: pointer;
  font-size: 13px;
}
</style>
