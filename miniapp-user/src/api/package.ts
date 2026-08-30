import type {
  CustomPackageConfig,
  PackageDetail,
  PackageListData,
  RefundApplyRequest,
  RefundApplyResponse,
  UserActivePackage,
  UserPackageDetail,
  UserPackageListItem,
} from '@/types/package';

import { request } from './request';

export function fetchPackageList(
  page = 1,
  pageSize = 20
): Promise<PackageListData> {
  return request<PackageListData>({
    url: '/package/list',
    method: 'POST',
    data: { page, pageSize },
    needToken: false,
  });
}

export function fetchPackageDetail(
  packageId: number,
  coachId?: number
): Promise<PackageDetail> {
  return request<PackageDetail>({
    url: '/package/detail',
    method: 'POST',
    data: { packageId, coachId },
    needToken: false,
  });
}

const DEFAULT_CUSTOM_CONFIG: CustomPackageConfig = {
  minHours: 1,
  maxHours: 50,
  defaultValidDays: 30,
  allowedValidDays: [30, 60, 90, 180],
};

export async function fetchCustomPackageConfig(): Promise<CustomPackageConfig> {
  const config = await request<CustomPackageConfig>({
    url: '/package/custom-config',
    method: 'POST',
    needToken: true,
  });
  return {
    ...DEFAULT_CUSTOM_CONFIG,
    ...config,
    allowedValidDays:
      config.allowedValidDays?.length > 0
        ? config.allowedValidDays
        : DEFAULT_CUSTOM_CONFIG.allowedValidDays,
  };
}

export function fetchActivePackage(): Promise<UserActivePackage | null> {
  return request<UserActivePackage | null>({
    url: '/user/package/active',
    method: 'POST',
  });
}

export function fetchMyPackageList(
  status?: string
): Promise<UserPackageListItem[]> {
  return request<UserPackageListItem[]>({
    url: '/user/package/list',
    method: 'POST',
    data: { status },
  });
}

export function fetchMyPackageDetail(
  packageId: number
): Promise<UserPackageDetail> {
  return request<UserPackageDetail>({
    url: '/user/package/detail',
    method: 'POST',
    data: { packageId },
  });
}

export function submitRefund(
  payload: RefundApplyRequest
): Promise<RefundApplyResponse> {
  return request<RefundApplyResponse>({
    url: '/user/package/refund',
    method: 'POST',
    data: payload,
  });
}
