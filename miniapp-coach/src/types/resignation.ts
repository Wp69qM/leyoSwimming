export type ResignationTicketStatus = 'processing' | 'pending_audit' | 'approved' | 'rejected' | 'none'

export type PackageAction = 'refund' | 'transfer' | 'continue'

export type PackageMode = 'standard' | 'experience' | 'custom'

export interface CoachPackage {
  id: number
  packageNo?: string
  studentName: string
  userId: number
  packageName: string
  packageMode: PackageMode
  totalHours: number
  availableCount: number
  reservedCount: number
  lessonCount: number
  pricePerHour?: number
  status: 'active' | 'frozen'
  action?: PackageAction
  targetCoachId?: number
  targetCoachName?: string
  targetCoachPhone?: string
}

export interface ResignationTicket {
  ticketId: number
  ticketNo: string
  status: ResignationTicketStatus
  reason?: string
  totalPackages: number
  processedPackages: number
  packages: CoachPackage[]
  updatedAt: string
  estimatedProcessTime?: string
}

export interface ApplyResignationResult {
  ticketId: number
  ticketNo: string
  totalPackages: number
  message: string
}
