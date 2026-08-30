import request from './request';
import type { ApiResponse } from '@/types/api';
import type {
  CoachApplicationDetail,
  CoachApplicationListData,
  CoachAuditListParams,
  CoachAuditStats,
} from '@/types/coachAudit';

export function listCoachApplications(
  params: CoachAuditListParams
): Promise<ApiResponse<CoachApplicationListData>> {
  const payload = {
    status: params.status || undefined,
    keyword: params.keyword || undefined,
    submitStartDate: params.submitStartDate || undefined,
    submitEndDate: params.submitEndDate || undefined,
    page: params.page,
    pageSize: params.pageSize,
  };
  return request.post('/admin/coach/application/list', payload) as Promise<
    ApiResponse<CoachApplicationListData>
  >;
}

export function getCoachApplicationDetail(
  applicationId: number
): Promise<ApiResponse<CoachApplicationDetail>> {
  return request.post('/admin/coach/application/detail', {
    applicationId,
  }) as Promise<ApiResponse<CoachApplicationDetail>>;
}

export function getCoachAuditStats(): Promise<ApiResponse<CoachAuditStats>> {
  return request.post('/admin/coach/application/stats') as Promise<
    ApiResponse<CoachAuditStats>
  >;
}

export function approveCoachApplication(
  applicationId: number
): Promise<ApiResponse<null>> {
  return request.post('/admin/coach/application/approve', {
    applicationId,
  }) as Promise<ApiResponse<null>>;
}

export function rejectCoachApplication(
  applicationId: number,
  reason: string
): Promise<ApiResponse<null>> {
  return request.post('/admin/coach/application/reject', {
    applicationId,
    reason,
  }) as Promise<ApiResponse<null>>;
}
