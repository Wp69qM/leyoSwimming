import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { adminLogin, adminLogout } from '@/api/adminAuth';
import type { AdminInfo, AdminLoginRequest } from '@/types/api';

const TOKEN_KEY = 'admin_token';
const ADMIN_KEY = 'admin_info';

export const useAdminAuthStore = defineStore('adminAuth', () => {
  function loadToken(): string | null {
    const value = localStorage.getItem(TOKEN_KEY);
    return value && value !== 'undefined' && value !== 'null' ? value : null;
  }

  const token = ref<string | null>(loadToken());
  const admin = ref<AdminInfo | null>(loadAdminInfo());
  const isLoggingIn = ref(false);

  const isAuthenticated = computed(() => !!token.value);

  function loadAdminInfo(): AdminInfo | null {
    try {
      const raw = localStorage.getItem(ADMIN_KEY);
      if (!raw || raw === 'undefined' || raw === 'null') return null;
      return JSON.parse(raw) as AdminInfo;
    } catch {
      return null;
    }
  }

  function setAuth(newToken: string, newAdmin: AdminInfo) {
    token.value = newToken;
    admin.value = newAdmin;
    localStorage.setItem(TOKEN_KEY, newToken);
    localStorage.setItem(ADMIN_KEY, JSON.stringify(newAdmin));
  }

  function clearAuth() {
    token.value = null;
    admin.value = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ADMIN_KEY);
  }

  async function login(data: AdminLoginRequest) {
    isLoggingIn.value = true;
    try {
      const response = await adminLogin(data);
      const { token: newToken, admin: newAdmin } = response.data!;
      setAuth(newToken, newAdmin);
      return { success: true };
    } catch (error) {
      return {
        success: false,
        message: error instanceof Error ? error.message : '登录失败',
      };
    } finally {
      isLoggingIn.value = false;
    }
  }

  async function logout() {
    try {
      await adminLogout();
    } finally {
      clearAuth();
    }
  }

  return {
    token,
    admin,
    isLoggingIn,
    isAuthenticated,
    login,
    logout,
    clearAuth,
  };
});
