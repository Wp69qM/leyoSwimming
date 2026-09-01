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

function generateUUIDFallback(): string {
  // v4 UUID fallback for non-secure contexts
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
    const rand = Math.random() * 16;
    const value =
      char === 'x' ? Math.floor(rand) : (Math.floor(rand) & 0x3) | 0x8;
    return value.toString(16);
  });
}

export { generateUUIDFallback };

export function generateUUID(): string {
  if (
    typeof globalThis !== 'undefined' &&
    typeof globalThis.crypto?.randomUUID === 'function'
  ) {
    return globalThis.crypto.randomUUID();
  }

  return generateUUIDFallback();
}
