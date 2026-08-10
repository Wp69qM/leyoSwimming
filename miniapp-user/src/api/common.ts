import { request } from './request';

export interface SendSmsCodeParams {
  phone: string;
  scene: 'login' | 'register' | 'reset_password';
}

export function sendSmsCode(params: SendSmsCodeParams) {
  return request<unknown>({
    url: '/common/sms/send',
    method: 'POST',
    data: params,
    needToken: false,
  });
}
