import Taro from '@tarojs/taro';

import { useAuthStore, calculateExpiresAt } from './authStore';

jest.mock('@tarojs/taro', () => ({
  getStorageSync: jest.fn(),
  setStorageSync: jest.fn(),
  removeStorageSync: jest.fn(),
  clearStorageSync: jest.fn(),
}));

describe('authStore', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useAuthStore.setState({
      accessToken: null,
      refreshToken: null,
      tokenExpiresAt: null,
      userInfo: null,
      isLoggedIn: false,
    });
  });

  describe('calculateExpiresAt', () => {
    test('calculates expiration timestamp from seconds', () => {
      const before = Date.now();
      const result = calculateExpiresAt(3600);
      const after = Date.now();
      expect(result).toBeGreaterThanOrEqual(before + 3600 * 1000);
      expect(result).toBeLessThanOrEqual(after + 3600 * 1000);
    });
  });

  describe('login', () => {
    test('updates state and persists tokens and minimal userInfo to storage', () => {
      const userInfo = {
        userId: 'u1',
        phone: '13800138000',
        profileCompleted: false,
      };
      useAuthStore.getState().login('access', 'refresh', 7200, userInfo);

      const state = useAuthStore.getState();
      expect(state.accessToken).toBe('access');
      expect(state.refreshToken).toBe('refresh');
      expect(state.userInfo).toEqual(userInfo);
      expect(state.isLoggedIn).toBe(true);
      expect(state.tokenExpiresAt).not.toBeNull();

      expect(Taro.setStorageSync).toHaveBeenCalledWith(
        'access_token',
        'access'
      );
      expect(Taro.setStorageSync).toHaveBeenCalledWith(
        'refresh_token',
        'refresh'
      );
      expect(Taro.setStorageSync).toHaveBeenCalledWith(
        'token_expires_at',
        expect.any(Number)
      );
      expect(Taro.setStorageSync).toHaveBeenCalledWith('user_info', {
        userId: 'u1',
        profileCompleted: false,
      });
    });
  });

  describe('setTokens', () => {
    test('updates only token fields', () => {
      useAuthStore.getState().setTokens('a', 'r', 3600);

      const state = useAuthStore.getState();
      expect(state.accessToken).toBe('a');
      expect(state.refreshToken).toBe('r');
      expect(state.isLoggedIn).toBe(false);
    });
  });

  describe('setUserInfo', () => {
    test('updates userInfo field and persists minimal fields', () => {
      const userInfo = {
        userId: 'u2',
        phone: '13900139000',
        profileCompleted: true,
      };
      useAuthStore.getState().setUserInfo(userInfo);
      expect(useAuthStore.getState().userInfo).toEqual(userInfo);
      expect(Taro.setStorageSync).toHaveBeenCalledWith('user_info', {
        userId: 'u2',
        profileCompleted: true,
      });
    });

    test('removes persisted userInfo when set to null', () => {
      useAuthStore.getState().setUserInfo(null);
      expect(useAuthStore.getState().userInfo).toBeNull();
      expect(Taro.removeStorageSync).toHaveBeenCalledWith('user_info');
    });
  });

  describe('logout', () => {
    test('clears state and storage', () => {
      useAuthStore
        .getState()
        .login('a', 'r', 7200, { userId: 'u1', profileCompleted: false });
      useAuthStore.getState().logout();

      const state = useAuthStore.getState();
      expect(state.accessToken).toBeNull();
      expect(state.refreshToken).toBeNull();
      expect(state.tokenExpiresAt).toBeNull();
      expect(state.userInfo).toBeNull();
      expect(state.isLoggedIn).toBe(false);

      expect(Taro.removeStorageSync).toHaveBeenCalledWith('access_token');
      expect(Taro.removeStorageSync).toHaveBeenCalledWith('refresh_token');
      expect(Taro.removeStorageSync).toHaveBeenCalledWith('token_expires_at');
      expect(Taro.removeStorageSync).toHaveBeenCalledWith('user_info');
    });
  });

  describe('clear', () => {
    test('behaves like logout', () => {
      useAuthStore
        .getState()
        .login('a', 'r', 7200, { userId: 'u1', profileCompleted: false });
      useAuthStore.getState().clear();

      const state = useAuthStore.getState();
      expect(state.isLoggedIn).toBe(false);
      expect(state.accessToken).toBeNull();
      expect(state.userInfo).toBeNull();
      expect(Taro.removeStorageSync).toHaveBeenCalledWith('user_info');
    });
  });

  describe('restoreFromStorage', () => {
    test('restores logged-in state when token is valid', () => {
      (Taro.getStorageSync as jest.Mock).mockImplementation((key: string) => {
        if (key === 'access_token') return 'stored_access';
        if (key === 'refresh_token') return 'stored_refresh';
        if (key === 'token_expires_at') return Date.now() + 100000;
        if (key === 'user_info') return { userId: 'u1', profileCompleted: true };
        return null;
      });

      useAuthStore.getState().restoreFromStorage();

      const state = useAuthStore.getState();
      expect(state.accessToken).toBe('stored_access');
      expect(state.refreshToken).toBe('stored_refresh');
      expect(state.userInfo).toEqual({ userId: 'u1', profileCompleted: true });
      expect(state.isLoggedIn).toBe(true);
    });

    test('sets logged-out state when token is expired', () => {
      (Taro.getStorageSync as jest.Mock).mockImplementation((key: string) => {
        if (key === 'access_token') return 'stored_access';
        if (key === 'token_expires_at') return Date.now() - 1000;
        return null;
      });

      useAuthStore.getState().restoreFromStorage();
      expect(useAuthStore.getState().isLoggedIn).toBe(false);
    });

    test('sets logged-out state when no token', () => {
      (Taro.getStorageSync as jest.Mock).mockReturnValue(null);
      useAuthStore.getState().restoreFromStorage();
      expect(useAuthStore.getState().isLoggedIn).toBe(false);
    });
  });
});
