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
