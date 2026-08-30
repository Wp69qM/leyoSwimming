import Taro from '@tarojs/taro'
import { API_BASE_URL } from '@/constants'
import { request } from './request'
import type { ConsentStatus } from './policy'

export interface CoachProfile {
  id: number
  name: string
  avatarUrl?: string
  portraitUrl?: string
  phone?: string
  age?: number
  gender?: 'male' | 'female'
  email?: string
  wechatQrUrl?: string
  idCardNoMasked?: string
  idCardFrontUrl?: string
  idCardBackUrl?: string
  coachCertUrls?: string[]
  healthCertUrl?: string
  totalStudents?: number
  totalHours?: number
  personalDesc?: string
  teachingYears?: number
  teachingStrokes?: string[]
  bio?: string
  referencePrice?: number
  status: number
  certificates?: string[]
  profileCompleted: boolean
  consent: ConsentStatus
}

export interface UpdateProfileParams {
  name: string
  age: number
  gender: 'male' | 'female'
  avatarUrl?: string
  portraitUrl?: string
  email?: string
  wechatQrUrl?: string
  personalDesc?: string
  teachingYears?: number
  certificates?: string[]
  teachingStrokes?: string[]
  bio?: string
  newPhone?: string
  oldPhoneVerifyCode?: string
  newPhoneVerifyCode?: string
  idempotencyKey: string
}

export interface ReferencePriceResult {
  referencePrice: number
  priceChangedAt: string
  remainingChangesToday: number
}

export function getProfile() {
  return request<CoachProfile>({
    url: '/coach/profile/detail',
    method: 'POST',
  })
}

export function updateProfile(params: UpdateProfileParams) {
  return request<CoachProfile>({
    url: '/coach/profile/update',
    method: 'POST',
    data: params,
  })
}

export function updateReferencePrice(params: {
  referencePrice: number
  idempotencyKey: string
}) {
  return request<ReferencePriceResult>({
    url: '/coach/profile/reference-price/update',
    method: 'POST',
    data: params,
  })
}

export function uploadAvatar(filePath: string) {
  return new Promise<string>((resolve, reject) => {
    const token = Taro.getStorageSync('leyo_coach_access_token')
    Taro.uploadFile({
      url: `${API_BASE_URL}/coach/profile/upload-avatar`,
      filePath,
      name: 'file',
      header: {
        Authorization: token ? `Bearer ${token}` : '',
      },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.code === 0) {
            resolve(data.data as string)
          } else {
            reject(new Error(data.message || '上传失败'))
          }
        } catch {
          reject(new Error('上传失败'))
        }
      },
      fail: reject,
    })
  })
}
