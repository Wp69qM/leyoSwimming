import Taro from '@tarojs/taro';

export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'access_token',
  REFRESH_TOKEN: 'refresh_token',
  TOKEN_EXPIRES_AT: 'token_expires_at',
} as const;

export function getStorageItem<T>(key: string): T | null {
  try {
    return Taro.getStorageSync<T>(key);
  } catch {
    return null;
  }
}

export function setStorageItem<T>(key: string, value: T): void {
  Taro.setStorageSync(key, value);
}

export function removeStorageItem(key: string): void {
  Taro.removeStorageSync(key);
}

export function clearStorage(): void {
  Taro.clearStorageSync();
}
