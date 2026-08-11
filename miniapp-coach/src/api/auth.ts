import type { LoginResult, CoachStatus } from '@/types/auth'
import { request } from './request'

export interface WechatLoginRequest {
  code: string
  phoneEncryptedData: string
  phoneIv: string
  termsAccepted: boolean
  privacyAccepted: boolean
  termsVersion: string
  privacyVersion: string
  appType: 'coach'
}

export interface PhoneLoginRequest {
  phone: string
  code: string
  termsAccepted: boolean
  privacyAccepted: boolean
  termsVersion: string
  privacyVersion: string
  appType: 'coach'
}

export interface CoachStatusDetail {
  coachStatus: CoachStatus
  rejectionReason: string | null
}

export function wechatLogin(data: WechatLoginRequest): Promise<LoginResult> {
  return request<LoginResult>({
    url: '/coach/auth/wechat-login',
    data,
    needToken: false
  })
}

export function phoneLogin(data: PhoneLoginRequest): Promise<LoginResult> {
  return request<LoginResult>({
    url: '/coach/auth/phone-login',
    data,
    needToken: false
  })
}

export function refreshToken(token: string): Promise<{ accessToken: string; refreshToken: string; expiresInSeconds: number }> {
  return request<{ accessToken: string; refreshToken: string; expiresInSeconds: number }>({
    url: '/coach/auth/refresh',
    data: { refreshToken: token },
    needToken: false
  })
}

export function logoutCoach(): Promise<void> {
  return request<void>({
    url: '/coach/auth/logout'
  })
}

export function getCoachStatusDetail(): Promise<CoachStatusDetail> {
  return request<CoachStatusDetail>({
    url: '/coach/status/detail'
  })
}
