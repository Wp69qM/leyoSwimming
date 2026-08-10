import { request } from './request'

export interface SendSmsRequest {
  phone: string
  appType: 'coach'
  scene: 'login'
}

export function sendSmsCode(data: SendSmsRequest): Promise<void> {
  return request<void>({
    url: '/common/sms/send',
    data,
    needToken: false
  })
}
