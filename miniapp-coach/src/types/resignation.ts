export type ResignationTicketStatus = 'processing' | 'pending_audit' | 'approved' | 'rejected'

export type PackageAction = 'refund' | 'transfer' | 'continue'

export interface CoachPackage {
  id: number
  studentName: string
  packageName: string
  lessonCount: number
  status: 'active' | 'frozen'
  action?: PackageAction
  targetCoachId?: number
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
