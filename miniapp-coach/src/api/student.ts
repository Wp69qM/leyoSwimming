import type {
  CoachPackageDetailData,
  CoachPackageDetailRequest,
  CoachStudentDetailData,
  CoachStudentListData,
  CoachStudentListRequest,
  CoachStudentPackageListData,
  CoachStudentUpdateRequest
} from '@/types/student'
import { request } from './request'

export function fetchCoachStudentList(
  params: CoachStudentListRequest
): Promise<CoachStudentListData> {
  return request<CoachStudentListData>({
    url: '/coach/student/list',
    method: 'POST',
    data: params
  })
}

export function fetchCoachStudentDetail(studentId: number): Promise<CoachStudentDetailData> {
  return request<CoachStudentDetailData>({
    url: '/coach/student/detail',
    method: 'POST',
    data: { studentId }
  })
}

export function fetchCoachStudentPackageList(
  studentId: number
): Promise<CoachStudentPackageListData> {
  return request<CoachStudentPackageListData>({
    url: '/coach/student/package-list',
    method: 'POST',
    data: { studentId }
  })
}

export function updateCoachStudent(
  params: CoachStudentUpdateRequest
): Promise<void> {
  return request<void>({
    url: '/coach/student/update',
    method: 'POST',
    data: params
  })
}

export function fetchCoachPackageDetail(
  params: CoachPackageDetailRequest
): Promise<CoachPackageDetailData> {
  return request<CoachPackageDetailData>({
    url: '/coach/package/detail',
    method: 'POST',
    data: params
  })
}
