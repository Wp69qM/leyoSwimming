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
    expiresInSeconds: number
  ) => void;
  setUserInfo: (userInfo: UserInfo | null) => void;
  login: (
    accessToken: string,
    refreshToken: string,
    expiresInSeconds: number,
    userInfo: UserInfo
  ) => void;
  logout: () => void;
  clear: () => void;
  restoreFromStorage: () => void;
}

export function calculateExpiresAt(expiresInSeconds: number): number {
  return Date.now() + expiresInSeconds * 1000;
}

function loadAuthStateFromStorage(): Pick<
  AuthState,
  'accessToken' | 'refreshToken' | 'tokenExpiresAt' | 'userInfo' | 'isLoggedIn'
> {
  const accessToken = getStorageItem<string>(STORAGE_KEYS.ACCESS_TOKEN);
  const refreshToken = getStorageItem<string>(STORAGE_KEYS.REFRESH_TOKEN);
  let tokenExpiresAt = getStorageItem<number>(STORAGE_KEYS.TOKEN_EXPIRES_AT);
  const userInfo = getStorageItem<PersistedUserInfo>(STORAGE_KEYS.USER_INFO);
  // 兼容旧版本只存了 accessToken、没有存过期时间的情况
  if (accessToken && !tokenExpiresAt) {
    tokenExpiresAt = Date.now() + 7 * 24 * 60 * 60 * 1000;
    setStorageItem(STORAGE_KEYS.TOKEN_EXPIRES_AT, tokenExpiresAt);
  }
  const isLoggedIn = Boolean(
    accessToken && tokenExpiresAt && Date.now() < tokenExpiresAt
  );
  return { accessToken, refreshToken, tokenExpiresAt, userInfo, isLoggedIn };
}

export const useAuthStore = create<AuthState>((set) => ({
  ...loadAuthStateFromStorage(),

  setTokens: (accessToken, refreshToken, expiresInSeconds) => {
    const tokenExpiresAt = calculateExpiresAt(expiresInSeconds);
    setStorageItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
    setStorageItem(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
    setStorageItem(STORAGE_KEYS.TOKEN_EXPIRES_AT, tokenExpiresAt);
    set({ accessToken, refreshToken, tokenExpiresAt, isLoggedIn: true });
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

  login: (accessToken, refreshToken, expiresInSeconds, userInfo) => {
    const tokenExpiresAt = calculateExpiresAt(expiresInSeconds);
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
    set(loadAuthStateFromStorage());
  },
}));
