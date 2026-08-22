import { request } from './request';

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  isNewUser: boolean;
  profileCompleted: boolean;
  userId: string;
}

export interface WechatLoginParams {
  code: string;
  phoneEncryptedData?: string;
  phoneIv?: string;
  avatarUrl?: string;
  nickName?: string;
  termsAccepted: boolean;
  privacyAccepted: boolean;
  termsVersion: string;
  privacyVersion: string;
  appType: 'user';
}

export interface PhoneLoginParams {
  phone: string;
  code: string;
  termsAccepted: boolean;
  privacyAccepted: boolean;
  termsVersion: string;
  privacyVersion: string;
}

export function wechatLogin(params: WechatLoginParams) {
  return request<LoginResult>({
    url: '/user/auth/wechat-login',
    method: 'POST',
    data: params,
    needToken: false,
  });
}

export function phoneLogin(params: PhoneLoginParams) {
  return request<LoginResult>({
    url: '/user/auth/phone-login',
    method: 'POST',
    data: params,
    needToken: false,
  });
}

export function logout(token: string) {
  return request<unknown>({
    url: '/user/auth/logout',
    method: 'POST',
    data: { refreshToken: token },
  });
}

export function refreshToken(token: string) {
  return request<{
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
  }>({
    url: '/user/auth/refresh',
    method: 'POST',
    data: { refreshToken: token },
    needToken: false,
  });
}
