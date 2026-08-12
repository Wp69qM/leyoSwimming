import request from './request';
import type {
  ApiResponse,
  AdminCoachListRequest,
  AdminCoachListItem,
  AdminCoachDetail,
  AdminCoachAddRequest,
  AdminCoachUpdateRequest,
  AdminCoachCancelEntryRequest,
  PageResponse,
} from '@/types/api';

export function getCoachList(
  data: AdminCoachListRequest
): Promise<ApiResponse<PageResponse<AdminCoachListItem>>> {
  return request.post('/admin/coach/list', data) as Promise<
    ApiResponse<PageResponse<AdminCoachListItem>>
  >;
}

export function getCoachDetail(data: {
  coachId: number;
}): Promise<ApiResponse<AdminCoachDetail>> {
  return request.post('/admin/coach/detail', data) as Promise<
    ApiResponse<AdminCoachDetail>
  >;
}

export function addCoach(
  data: AdminCoachAddRequest
): Promise<ApiResponse<AdminCoachDetail>> {
  return request.post('/admin/coach/add', data) as Promise<
    ApiResponse<AdminCoachDetail>
  >;
}

export function updateCoach(
  data: AdminCoachUpdateRequest
): Promise<ApiResponse<AdminCoachDetail>> {
  return request.post('/admin/coach/update', data) as Promise<
    ApiResponse<AdminCoachDetail>
  >;
}

export function cancelCoachEntry(
  data: AdminCoachCancelEntryRequest
): Promise<ApiResponse<AdminCoachDetail>> {
  return request.post('/admin/coach/cancelEntry', data) as Promise<
    ApiResponse<AdminCoachDetail>
  >;
}
