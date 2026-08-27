import type {
  CoachDetail,
  CoachListData,
  CoachListSearchParams,
  UserPackageQualification,
} from '@/types/coach';

import { request } from './request';

export function fetchCoachList(
  params: CoachListSearchParams = {}
): Promise<CoachListData> {
  return request<CoachListData>({
    url: '/coach/list',
    method: 'POST',
    data: {
      page: params.page ?? 1,
      pageSize: params.pageSize ?? 10,
      keyword: params.keyword,
      sortBy: params.sortBy,
      sortOrder: params.sortOrder,
    },
    needToken: false,
  });
}

export function fetchCoachDetail(coachId: number): Promise<CoachDetail> {
  return request<CoachDetail>({
    url: '/coach/detail',
    method: 'POST',
    data: { coachId },
    needToken: true,
  });
}

export function fetchUserPackageQualification(): Promise<UserPackageQualification> {
  return request<UserPackageQualification>({
    url: '/user/package/qualification',
    method: 'POST',
  });
}
