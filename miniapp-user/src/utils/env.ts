export function getApiBaseUrl(): string {
  const envValue =
    typeof process !== 'undefined' &&
    process.env &&
    process.env.TARO_APP_API_BASE_URL
      ? process.env.TARO_APP_API_BASE_URL
      : '';
  return envValue || 'http://localhost:8080';
}
