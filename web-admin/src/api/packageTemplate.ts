import request from './request';
import type {
  ApiResponse,
  AdminPackageTemplateListRequest,
  AdminPackageTemplateListResponse,
  AdminPackageTemplateDetail,
  AdminPackageTemplateAddRequest,
  AdminPackageTemplateUpdateRequest,
  AdminPackageTemplateToggleRequest,
  AdminPackageTemplateCustomConfigRequest,
  AdminPackageTemplateCustomConfigResponse,
} from '@/types/api';

export function getPackageTemplateList(
  data: AdminPackageTemplateListRequest
): Promise<ApiResponse<AdminPackageTemplateListResponse>> {
  return request.post('/admin/package-template/list', data) as Promise<
    ApiResponse<AdminPackageTemplateListResponse>
  >;
}

export function getPackageTemplateDetail(data: {
  packageTemplateId: number;
}): Promise<ApiResponse<AdminPackageTemplateDetail>> {
  return request.post('/admin/package-template/detail', data) as Promise<
    ApiResponse<AdminPackageTemplateDetail>
  >;
}

export function addPackageTemplate(
  data: AdminPackageTemplateAddRequest
): Promise<ApiResponse<AdminPackageTemplateDetail>> {
  return request.post('/admin/package-template/add', data) as Promise<
    ApiResponse<AdminPackageTemplateDetail>
  >;
}

export function updatePackageTemplate(
  data: AdminPackageTemplateUpdateRequest
): Promise<ApiResponse<AdminPackageTemplateDetail>> {
  return request.post('/admin/package-template/update', data) as Promise<
    ApiResponse<AdminPackageTemplateDetail>
  >;
}

export function togglePackageTemplateStatus(
  data: AdminPackageTemplateToggleRequest
): Promise<ApiResponse<AdminPackageTemplateDetail>> {
  return request.post('/admin/package-template/toggle-status', data) as Promise<
    ApiResponse<AdminPackageTemplateDetail>
  >;
}

export function getCustomPackageConfig(): Promise<
  ApiResponse<AdminPackageTemplateCustomConfigResponse>
> {
  return request.post(
    '/admin/package-template/custom-config/detail',
    {}
  ) as Promise<ApiResponse<AdminPackageTemplateCustomConfigResponse>>;
}

export function saveCustomPackageConfig(
  data: AdminPackageTemplateCustomConfigRequest
): Promise<ApiResponse<AdminPackageTemplateCustomConfigResponse>> {
  return request.post('/admin/package-template/custom-config', data) as Promise<
    ApiResponse<AdminPackageTemplateCustomConfigResponse>
  >;
}
