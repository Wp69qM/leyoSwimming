export type CoachAuditStatus = 'pending' | 'approved' | 'rejected';

export type Gender = 'male' | 'female';

export interface CoachApplicationItem {
  coachId: number;
  applicationId: number;
  name: string;
  gender: Gender;
  age: number;
  teachingYears: number;
  teachingStrokes: string[];
  submittedAt: string;
  status: CoachAuditStatus;
  previousCoachStatus: number;
  phone: string;
}

export interface CoachApplicationListData {
  list: CoachApplicationItem[];
  total: number;
  page: number;
  pageSize: number;
}

export interface CoachAuditListParams {
  status?: CoachAuditStatus | '';
  keyword?: string;
  submitStartDate?: string;
  submitEndDate?: string;
  page: number;
  pageSize: number;
}

export interface CoachAuditStats {
  pendingCount: number;
  todayNewCount: number;
  overdue24hCount: number;
}

export interface CoachApplicationCertificate {
  certId: number;
  certType: string;
  imageUrl: string;
  sortOrder: number;
}

export interface CoachApplicationHistoryItem {
  applicationId: number;
  status: string;
  submittedAt: string;
  approvedAt: string | null;
  approvedBy: number | null;
  rejectionReason: string | null;
}

export interface CoachAuditLogItem {
  logId: number;
  adminId: number;
  action: string;
  fromStatus: number;
  toStatus: number;
  reason: string | null;
  createdAt: string;
}

export interface CoachApplicationDetail {
  coachId: number;
  applicationId: number;
  status: CoachAuditStatus;
  previousCoachStatus: number;
  name: string;
  phone: string;
  gender: Gender;
  age: number;
  email: string;
  wechatQrUrl: string;
  idCardNo: string;
  teachingYears: number;
  totalStudents: number;
  totalHours: number;
  teachingStrokes: string[];
  bio: string;
  referencePrice: number;
  submittedAt: string;
  approvedAt: string | null;
  approvedBy: number | null;
  rejectionReason: string | null;
  certificates: CoachApplicationCertificate[];
  history: CoachApplicationHistoryItem[];
  auditLogs: CoachAuditLogItem[];
}
