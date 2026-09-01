import { generateUUIDFallback } from '@leyo/shared';

/**
 * 为不支持 crypto.randomUUID 的运行时（如旧版微信小程序基础库、
 * 非安全上下文的浏览器等）注入 v4 UUID polyfill。
 */
export function installCryptoPolyfill(): void {
  if (typeof globalThis === 'undefined') {
    return;
  }

  const globalCtx = globalThis as unknown as Record<string, unknown>;

  let cryptoObj = globalCtx.crypto as Record<string, unknown> | undefined;
  if (!cryptoObj) {
    cryptoObj = {};
    globalCtx.crypto = cryptoObj;
  }

  if (typeof cryptoObj.randomUUID === 'function') {
    return;
  }

  cryptoObj.randomUUID = generateUUIDFallback;
}
