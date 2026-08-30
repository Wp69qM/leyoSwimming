import Taro from '@tarojs/taro';
import {
  clearStorage,
  getStorageItem,
  removeStorageItem,
  setStorageItem,
} from './storage';

jest.mock('@tarojs/taro', () => ({
  getStorageSync: jest.fn(),
  setStorageSync: jest.fn(),
  removeStorageSync: jest.fn(),
  clearStorageSync: jest.fn(),
}));

describe('storage utils', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getStorageItem', () => {
    test('returns stored value when Taro succeeds', () => {
      (Taro.getStorageSync as jest.Mock).mockReturnValue('token');
      expect(getStorageItem<string>('key')).toBe('token');
      expect(Taro.getStorageSync).toHaveBeenCalledWith('key');
    });

    test('returns null when Taro throws', () => {
      (Taro.getStorageSync as jest.Mock).mockImplementation(() => {
        throw new Error('fail');
      });
      expect(getStorageItem<string>('key')).toBeNull();
    });
  });

  describe('setStorageItem', () => {
    test('calls Taro.setStorageSync with key and value', () => {
      setStorageItem('key', 'value');
      expect(Taro.setStorageSync).toHaveBeenCalledWith('key', 'value');
    });
  });

  describe('removeStorageItem', () => {
    test('calls Taro.removeStorageSync with key', () => {
      removeStorageItem('key');
      expect(Taro.removeStorageSync).toHaveBeenCalledWith('key');
    });
  });

  describe('clearStorage', () => {
    test('calls Taro.clearStorageSync', () => {
      clearStorage();
      expect(Taro.clearStorageSync).toHaveBeenCalled();
    });
  });
});
