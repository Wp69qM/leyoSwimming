import Taro from '@tarojs/taro'
import { API_BASE_URL } from '@/constants'
import { request } from './request'

export interface SendSmsRequest {
  phone: string
  appType: 'coach'
  scene: 'login' | 'change_phone_old' | 'change_phone_new'
}

export function sendSmsCode(data: SendSmsRequest): Promise<void> {
  return request<void>({
    url: '/common/sms/send',
    data,
    needToken: false
  })
}

export function uploadFile(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const token = Taro.getStorageSync('leyo_coach_access_token')
    Taro.uploadFile({
      url: `${API_BASE_URL}/common/file/upload`,
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
