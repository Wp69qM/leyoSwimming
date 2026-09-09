import Taro from '@tarojs/taro';
import { getPageQuery } from './router';

jest.mock('@tarojs/taro', () => ({
  getCurrentInstance: jest.fn(),
}));

const mockedGetCurrentInstance = Taro.getCurrentInstance as jest.Mock;

describe('getPageQuery', () => {
  const originalEnv = process.env.TARO_ENV;

  beforeEach(() => {
    jest.clearAllMocks();
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { hash: '', search: '' },
    });
  });

  afterEach(() => {
    process.env.TARO_ENV = originalEnv;
  });

  test('returns router params when they exist', () => {
    mockedGetCurrentInstance.mockReturnValue({
      router: { params: { id: '1', source: 'list' } },
    });

    expect(getPageQuery()).toEqual({ id: '1', source: 'list' });
  });

  test('returns empty object when router params are missing', () => {
    mockedGetCurrentInstance.mockReturnValue({});

    expect(getPageQuery()).toEqual({});
  });

  test('parses query string from hash in h5 mode', () => {
    process.env.TARO_ENV = 'h5';
    mockedGetCurrentInstance.mockReturnValue({
      router: { params: {} },
    });
    Object.defineProperty(window, 'location', {
      writable: true,
      value: {
        hash: '#/pages/coach/detail/index?id=1&source=list',
        search: '',
      },
    });

    expect(getPageQuery()).toEqual({ id: '1', source: 'list' });
  });

  test('parses query string from search when hash has no query', () => {
    process.env.TARO_ENV = 'h5';
    mockedGetCurrentInstance.mockReturnValue({
      router: { params: {} },
    });
    Object.defineProperty(window, 'location', {
      writable: true,
      value: {
        hash: '#/pages/coach/detail/index',
        search: '?id=2&source=detail',
      },
    });

    expect(getPageQuery()).toEqual({ id: '2', source: 'detail' });
  });

  test('decodes URL-encoded keys and values', () => {
    process.env.TARO_ENV = 'h5';
    mockedGetCurrentInstance.mockReturnValue({
      router: { params: {} },
    });
    Object.defineProperty(window, 'location', {
      writable: true,
      value: {
        hash: '#/pages/coach/detail/index?name=%E5%BC%A0%E4%B8%89&tag=a%26b',
        search: '',
      },
    });

    expect(getPageQuery()).toEqual({ name: '张三', tag: 'a&b' });
  });

  test('treats value as empty string when no value is provided', () => {
    process.env.TARO_ENV = 'h5';
    mockedGetCurrentInstance.mockReturnValue({
      router: { params: {} },
    });
    Object.defineProperty(window, 'location', {
      writable: true,
      value: {
        hash: '#/pages/coach/detail/index?debug',
        search: '',
      },
    });

    expect(getPageQuery()).toEqual({ debug: '' });
  });

  test('falls back to router params in non-h5 environments', () => {
    process.env.TARO_ENV = 'weapp';
    mockedGetCurrentInstance.mockReturnValue({
      router: { params: { id: '3' } },
    });

    expect(getPageQuery()).toEqual({ id: '3' });
  });

  test('returns router params directly in h5 mode when id already exists', () => {
    process.env.TARO_ENV = 'h5';
    mockedGetCurrentInstance.mockReturnValue({
      router: { params: { id: '4', source: 'taro' } },
    });
    Object.defineProperty(window, 'location', {
      writable: true,
      value: {
        hash: '#/pages/coach/detail/index?id=5&source=hash',
        search: '',
      },
    });

    expect(getPageQuery()).toEqual({ id: '4', source: 'taro' });
  });
});
