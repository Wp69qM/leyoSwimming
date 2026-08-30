import Taro from '@tarojs/taro';
import { getApiBaseUrl } from '@/utils/env';
import { request } from './request';
import type { ConsentStatus } from './policy';

export interface UserProfile {
  id: string;
  name: string;
  avatarUrl?: string;
  phone?: string;
  age?: number;
  gender?: 'male' | 'female';
  guardianName?: string;
  guardianPhone?: string;
  hasSwimBasis?: boolean;
  swimStrokes?: string[];
  swimYears?: number;
  personalDesc?: string;
  profileCompleted: boolean;
  consent: ConsentStatus;
}

export interface UpdateProfileParams {
  name: string;
  age: number;
  gender: 'male' | 'female';
  guardianName?: string;
  guardianPhone?: string;
  hasSwimBasis: boolean;
  swimStrokes?: string[];
  swimYears?: number;
  personalDesc?: string;
  avatarUrl?: string;
  phone?: string;
  newPhone?: string;
  oldPhoneVerifyCode?: string;
  newPhoneVerifyCode?: string;
  idempotencyKey: string;
}

export function getProfile() {
  return request<UserProfile>({
    url: '/user/profile/detail',
    method: 'POST',
  });
}

export function updateProfile(params: UpdateProfileParams) {
  return request<UserProfile>({
    url: '/user/profile/update',
    method: 'POST',
    data: params,
  });
}

function uploadFileToUrl(filePath: string, url: string): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    const token = Taro.getStorageSync('access_token');
    Taro.uploadFile({
      url,
      filePath,
      name: 'file',
      header: {
        Authorization: token ? `Bearer ${token}` : '',
      },
      success: (res) => {
        try {
          const data = JSON.parse(res.data);
          if (data.code === 0) {
            resolve(data.data as string);
          } else {
            reject(new Error(data.message || '上传失败'));
          }
        } catch {
          reject(new Error('上传失败'));
        }
      },
      fail: reject,
    });
  });
}

export function uploadAvatar(filePath: string) {
  return uploadFileToUrl(
    filePath,
    `${getApiBaseUrl()}/api/user/profile/upload-avatar`
  );
}

export function uploadFile(filePath: string) {
  return uploadFileToUrl(filePath, `${getApiBaseUrl()}/api/common/file/upload`);
}
