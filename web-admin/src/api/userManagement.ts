import request from './request';
import type {
  ApiResponse,
  AdminUserListRequest,
  AdminUserListItem,
  AdminUserDetail,
  AdminUserAddRequest,
  AdminUserUpdateRequest,
  AdminUserBanRequest,
  PageResponse,
} from '@/types/api';

export function getUserList(
  data: AdminUserListRequest
): Promise<ApiResponse<PageResponse<AdminUserListItem>>> {
  return request.post('/admin/user/list', data) as Promise<
    ApiResponse<PageResponse<AdminUserListItem>>
  >;
}

export function getUserDetail(data: {
  userId: number;
}): Promise<ApiResponse<AdminUserDetail>> {
  return request.post('/admin/user/detail', data) as Promise<
    ApiResponse<AdminUserDetail>
  >;
}

export function addUser(
  data: AdminUserAddRequest
): Promise<ApiResponse<AdminUserDetail>> {
  return request.post('/admin/user/add', data) as Promise<
    ApiResponse<AdminUserDetail>
  >;
}

export function updateUser(
  data: AdminUserUpdateRequest
): Promise<ApiResponse<AdminUserDetail>> {
  return request.post('/admin/user/update', data) as Promise<
    ApiResponse<AdminUserDetail>
  >;
}

export function banUser(
  data: AdminUserBanRequest
): Promise<ApiResponse<AdminUserDetail>> {
  return request.post('/admin/user/ban', data) as Promise<
    ApiResponse<AdminUserDetail>
  >;
}

export function unbanUser(
  data: AdminUserBanRequest
): Promise<ApiResponse<AdminUserDetail>> {
  return request.post('/admin/user/unban', data) as Promise<
    ApiResponse<AdminUserDetail>
  >;
}
