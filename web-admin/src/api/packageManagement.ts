import request from './request';
import type {
  ApiResponse,
  AdminPackageListRequest,
  AdminPackageListResponse,
  AdminPackageDetail,
  AdminPackageDetailRequest,
  AdminPackageFreezeRequest,
  AdminPackageExtendRequest,
  AdminPackageRefundRequest,
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
): Promise<ApiResponse<void>> {
  return request.post('/admin/package/freeze', data) as Promise<
    ApiResponse<void>
  >;
}

export function unfreezePackage(data: {
  packageId: number;
}): Promise<ApiResponse<void>> {
  return request.post('/admin/package/unfreeze', data) as Promise<
    ApiResponse<void>
  >;
}

export function extendPackage(
  data: AdminPackageExtendRequest
): Promise<ApiResponse<void>> {
  return request.post('/admin/package/extend', data) as Promise<
    ApiResponse<void>
  >;
}

export function refundPackage(
  data: AdminPackageRefundRequest
): Promise<ApiResponse<void>> {
  return request.post('/admin/package/refund', data) as Promise<
    ApiResponse<void>
  >;
}
