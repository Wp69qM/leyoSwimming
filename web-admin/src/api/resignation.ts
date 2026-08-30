import request from './request';
import type { ApiResponse } from '@/types/api';
import type {
  ResignationListParams,
  ResignationTicketDetail,
  ResignationTicketListData,
} from '@/types/resignation';

export function listResignationTickets(
  params: ResignationListParams
): Promise<ApiResponse<ResignationTicketListData>> {
  const payload = {
    status: params.status || undefined,
    page: params.page,
    pageSize: params.pageSize,
    keyword: params.keyword || undefined,
    submitStartDate: params.submitStartDate || undefined,
    submitEndDate: params.submitEndDate || undefined,
  };
  return request.post(
    '/admin/coach/resignation-ticket/list',
    payload
  ) as Promise<ApiResponse<ResignationTicketListData>>;
}

export function getResignationTicketDetail(
  ticketId: string
): Promise<ApiResponse<ResignationTicketDetail>> {
  return request.post('/admin/coach/resignation-ticket/detail', {
    ticketId,
  }) as Promise<ApiResponse<ResignationTicketDetail>>;
}

export function approveResignationTicket(
  ticketId: string,
  comment?: string
): Promise<ApiResponse<null>> {
  return request.post('/admin/coach/resignation-ticket/approve', {
    ticketId,
    comment: comment?.trim() || undefined,
  }) as Promise<ApiResponse<null>>;
}

export function rejectResignationTicket(
  ticketId: string,
  reason: string
): Promise<ApiResponse<null>> {
  return request.post('/admin/coach/resignation-ticket/reject', {
    ticketId,
    reason,
  }) as Promise<ApiResponse<null>>;
}
