import request from './request';
import type {
  ApiResponse,
  AdminPackageListRequest,
  AdminPackageListResponse,
  AdminPackageDetail,
  AdminPackageDetailRequest,
  AdminPackageFreezeRequest,
  AdminPackageUnfreezeRequest,
  AdminPackageExtendRequest,
  AdminPackageRefundRequest,
  AdminPackageOperationResponse,
} from '@/types/api';

export function getPackageList(
  data: AdminPackageListRequest
): Promise<ApiResponse<AdminPackageListResponse>> {
  return request.post('/admin/package/list', data) as Promise<
    ApiResponse<AdminPackageListResponse>
  >;
}

export function getPackageDetail(
  data: AdminPackageDetailRequest
): Promise<ApiResponse<AdminPackageDetail>> {
  return request.post('/admin/package/detail', data) as Promise<
    ApiResponse<AdminPackageDetail>
  >;
}

export function freezePackage(
  data: AdminPackageFreezeRequest
): Promise<ApiResponse<AdminPackageOperationResponse>> {
  return request.post('/admin/package/freeze', data) as Promise<
    ApiResponse<AdminPackageOperationResponse>
  >;
}

export function unfreezePackage(
  data: AdminPackageUnfreezeRequest
): Promise<ApiResponse<AdminPackageOperationResponse>> {
  return request.post('/admin/package/unfreeze', data) as Promise<
    ApiResponse<AdminPackageOperationResponse>
  >;
}

export function extendPackage(
  data: AdminPackageExtendRequest
): Promise<ApiResponse<AdminPackageOperationResponse>> {
  return request.post('/admin/package/extend', data) as Promise<
    ApiResponse<AdminPackageOperationResponse>
  >;
}

export function refundPackage(
  data: AdminPackageRefundRequest
): Promise<ApiResponse<AdminPackageOperationResponse>> {
  return request.post('/admin/package/refund', data) as Promise<
    ApiResponse<AdminPackageOperationResponse>
  >;
}
