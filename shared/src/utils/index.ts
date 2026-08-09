/**
 * 纯函数工具集合（禁止引入 UI / 后端框架依赖）。
 */

export function formatPhone(phone: string): string {
  if (phone.length !== 11) return phone;
  return `${phone.slice(0, 3)} **** ${phone.slice(-4)}`;
}

export function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0;
}
