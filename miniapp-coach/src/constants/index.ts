export const APP_NAME = 'leyoSwimming 教练端'

export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'leyo_coach_access_token',
  REFRESH_TOKEN: 'leyo_coach_refresh_token',
  TOKEN_EXPIRES_AT: 'leyo_coach_token_expires_at',
  COACH_INFO: 'leyo_coach_info'
} as const

export const API_BASE_URL =
  (typeof process !== 'undefined' &&
    process.env &&
    process.env.TARO_APP_API_BASE_URL) ||
  '/api'

export const COACH_STATUS = {
  PENDING_ONBOARDING: -1,
  UNDER_REVIEW: 0,
  APPROVED: 1,
  REJECTED: 2,
  RESIGNED: 3,
  RESIGNING: 4
} as const

export const SMS_COUNTDOWN_SECONDS = 60
export const ACCESS_TOKEN_LEEWAY_SECONDS = 60
