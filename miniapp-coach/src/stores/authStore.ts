import { create } from 'zustand'
import { STORAGE_KEYS, COACH_STATUS } from '@/constants'
import { storage } from '@/utils/storage'
import type { CoachInfo, CoachStatus } from '@/types/auth'

export interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  tokenExpiresAt: number | null
  coachInfo: CoachInfo | null
  isLoggedIn: boolean
  setTokens: (accessToken: string, refreshToken: string, expiresInSeconds: number) => void
  setCoachInfo: (coachInfo: CoachInfo | null) => void
  login: (accessToken: string, refreshToken: string, expiresInSeconds: number, coachInfo: CoachInfo) => void
  logout: () => void
  clear: () => void
  restoreFromStorage: () => void
}

export function calculateExpiresAt(expiresInSeconds: number): number {
  return Date.now() + expiresInSeconds * 1000
}

export function getRedirectPageByStatus(status: CoachStatus): string {
  switch (status) {
    case COACH_STATUS.PENDING_ONBOARDING:
    case COACH_STATUS.REJECTED:
    case COACH_STATUS.RESIGNED:
      return '/pages/onboarding/index/index'
    case COACH_STATUS.UNDER_REVIEW:
      return '/pages/onboarding/pending/index'
    case COACH_STATUS.APPROVED:
    case COACH_STATUS.RESIGNING:
      return '/pages/index/index'
    default:
      return '/pages/onboarding/index/index'
  }
}

function loadToken(): string | null {
  const value = storage.get<string>(STORAGE_KEYS.ACCESS_TOKEN)
  return value && value !== 'undefined' && value !== 'null' ? value : null
}

function loadRefreshToken(): string | null {
  const value = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
  return value && value !== 'undefined' && value !== 'null' ? value : null
}

function loadTokenExpiresAt(): number | null {
  const value = storage.get<number>(STORAGE_KEYS.TOKEN_EXPIRES_AT)
  return value && value !== 0 ? value : null
}

function loadCoachInfo(): CoachInfo | null {
  try {
    const raw = storage.get<string>(STORAGE_KEYS.COACH_INFO)
    if (!raw || raw === 'undefined' || raw === 'null') return null
    return JSON.parse(raw) as CoachInfo
  } catch {
    return null
  }
}

function loadAuthStateFromStorage(): Pick<
  AuthState,
  'accessToken' | 'refreshToken' | 'tokenExpiresAt' | 'coachInfo' | 'isLoggedIn'
> {
  const accessToken = loadToken()
  const refreshToken = loadRefreshToken()
  const tokenExpiresAt = loadTokenExpiresAt()
  const coachInfo = loadCoachInfo()
  const isLoggedIn = Boolean(
    accessToken && tokenExpiresAt && Date.now() < tokenExpiresAt
  )
  return { accessToken, refreshToken, tokenExpiresAt, coachInfo, isLoggedIn }
}

export const useAuthStore = create<AuthState>((set) => ({
  ...loadAuthStateFromStorage(),

  setTokens: (accessToken, refreshToken, expiresInSeconds) => {
    const tokenExpiresAt = calculateExpiresAt(expiresInSeconds)
    storage.set(STORAGE_KEYS.ACCESS_TOKEN, accessToken)
    storage.set(STORAGE_KEYS.REFRESH_TOKEN, refreshToken)
    storage.set(STORAGE_KEYS.TOKEN_EXPIRES_AT, tokenExpiresAt)
    set({ accessToken, refreshToken, tokenExpiresAt })
  },

  setCoachInfo: (coachInfo) => {
    if (coachInfo) {
      storage.set(STORAGE_KEYS.COACH_INFO, JSON.stringify(coachInfo))
    } else {
      storage.remove(STORAGE_KEYS.COACH_INFO)
    }
    set({ coachInfo })
  },

  login: (accessToken, refreshToken, expiresInSeconds, coachInfo) => {
    const tokenExpiresAt = calculateExpiresAt(expiresInSeconds)
    storage.set(STORAGE_KEYS.ACCESS_TOKEN, accessToken)
    storage.set(STORAGE_KEYS.REFRESH_TOKEN, refreshToken)
    storage.set(STORAGE_KEYS.TOKEN_EXPIRES_AT, tokenExpiresAt)
    storage.set(STORAGE_KEYS.COACH_INFO, JSON.stringify(coachInfo))
    set({ accessToken, refreshToken, tokenExpiresAt, coachInfo, isLoggedIn: true })
  },

  logout: () => {
    storage.remove(STORAGE_KEYS.ACCESS_TOKEN)
    storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
    storage.remove(STORAGE_KEYS.TOKEN_EXPIRES_AT)
    storage.remove(STORAGE_KEYS.COACH_INFO)
    set({ accessToken: null, refreshToken: null, tokenExpiresAt: null, coachInfo: null, isLoggedIn: false })
  },

  clear: () => {
    storage.remove(STORAGE_KEYS.ACCESS_TOKEN)
    storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
    storage.remove(STORAGE_KEYS.TOKEN_EXPIRES_AT)
    storage.remove(STORAGE_KEYS.COACH_INFO)
    set({ accessToken: null, refreshToken: null, tokenExpiresAt: null, coachInfo: null, isLoggedIn: false })
  },

  restoreFromStorage: () => {
    set(loadAuthStateFromStorage())
  }
}))
