import Taro from '@tarojs/taro';

export function getApiBaseUrl(): string {
  if (Taro.getEnv() === Taro.ENV_TYPE.WEB) {
    return '';
  }
  const envValue =
    typeof process !== 'undefined' &&
    process.env &&
    process.env.TARO_APP_API_BASE_URL
      ? process.env.TARO_APP_API_BASE_URL
      : '';
  return envValue || 'http://localhost:8080';
}
