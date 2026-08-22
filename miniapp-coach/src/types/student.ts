export interface CoachStudentPackageTag {
  label: string
  type: string
}

export interface CoachStudentItem {
  studentUserId: number
  avatarUrl: string | null
  name: string
  gender: string | null
  age: number | null
  isMinor: boolean
  activePackageTags: CoachStudentPackageTag[]
}

export interface CoachStudentListData {
  students: CoachStudentItem[]
}

export type StudentTab = 'active' | 'history'

export interface CoachStudentListRequest {
  tab: StudentTab
  keyword?: string
}

export interface CoachStudentUserProfile {
  avatarUrl: string | null
  name: string
  phoneMasked: string | null
  age: number | null
  gender: string | null
  hasSwimBasis: boolean | null
  swimStrokes: string | null
  swimYears: string | null
  personalDesc: string | null
  isMinor: boolean
  guardianName: string | null
  guardianPhoneMasked: string | null
}

export interface CoachStudentSlice {
  learningStrokes: string | null
  swimLevel: number | null
  basics: string | null
  notes: string | null
}

export interface CoachStudentSummary {
  totalHours: number
  remainingHours: number
  lastClassDate: string | null
}

export interface CoachStudentDetailData {
  studentUserId: number
  userProfile: CoachStudentUserProfile
  coachSlice: CoachStudentSlice
  summary: CoachStudentSummary
}

export interface CoachStudentPackageItem {
  packageId: number
  packageName: string
  packageMode: string
  status: string
  statusLabel: string
  validStart: string | null
  validEnd: string | null
  remainingHours: number
}

export interface CoachStudentPackageListData {
  packages: CoachStudentPackageItem[]
}

export interface CoachPackageDetailData {
  packageId: number
  packageName: string
  packageMode: string
  status: string
  statusLabel: string
  teachingType: string | null
  strokeNames: string | null
  durationMinutes: number | null
  validStart: string | null
  validEnd: string | null
  totalHours: number
  consumedHours: number
  availableHours: number
  reservedHours: number
  paidAmount: number | null
  student: CoachPackageStudentMiniCard
  usageRecords: CoachPackageUsageRecord[]
}

export interface CoachPackageStudentMiniCard {
  userId: number
  name: string
  avatarUrl: string | null
  age: number | null
  gender: string | null
}

export interface CoachPackageUsageRecord {
  bookingId: number
  startTime: string
  status: string
  statusLabel: string
  consumedHours: number
  cancelReason: string | null
}

export interface CoachPackageDetailRequest {
  packageId: number
}

export interface CoachStudentUpdateRequest {
  studentId: number
  learningStrokes?: string
  swimLevel?: number
  basics?: string
  notes?: string
  idempotencyKey: string
}
