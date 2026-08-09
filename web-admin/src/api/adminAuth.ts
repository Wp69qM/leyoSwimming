import request from './request';
import type { ApiResponse, AdminLoginRequest, AdminLoginResponse } from '@/types/api';

export function adminLogin(data: AdminLoginRequest): Promise<ApiResponse<AdminLoginResponse>> {
  return request.post('/admin/auth/login', data) as Promise<ApiResponse<AdminLoginResponse>>;
}

export function adminLogout(): Promise<ApiResponse<null>> {
  return request.post('/admin/auth/logout') as Promise<ApiResponse<null>>;
}
