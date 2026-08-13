import request from './request';
import type {
  ApiResponse,
  AdminOrderListRequest,
  AdminOrderListResponse,
  AdminOrderDetail,
  AdminOrderDetailRequest,
  AdminOrderRefundApproveRequest,
  AdminOrderRefundRejectRequest,
} from '@/types/api';

export function getOrderList(
  data: AdminOrderListRequest
): Promise<ApiResponse<AdminOrderListResponse>> {
  return request.post('/admin/order/list', data) as Promise<
    ApiResponse<AdminOrderListResponse>
  >;
}

export function getOrderDetail(
  data: AdminOrderDetailRequest
): Promise<ApiResponse<AdminOrderDetail>> {
  return request.post('/admin/order/detail', data) as Promise<
    ApiResponse<AdminOrderDetail>
  >;
}

export function approveRefund(
  data: AdminOrderRefundApproveRequest
): Promise<ApiResponse<void>> {
  return request.post('/admin/order/refundApprove', data) as Promise<
    ApiResponse<void>
  >;
}

export function rejectRefund(
  data: AdminOrderRefundRejectRequest
): Promise<ApiResponse<void>> {
  return request.post('/admin/order/refundReject', data) as Promise<
    ApiResponse<void>
  >;
}
