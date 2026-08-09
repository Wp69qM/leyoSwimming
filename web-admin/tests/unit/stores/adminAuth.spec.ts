import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAdminAuthStore } from '@/stores/adminAuth';
import * as adminAuthApi from '@/api/adminAuth';
import type { AdminInfo, ApiResponse, AdminLoginResponse } from '@/types/api';

vi.mock('@/api/adminAuth');

describe('useAdminAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    vi.resetAllMocks();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('初始状态为未登录', () => {
    const store = useAdminAuthStore();
    expect(store.isAuthenticated).toBe(false);
    expect(store.admin).toBeNull();
  });

  it('登录成功时保存 token 与管理员信息', async () => {
    const admin: AdminInfo = { id: 1, username: 'admin', name: '系统管理员', role: 'super_admin' };
    const response: ApiResponse<AdminLoginResponse> = {
      code: 0,
      message: 'success',
      data: { token: 'fake-token', expiresIn: 86400, admin },
    };
    vi.mocked(adminAuthApi.adminLogin).mockResolvedValue(response);

    const store = useAdminAuthStore();
    const result = await store.login({ username: 'admin', password: 'admin123' });

    expect(result.success).toBe(true);
    expect(store.isAuthenticated).toBe(true);
    expect(store.admin).toEqual(admin);
    expect(store.token).toBe('fake-token');
    expect(localStorage.getItem('admin_token')).toBe('fake-token');
  });

  it('登录失败时返回错误信息且不改变认证状态', async () => {
    vi.mocked(adminAuthApi.adminLogin).mockRejectedValue(new Error('用户名或密码错误'));

    const store = useAdminAuthStore();
    const result = await store.login({ username: 'admin', password: 'wrong' });

    expect(result.success).toBe(false);
    expect(result.message).toBe('用户名或密码错误');
    expect(store.isAuthenticated).toBe(false);
  });

  it('退出登录时清除本地状态并调用接口', async () => {
    const admin: AdminInfo = { id: 1, username: 'admin', name: '系统管理员', role: 'super_admin' };
    localStorage.setItem('admin_token', 'fake-token');
    localStorage.setItem('admin_info', JSON.stringify(admin));
    vi.mocked(adminAuthApi.adminLogout).mockResolvedValue({ code: 0, message: 'success', data: null });

    const store = useAdminAuthStore();
    await store.logout();

    expect(store.isAuthenticated).toBe(false);
    expect(store.admin).toBeNull();
    expect(localStorage.getItem('admin_token')).toBeNull();
    expect(adminAuthApi.adminLogout).toHaveBeenCalled();
  });
});
