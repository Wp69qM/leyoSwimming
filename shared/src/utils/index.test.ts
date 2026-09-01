import { generateUUID, generateUUIDFallback } from './index';

function setGlobalCrypto(value: Crypto | undefined) {
  Object.defineProperty(globalThis, 'crypto', {
    value,
    configurable: true,
    writable: true,
  });
}

describe('generateUUID', () => {
  beforeEach(() => {
    setGlobalCrypto(undefined);
  });

  afterEach(() => {
    setGlobalCrypto(undefined);
  });

  it('优先使用 crypto.randomUUID 当可用时', () => {
    const expected = 'mocked-uuid-from-crypto';
    const randomUUID = jest.fn().mockReturnValue(expected);
    setGlobalCrypto({ randomUUID } as unknown as Crypto);

    expect(generateUUID()).toBe(expected);
    expect(randomUUID).toHaveBeenCalledTimes(1);
  });

  it('当 crypto 不存在时回退到 Math.random', () => {
    const result = generateUUID();

    expect(result).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
    );
  });

  it('当 crypto.randomUUID 不是函数时回退到 Math.random', () => {
    setGlobalCrypto({ randomUUID: undefined } as unknown as Crypto);

    const result = generateUUID();

    expect(result).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
    );
  });

  it('回退时生成固定 UUID 当 Math.random 恒为 0', () => {
    jest.spyOn(Math, 'random').mockReturnValue(0);

    expect(generateUUID()).toBe('00000000-0000-4000-8000-000000000000');
  });
});

describe('generateUUIDFallback', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('始终使用 Math.random 生成 v4 UUID', () => {
    const result = generateUUIDFallback();

    expect(result).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
    );
  });

  it('Math.random 恒为 0 时返回全零 UUID', () => {
    jest.spyOn(Math, 'random').mockReturnValue(0);

    expect(generateUUIDFallback()).toBe(
      '00000000-0000-4000-8000-000000000000'
    );
  });
});
