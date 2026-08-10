import Taro from '@tarojs/taro'
import { API_BASE_URL, STORAGE_KEYS } from '@/constants'
import { storage } from '@/utils/storage'
import type { ApiResponse } from '@/types/api'

export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  data?: unknown
  headers?: Record<string, string>
  needToken?: boolean
}

export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'ApiError'
  }
}

function getAccessToken(): string | null {
  return storage.get<string>(STORAGE_KEYS.ACCESS_TOKEN)
}

function getRefreshToken(): string | null {
  return storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
}

function setTokens(accessToken: string, refreshToken: string, expiresIn: number): void {
  storage.set(STORAGE_KEYS.ACCESS_TOKEN, accessToken)
  storage.set(STORAGE_KEYS.REFRESH_TOKEN, refreshToken)
  storage.set(STORAGE_KEYS.TOKEN_EXPIRES_AT, Date.now() + expiresIn * 1000)
}

function clearTokens(): void {
  storage.remove(STORAGE_KEYS.ACCESS_TOKEN)
  storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
  storage.remove(STORAGE_KEYS.TOKEN_EXPIRES_AT)
}

export function isTokenExpired(): boolean {
  const expiresAt = storage.get<number>(STORAGE_KEYS.TOKEN_EXPIRES_AT)
  if (!expiresAt) return true
  return Date.now() >= expiresAt
}

let refreshPromise: Promise<string | null> | null = null

async function doRefreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    clearTokens()
    return null
  }

  try {
    const res = await Taro.request<ApiResponse<{ accessToken: string; refreshToken: string; expiresInSeconds: number }>>({
      url: `${API_BASE_URL}/coach/auth/refresh`,
      method: 'POST',
      data: { refreshToken },
      header: { 'Content-Type': 'application/json' }
    })

    const { data: result } = res
    if (result.code === 0 && result.data) {
      const { accessToken, refreshToken: newRefreshToken, expiresInSeconds } = result.data
      setTokens(accessToken, newRefreshToken, expiresInSeconds)
      return accessToken
    }
  } catch {
    // refresh failed, fall through to clear tokens
  }

  clearTokens()
  return null
}

export async function refreshAccessToken(): Promise<string | null> {
  if (refreshPromise) return refreshPromise

  refreshPromise = doRefreshAccessToken()
  refreshPromise.finally(() => {
    refreshPromise = null
  })

  return refreshPromise
}

function redirectToLogin(): void {
  Taro.redirectTo({ url: '/pages/login/wechat/index' }).catch(() => {
    // ignore navigation errors
  })
}

export async function request<T>(options: RequestOptions): Promise<T> {
  const { url, method = 'POST', data, headers = {}, needToken = true } = options

  const requestHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...headers
  }

  if (needToken) {
    const token = getAccessToken()
    if (token) {
      requestHeaders.Authorization = `Bearer ${token}`
    }
  }

  try {
    const res = await Taro.request<ApiResponse<T>>({
      url: `${API_BASE_URL}${url}`,
      method,
      data,
      header: requestHeaders
    })

    const { statusCode, data: responseData } = res

    if (statusCode === 401) {
      const newToken = await refreshAccessToken()
      if (newToken) {
        return request<T>(options)
      }
      redirectToLogin()
      throw new ApiError(200002, '登录已过期，请重新登录')
    }

    if (statusCode < 200 || statusCode >= 300) {
      throw new ApiError(
        responseData?.code ?? statusCode,
        responseData?.message || `HTTP ${statusCode}`
      )
    }

    if (responseData.code !== 0) {
      throw new ApiError(responseData.code, responseData.message || '请求失败')
    }

    return responseData.data as T
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    const message = error instanceof Error ? error.message : '网络异常，请稍后重试'
    throw new ApiError(0, message)
  }
}

export function handleBusinessError(error: unknown): string {
  if (error instanceof Error) return error.message
  return '网络异常，请稍后重试'
}

export function getErrorCode(error: unknown): number | null {
  if (error instanceof ApiError) return error.code
  return null
}
