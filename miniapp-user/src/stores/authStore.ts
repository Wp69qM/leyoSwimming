import { create } from 'zustand';
import {
  STORAGE_KEYS,
  getStorageItem,
  removeStorageItem,
  setStorageItem,
} from '@/utils/storage';

export interface UserInfo {
  userId: string;
  phone?: string;
  avatarUrl?: string;
  nickName?: string;
  profileCompleted: boolean;
}

/** 仅持久化启动/路由判定所需的最小字段，避免在本地存放 PII。 */
type PersistedUserInfo = Pick<UserInfo, 'userId' | 'profileCompleted'>;

export interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  tokenExpiresAt: number | null;
  userInfo: UserInfo | null;
  isLoggedIn: boolean;
  setTokens: (
    accessToken: string,
    refreshToken: string,
    expiresIn: number
  ) => void;
  setUserInfo: (userInfo: UserInfo | null) => void;
  login: (
    accessToken: string,
    refreshToken: string,
    expiresIn: number,
    userInfo: UserInfo
  ) => void;
  logout: () => void;
  clear: () => void;
  restoreFromStorage: () => void;
}

export function calculateExpiresAt(expiresIn: number): number {
  return Date.now() + expiresIn * 1000;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  tokenExpiresAt: null,
  userInfo: null,
  isLoggedIn: false,

  setTokens: (accessToken, refreshToken, expiresIn) => {
    const tokenExpiresAt = calculateExpiresAt(expiresIn);
    setStorageItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
    setStorageItem(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
    setStorageItem(STORAGE_KEYS.TOKEN_EXPIRES_AT, tokenExpiresAt);
    set({ accessToken, refreshToken, tokenExpiresAt });
  },

  setUserInfo: (userInfo) => {
    if (userInfo) {
      const persisted: PersistedUserInfo = {
        userId: userInfo.userId,
        profileCompleted: userInfo.profileCompleted,
      };
      setStorageItem(STORAGE_KEYS.USER_INFO, persisted);
    } else {
      removeStorageItem(STORAGE_KEYS.USER_INFO);
    }
    set({ userInfo });
  },

  login: (accessToken, refreshToken, expiresIn, userInfo) => {
    const tokenExpiresAt = calculateExpiresAt(expiresIn);
    const persisted: PersistedUserInfo = {
      userId: userInfo.userId,
      profileCompleted: userInfo.profileCompleted,
    };
    setStorageItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
    setStorageItem(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
    setStorageItem(STORAGE_KEYS.TOKEN_EXPIRES_AT, tokenExpiresAt);
    setStorageItem(STORAGE_KEYS.USER_INFO, persisted);
    set({
      accessToken,
      refreshToken,
      tokenExpiresAt,
      userInfo,
      isLoggedIn: true,
    });
  },

  logout: () => {
    removeStorageItem(STORAGE_KEYS.ACCESS_TOKEN);
    removeStorageItem(STORAGE_KEYS.REFRESH_TOKEN);
    removeStorageItem(STORAGE_KEYS.TOKEN_EXPIRES_AT);
    removeStorageItem(STORAGE_KEYS.USER_INFO);
    set({
      accessToken: null,
      refreshToken: null,
      tokenExpiresAt: null,
      userInfo: null,
      isLoggedIn: false,
    });
  },

  clear: () => {
    removeStorageItem(STORAGE_KEYS.ACCESS_TOKEN);
    removeStorageItem(STORAGE_KEYS.REFRESH_TOKEN);
    removeStorageItem(STORAGE_KEYS.TOKEN_EXPIRES_AT);
    removeStorageItem(STORAGE_KEYS.USER_INFO);
    set({
      accessToken: null,
      refreshToken: null,
      tokenExpiresAt: null,
      userInfo: null,
      isLoggedIn: false,
    });
  },

  restoreFromStorage: () => {
    const accessToken = getStorageItem<string>(STORAGE_KEYS.ACCESS_TOKEN);
    const refreshToken = getStorageItem<string>(STORAGE_KEYS.REFRESH_TOKEN);
    const tokenExpiresAt = getStorageItem<number>(
      STORAGE_KEYS.TOKEN_EXPIRES_AT
    );
    const userInfo = getStorageItem<PersistedUserInfo>(STORAGE_KEYS.USER_INFO);
    const isLoggedIn = Boolean(
      accessToken && tokenExpiresAt && Date.now() < tokenExpiresAt
    );
    set({ accessToken, refreshToken, tokenExpiresAt, userInfo, isLoggedIn });
  },
}));
