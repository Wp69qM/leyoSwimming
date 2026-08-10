import Taro from '@tarojs/taro'
import { STORAGE_KEYS } from '@/constants'
import { useAuthStore, calculateExpiresAt, getRedirectPageByStatus } from './authStore'

jest.mock('@tarojs/taro', () => ({
  getStorageSync: jest.fn(),
  setStorageSync: jest.fn(),
  removeStorageSync: jest.fn(),
  clearStorageSync: jest.fn()
}))

describe('authStore', () => {
  beforeEach(() => {
    jest.clearAllMocks()
    useAuthStore.setState({
      accessToken: null,
      refreshToken: null,
      tokenExpiresAt: null,
      coachInfo: null,
      isLoggedIn: false
    })
  })

  describe('calculateExpiresAt', () => {
    test('calculates expiration timestamp from seconds', () => {
      const before = Date.now()
      const result = calculateExpiresAt(3600)
      const after = Date.now()
      expect(result).toBeGreaterThanOrEqual(before + 3600 * 1000)
      expect(result).toBeLessThanOrEqual(after + 3600 * 1000)
    })
  })

  describe('getRedirectPageByStatus', () => {
    test('returns onboarding page for pending, rejected and resigned statuses', () => {
      expect(getRedirectPageByStatus(-1)).toBe('/pages/onboarding/index')
      expect(getRedirectPageByStatus(2)).toBe('/pages/onboarding/index')
      expect(getRedirectPageByStatus(3)).toBe('/pages/onboarding/index')
    })

    test('returns pending page for under review status', () => {
      expect(getRedirectPageByStatus(0)).toBe('/pages/onboarding/pending/index')
    })

    test('returns home page for approved and resigning statuses', () => {
      expect(getRedirectPageByStatus(1)).toBe('/pages/index/index')
      expect(getRedirectPageByStatus(4)).toBe('/pages/index/index')
    })
  })

  describe('login', () => {
    test('updates state and persists tokens to storage', () => {
      const coachInfo = { id: 1, status: 1 as const }
      useAuthStore.getState().login('access', 'refresh', 7200, coachInfo)

      const state = useAuthStore.getState()
      expect(state.accessToken).toBe('access')
      expect(state.refreshToken).toBe('refresh')
      expect(state.coachInfo).toEqual(coachInfo)
      expect(state.isLoggedIn).toBe(true)
      expect(state.tokenExpiresAt).not.toBeNull()

      expect(Taro.setStorageSync).toHaveBeenCalledWith(STORAGE_KEYS.ACCESS_TOKEN, 'access')
      expect(Taro.setStorageSync).toHaveBeenCalledWith(STORAGE_KEYS.REFRESH_TOKEN, 'refresh')
      expect(Taro.setStorageSync).toHaveBeenCalledWith(STORAGE_KEYS.TOKEN_EXPIRES_AT, expect.any(Number))
      expect(Taro.setStorageSync).toHaveBeenCalledWith(STORAGE_KEYS.COACH_INFO, JSON.stringify(coachInfo))
    })
  })

  describe('setTokens', () => {
    test('updates only token fields', () => {
      useAuthStore.getState().setTokens('a', 'r', 3600)

      const state = useAuthStore.getState()
      expect(state.accessToken).toBe('a')
      expect(state.refreshToken).toBe('r')
      expect(state.isLoggedIn).toBe(false)
    })
  })

  describe('setCoachInfo', () => {
    test('updates coachInfo field', () => {
      const coachInfo = { id: 2, status: 0 as const }
      useAuthStore.getState().setCoachInfo(coachInfo)
      expect(useAuthStore.getState().coachInfo).toEqual(coachInfo)
    })
  })

  describe('logout', () => {
    test('clears state and storage', () => {
      useAuthStore.getState().login('a', 'r', 7200, { id: 1, status: 1 as const })
      useAuthStore.getState().logout()

      const state = useAuthStore.getState()
      expect(state.accessToken).toBeNull()
      expect(state.refreshToken).toBeNull()
      expect(state.tokenExpiresAt).toBeNull()
      expect(state.coachInfo).toBeNull()
      expect(state.isLoggedIn).toBe(false)

      expect(Taro.removeStorageSync).toHaveBeenCalledWith(STORAGE_KEYS.ACCESS_TOKEN)
      expect(Taro.removeStorageSync).toHaveBeenCalledWith(STORAGE_KEYS.REFRESH_TOKEN)
      expect(Taro.removeStorageSync).toHaveBeenCalledWith(STORAGE_KEYS.TOKEN_EXPIRES_AT)
      expect(Taro.removeStorageSync).toHaveBeenCalledWith(STORAGE_KEYS.COACH_INFO)
    })
  })

  describe('restoreFromStorage', () => {
    test('restores logged-in state when token is valid', () => {
      const mockGetStorage = Taro.getStorageSync as jest.Mock
      mockGetStorage.mockImplementation((key: string) => {
        if (key === STORAGE_KEYS.ACCESS_TOKEN) return 'stored_access'
        if (key === STORAGE_KEYS.REFRESH_TOKEN) return 'stored_refresh'
        if (key === STORAGE_KEYS.TOKEN_EXPIRES_AT) return Date.now() + 100000
        return null
      })

      useAuthStore.getState().restoreFromStorage()

      const state = useAuthStore.getState()
      expect(state.accessToken).toBe('stored_access')
      expect(state.refreshToken).toBe('stored_refresh')
      expect(state.isLoggedIn).toBe(true)
    })

    test('sets logged-out state when token is expired', () => {
      const mockGetStorage = Taro.getStorageSync as jest.Mock
      mockGetStorage.mockImplementation((key: string) => {
        if (key === STORAGE_KEYS.ACCESS_TOKEN) return 'stored_access'
        if (key === STORAGE_KEYS.TOKEN_EXPIRES_AT) return Date.now() - 1000
        return null
      })

      useAuthStore.getState().restoreFromStorage()
      expect(useAuthStore.getState().isLoggedIn).toBe(false)
    })

    test('sets logged-out state when no token', () => {
      const mockGetStorage = Taro.getStorageSync as jest.Mock
      mockGetStorage.mockReturnValue(null)
      useAuthStore.getState().restoreFromStorage()
      expect(useAuthStore.getState().isLoggedIn).toBe(false)
    })

    test('ignores undefined or null string tokens', () => {
      const mockGetStorage = Taro.getStorageSync as jest.Mock
      mockGetStorage.mockReturnValue('undefined')
      useAuthStore.getState().restoreFromStorage()
      expect(useAuthStore.getState().isLoggedIn).toBe(false)
    })
  })
})
