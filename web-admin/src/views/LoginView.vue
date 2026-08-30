<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAdminAuthStore } from '@/stores/adminAuth';

const router = useRouter();
const route = useRoute();
const authStore = useAdminAuthStore();

const form = reactive({
  username: '',
  password: '',
});
const serverError = ref('');
const tokenExpiredVisible = ref(false);
const fieldErrors = reactive({
  username: '',
  password: '',
});

watch(
  () => route.query.expired,
  (expired) => {
    tokenExpiredVisible.value = expired === '1';
  },
  { immediate: true }
);

function clearErrors() {
  serverError.value = '';
  tokenExpiredVisible.value = false;
  fieldErrors.username = '';
  fieldErrors.password = '';
}

function validateForm(): boolean {
  fieldErrors.username = '';
  fieldErrors.password = '';

  if (!form.username.trim()) {
    fieldErrors.username = '请输入用户名';
  }
  if (!form.password) {
    fieldErrors.password = '请输入密码';
  }

  return !fieldErrors.username && !fieldErrors.password;
}

const isDisabled = computed(() => authStore.isLoggingIn);

async function handleLogin() {
  if (authStore.isLoggingIn) return;
  clearErrors();

  if (!validateForm()) return;

  const result = await authStore.login({
    username: form.username,
    password: form.password,
  });

  if (result.success) {
    ElMessage.success('登录成功');
    await router.push((route.query.redirect as string) || '/');
  } else {
    serverError.value = result.message || '登录失败';
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div v-if="tokenExpiredVisible" class="token-expired-tip">
        <svg
          class="tip-icon"
          viewBox="0 0 24 24"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <path
            d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 15c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm1-4h-2V7h2v6z"
            fill="currentColor"
          />
        </svg>
        <span>登录信息已过期，请重新登录</span>
      </div>

      <div class="card-header">
        <div class="logo">
          <svg
            class="logo-icon"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"
              fill="currentColor"
            />
          </svg>
        </div>
        <h1 class="system-name">leyoSwimming 管理后台</h1>
        <p class="subtitle">管理员登录</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-field">
          <label class="field-label">用户名</label>
          <div
            class="input-wrapper"
            :class="{
              'is-error':
                (serverError || fieldErrors.username) && !authStore.isLoggingIn,
            }"
          >
            <svg
              class="input-prefix"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
                fill="currentColor"
              />
            </svg>
            <input
              v-model="form.username"
              type="text"
              class="custom-input"
              placeholder="请输入管理员账号"
              :disabled="isDisabled"
              @input="clearErrors"
            />
          </div>
          <div v-if="fieldErrors.username" class="field-error">
            {{ fieldErrors.username }}
          </div>
        </div>

        <div class="form-field">
          <label class="field-label">密码</label>
          <div
            class="input-wrapper"
            :class="{
              'is-error':
                (serverError || fieldErrors.password) && !authStore.isLoggingIn,
            }"
          >
            <svg
              class="input-prefix"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"
                fill="currentColor"
              />
            </svg>
            <input
              v-model="form.password"
              type="password"
              class="custom-input"
              placeholder="请输入密码"
              :disabled="isDisabled"
              @input="clearErrors"
            />
            <svg
              class="input-suffix"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5C21.27 7.61 17 4.5 12 4.5zm0 12.5c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"
                fill="currentColor"
              />
            </svg>
          </div>
          <div v-if="fieldErrors.password" class="field-error">
            {{ fieldErrors.password }}
          </div>
        </div>

        <div v-if="serverError" class="error-message">
          <svg
            class="error-icon"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"
              fill="currentColor"
            />
          </svg>
          <span>{{ serverError }}</span>
        </div>

        <button
          type="submit"
          class="login-button"
          :class="{ 'has-error-above': serverError }"
          :disabled="isDisabled"
        >
          <svg
            v-if="authStore.isLoggingIn"
            class="loading-icon"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z"
              fill="currentColor"
            />
          </svg>
          <span>{{ authStore.isLoggingIn ? '登录中...' : '登 录' }}</span>
        </button>
      </form>
    </div>

    <footer class="copyright">© 2026 leyoSwimming. All rights reserved.</footer>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  padding: 24px;
  overflow: hidden;
  box-sizing: border-box;
  background: linear-gradient(180deg, #e6f7ff 0%, #ffffff 100%);
}

.login-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 480px;
  padding: 48px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  box-sizing: border-box;
}

.token-expired-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 384px;
  padding: 10px 12px;
  margin-bottom: 24px;
  background: #fffbe6;
  border: 0.7px solid #ffe58f;
  border-radius: 8px;
  box-sizing: border-box;
  font-size: 12px;
  line-height: 1.5;
  color: #262626;

  .tip-icon {
    flex-shrink: 0;
    width: 18px;
    height: 18px;
    color: #faad14;
  }
}

.card-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 100%);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.35);
}

.logo-icon {
  width: 32px;
  height: 32px;
  color: #ffffff;
}

.system-name {
  margin: 16px 0 0;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.33;
  color: #262626;
}

.subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.43;
  color: #8c8c8c;
}

.login-form {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  width: 100%;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.43;
  color: #262626;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 384px;
  height: 44px;
  padding: 0 12px;
  background: #ffffff;
  border: 0.7px solid #d9d9d9;
  border-radius: 8px;
  box-sizing: border-box;
  transition: border-color 0.2s ease;

  &.is-error {
    border-color: #ff4d4f;
  }

  &:focus-within:not(.is-error) {
    border-color: #1890ff;
  }
}

.input-prefix,
.input-suffix {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  color: #bfbfbf;
}

.custom-input {
  flex: 1;
  width: 0;
  height: 100%;
  padding: 0;
  font-size: 14px;
  line-height: 1.43;
  color: #262626;
  background: transparent;
  border: none;
  outline: none;

  &::placeholder {
    color: #bfbfbf;
  }

  &:disabled {
    background: transparent;
    cursor: not-allowed;
  }
}

.field-error {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #ff4d4f;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 384px;
  margin-top: -12px;
  font-size: 12px;
  line-height: 1.5;
  color: #ff4d4f;
  box-sizing: border-box;
}

.error-icon {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
}

.login-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 384px;
  height: 44px;
  margin-top: 20px;
  padding: 0;
  font-size: 16px;
  font-weight: 500;
  line-height: 1.5;
  color: #ffffff;
  cursor: pointer;
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 100%);
  border: none;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.35);
  box-sizing: border-box;
  transition: opacity 0.2s ease;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.7;
  }

  &.has-error-above {
    margin-top: 12px;
  }
}

.loading-icon {
  width: 20px;
  height: 20px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.copyright {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  line-height: 1.5;
  color: #8c8c8c;
  text-align: center;
}
</style>
