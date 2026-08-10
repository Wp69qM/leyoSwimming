import type {
  ApplyResignationResult,
  PackageAction,
  ResignationTicket
} from '@/types/resignation'
import { request } from './request'

export function applyResignation(data: { reason?: string }) {
  return request<ApplyResignationResult>({
    url: '/coach/resignation/apply',
    method: 'POST',
    data
  })
}

export function getResignationDetail(data: { ticketId?: number }) {
  return request<ResignationTicket>({
    url: '/coach/resignation/detail',
    method: 'POST',
    data
  })
}

export function submitPackageAction(data: {
  ticketId: number
  packageId: number
  action: PackageAction
  targetCoachId?: number
}) {
  return request<{ success: boolean }>({
    url: '/coach/resignation/package/action',
    method: 'POST',
    data
  })
}

export function submitResignationTicket(data: { ticketId: number }) {
  return request<{ success: boolean }>({
    url: '/coach/resignation/submit',
    method: 'POST',
    data
  })
}
