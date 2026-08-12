import { request } from './request';

export interface SendSmsCodeParams {
  phone: string;
  scene:
    | 'login'
    | 'register'
    | 'reset_password'
    | 'change_phone_old'
    | 'change_phone_new';
}

export function sendSmsCode(params: SendSmsCodeParams) {
  return request<unknown>({
    url: '/common/sms/send',
    method: 'POST',
    data: params,
    needToken: false,
  });
}
