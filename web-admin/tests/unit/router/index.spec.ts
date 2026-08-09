import { describe, it, expect, beforeEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { router } from '@/router';
import { useAdminAuthStore } from '@/stores/adminAuth';

describe('router guards', () => {
  beforeEach(async () => {
    setActivePinia(createPinia());
    localStorage.clear();
    await router.push('/');
  });

  it('未登录访问首页被重定向到登录页', async () => {
    const store = useAdminAuthStore();
    expect(store.isAuthenticated).toBe(false);

    await router.push('/');
    expect(router.currentRoute.value.path).toBe('/login');
  });

  it('已登录访问登录页被重定向到首页', async () => {
    const store = useAdminAuthStore();
    store.token = 'token';
    expect(store.isAuthenticated).toBe(true);

    await router.push('/login');
    expect(router.currentRoute.value.path).toBe('/');
  });
});
