import Taro from '@tarojs/taro'

export const storage = {
  get<T>(key: string): T | null {
    try {
      const value = Taro.getStorageSync<T>(key)
      if (value === '' || value === null || value === undefined) return null
      return value
    } catch {
      return null
    }
  },

  set<T>(key: string, value: T): void {
    try {
      Taro.setStorageSync(key, value)
    } catch (error) {
      console.error(`Storage set failed for key ${key}:`, error)
    }
  },

  remove(key: string): void {
    try {
      Taro.removeStorageSync(key)
    } catch (error) {
      console.error(`Storage remove failed for key ${key}:`, error)
    }
  },

  clear(): void {
    try {
      Taro.clearStorageSync()
    } catch (error) {
      console.error('Storage clear failed:', error)
    }
  }
}
