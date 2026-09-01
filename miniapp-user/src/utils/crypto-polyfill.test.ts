import { installCryptoPolyfill } from './crypto-polyfill';

function setGlobalCrypto(value: unknown) {
  Object.defineProperty(globalThis, 'crypto', {
    value,
    configurable: true,
    writable: true,
  });
}

describe('installCryptoPolyfill', () => {
  afterEach(() => {
    setGlobalCrypto(undefined);
  });

  it('当 crypto 不存在时创建 crypto 对象并注入 randomUUID', () => {
    setGlobalCrypto(undefined);

    installCryptoPolyfill();

    expect(typeof globalThis.crypto.randomUUID).toBe('function');
    expect(globalThis.crypto.randomUUID()).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
    );
  });

  it('当 crypto.randomUUID 不存在时注入 randomUUID', () => {
    setGlobalCrypto({});

    installCryptoPolyfill();

    expect(typeof globalThis.crypto.randomUUID).toBe('function');
  });

  it('当 crypto.randomUUID 已存在时不覆盖', () => {
    const existing = jest.fn().mockReturnValue('existing-uuid');
    setGlobalCrypto({ randomUUID: existing });

    installCryptoPolyfill();

    expect(globalThis.crypto.randomUUID()).toBe('existing-uuid');
    expect(existing).toHaveBeenCalledTimes(1);
  });
});
