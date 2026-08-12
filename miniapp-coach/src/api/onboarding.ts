import { request } from './request'

export interface CoachCertificate {
  id?: number
  certType: string
  imageUrl: string
  sortOrder?: number
}

export interface CoachApplication {
  coachId: number
  applicationId?: number
  phone?: string
  name?: string
  gender?: 'male' | 'female'
  age?: number
  email?: string
  wechatQrUrl?: string
  idCardNo?: string
  teachingYears?: number
  totalStudents?: number
  totalHours?: number
  teachingStrokes?: string[]
  bio?: string
  referencePrice?: number
  status: number
  applicationStatus?: string
  submittedAt?: string
  entryType: 'first' | 'draft' | 'reapply' | 'rejected'
  promptMessage?: string
  certificates: CoachCertificate[]
}

export interface SaveDraftParams {
  applicationId?: number
  name?: string
  gender?: 'male' | 'female'
  age?: number
  email?: string
  wechatQrUrl?: string
  idCardNo?: string
  teachingYears?: number
  totalStudents?: number
  totalHours?: number
  teachingStrokes?: string[]
  bio?: string
  referencePrice?: number
  certificates?: CoachCertificate[]
  idempotencyKey: string
}

export interface SubmitApplicationParams {
  name: string
  gender: 'male' | 'female'
  age: number
  email: string
  wechatQrUrl: string
  idCardNo: string
  teachingYears: number
  totalStudents: number
  totalHours: number
  bio: string
  referencePrice: number
  certificates: CoachCertificate[]
  idempotencyKey: string
}

export interface SaveDraftResponse {
  coachId: number
  applicationId?: number
  status: number
  submittedAt?: string
}

export function getApplicationDetail() {
  return request<CoachApplication>({
    url: '/coach/application/detail',
    method: 'POST',
  })
}

export function saveDraft(params: SaveDraftParams) {
  return request<SaveDraftResponse>({
    url: '/coach/application/save-draft',
    method: 'POST',
    data: params,
  })
}

export function submitApplication(params: SubmitApplicationParams) {
  return request<SaveDraftResponse>({
    url: '/coach/application/submit',
    method: 'POST',
    data: params,
  })
}
