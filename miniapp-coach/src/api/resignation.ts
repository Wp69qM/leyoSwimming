import type {
  ApplyResignationResult,
  CoachPackage,
  PackageAction,
  PackageMode,
  ResignationTicket,
  ResignationTicketStatus
} from '@/types/resignation'
import { request } from './request'

export function applyResignation(data: { reason?: string }) {
  return request<ApplyResignationResult>({
    url: '/coach/resignation/apply',
    method: 'POST',
    data
  })
}

interface RawPackageItem {
  packageId?: number
  packageNo?: string
  userId?: number
  userName?: string
  packageName?: string
  packageMode?: PackageMode
  totalHours?: number
  availableCount?: number
  reservedCount?: number
  pricePerHour?: number
  action?: PackageAction
  targetCoachId?: number
  targetCoachName?: string
  targetCoachPhone?: string
}

interface RawResignationDetail {
  ticketId?: number
  ticketNo?: string
  status?: ResignationTicketStatus
  reason?: string
  totalPackages?: number
  handledPackages?: number
  submittedAt?: string
  packages?: RawPackageItem[]
}

function mapResignationTicket(raw: RawResignationDetail): ResignationTicket {
  const packages = (raw.packages ?? []).map((item): CoachPackage => {
    const availableCount = item.availableCount ?? 0
    const reservedCount = item.reservedCount ?? 0
    return {
      id: item.packageId ?? 0,
      packageNo: item.packageNo,
      userId: item.userId ?? 0,
      studentName: item.userName ?? '',
      packageName: item.packageName ?? '标准套餐',
      packageMode: item.packageMode ?? 'standard',
      totalHours: item.totalHours ?? 0,
      availableCount,
      reservedCount,
      lessonCount: availableCount + reservedCount,
      pricePerHour: item.pricePerHour,
      status: 'active',
      action: item.action,
      targetCoachId: item.targetCoachId,
      targetCoachName: item.targetCoachName,
      targetCoachPhone: item.targetCoachPhone
    }
  })

  return {
    ticketId: raw.ticketId ?? 0,
    ticketNo: raw.ticketNo ?? '',
    status: raw.status ?? 'none',
    reason: raw.reason,
    totalPackages: raw.totalPackages ?? packages.length,
    processedPackages: raw.handledPackages ?? 0,
    packages,
    updatedAt: raw.submittedAt ?? ''
  }
}

export function getResignationDetail(data: { ticketId?: number }) {
  return request<RawResignationDetail>({
    url: '/coach/resignation/detail',
    method: 'POST',
    data
  }).then(mapResignationTicket)
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
