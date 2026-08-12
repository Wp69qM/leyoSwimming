import request from './request';
import type {
  ApiResponse,
  AdminAccountListRequest,
  AdminAccountListItem,
  AdminAccountDetail,
  AdminAccountAddRequest,
  AdminAccountUpdateRequest,
  AdminAccountToggleStatusRequest,
  AdminAccountDeleteRequest,
  AdminAccountResetPasswordRequest,
  AdminAccountResetPasswordResponse,
  PageResponse,
} from '@/types/api';

export function getAdminAccountList(
  data: AdminAccountListRequest
): Promise<ApiResponse<PageResponse<AdminAccountListItem>>> {
  return request.post('/admin/admin/list', data) as Promise<
    ApiResponse<PageResponse<AdminAccountListItem>>
  >;
}

export function getAdminAccountDetail(data: {
  adminId: number;
}): Promise<ApiResponse<AdminAccountDetail>> {
  return request.post('/admin/admin/detail', data) as Promise<
    ApiResponse<AdminAccountDetail>
  >;
}

export function addAdminAccount(
  data: AdminAccountAddRequest
): Promise<ApiResponse<AdminAccountDetail>> {
  return request.post('/admin/admin/add', data) as Promise<
    ApiResponse<AdminAccountDetail>
  >;
}

export function updateAdminAccount(
  data: AdminAccountUpdateRequest
): Promise<ApiResponse<AdminAccountDetail>> {
  return request.post('/admin/admin/update', data) as Promise<
    ApiResponse<AdminAccountDetail>
  >;
}

export function toggleAdminAccountStatus(
  data: AdminAccountToggleStatusRequest
): Promise<ApiResponse<AdminAccountDetail>> {
  return request.post('/admin/admin/toggle-status', data) as Promise<
    ApiResponse<AdminAccountDetail>
  >;
}

export function deleteAdminAccount(
  data: AdminAccountDeleteRequest
): Promise<ApiResponse<null>> {
  return request.post('/admin/admin/delete', data) as Promise<
    ApiResponse<null>
  >;
}

export function resetAdminAccountPassword(
  data: AdminAccountResetPasswordRequest
): Promise<ApiResponse<AdminAccountResetPasswordResponse>> {
  return request.post('/admin/admin/reset-password', data) as Promise<
    ApiResponse<AdminAccountResetPasswordResponse>
  >;
}
