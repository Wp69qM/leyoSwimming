export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T | null;
}

export interface AdminInfo {
  id: number;
  username: string;
  name: string;
  role: string;
}

export interface AdminLoginResponse {
  token: string;
  expiresIn: number;
  admin: AdminInfo;
}

export interface AdminLoginRequest {
  username: string;
  password: string;
}

export interface PageRequest {
  page: number;
  pageSize: number;
}

export interface PageResponse<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface AdminUserListRequest extends PageRequest {
  identity?: number | null;
  status?: number | null;
  profileCompleted?: boolean | null;
  startDate?: string;
  endDate?: string;
  keyword?: string;
}

export interface AdminUserListItem {
  userId: number;
  avatarUrl: string;
  name: string;
  phone: string;
  gender: number;
  age: number;
  identity: number;
  profileCompleted: boolean;
  status: number;
  createdAt: string;
}

export interface AdminUserDetail {
  userId: number;
  avatarUrl: string;
  name: string;
  phone: string;
  gender: number;
  age: number;
  identity: number;
  status: number;
  profileCompleted: boolean;
  source: string;
  hasSwimBasis: boolean;
  swimStrokes: string[];
  swimYears: number;
  personalDesc: string;
  guardianName: string;
  guardianPhone: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface AdminUserAddRequest {
  avatarUrl?: string;
  phone: string;
  name: string;
  gender: number;
  age: number;
  hasSwimBasis?: boolean;
  swimStrokes?: string[];
  swimYears?: number;
  personalDesc?: string;
  guardianName?: string;
  guardianPhone?: string;
}

export interface AdminUserUpdateRequest {
  userId: number;
  avatarUrl?: string;
  name: string;
  gender: number;
  age: number;
  hasSwimBasis: boolean;
  swimStrokes?: string[];
  swimYears?: number;
  personalDesc?: string;
  guardianName?: string;
  guardianPhone?: string;
  version: number;
}

export interface AdminUserBanRequest {
  userId: number;
  reason: string;
}

export interface AdminCoachListRequest extends PageRequest {
  keyword?: string;
  status?: string;
  realtimeStatus?: string;
}

export interface AdminCoachListItem {
  coachId: number;
  name: string;
  gender: string;
  age: number;
  teachingYears: number;
  teachingStrokes: string;
  approvedAt: string;
  tenure: string;
  status: number;
  currentStudentCount: number;
  realtimeStatus: string;
}

export interface AdminCoachCertificate {
  certificateId: number;
  certType: string;
  imageUrl: string;
  sortOrder: number;
}

export interface AdminCoachDetail {
  coachId: number;
  avatarUrl: string;
  phone: string;
  name: string;
  gender: string;
  age: number;
  email: string;
  wechatQrUrl: string;
  idCardNo: string;
  teachingYears: number;
  totalStudents: number;
  totalHours: number;
  teachingStrokes: string;
  bio: string;
  referencePrice: number;
  status: number;
  approvedAt: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  certificates: AdminCoachCertificate[];
}

export interface CoachCertificateItem {
  certType: string;
  imageUrl: string;
}

export interface AdminCoachAddRequest {
  avatarUrl?: string;
  phone: string;
  name: string;
  gender: string;
  age: number;
  email?: string;
  wechatQrUrl?: string;
  idCardNo: string;
  teachingYears: number;
  totalStudents: number;
  totalHours: number;
  teachingStrokes?: string[];
  bio?: string;
  referencePrice: number;
  certificates: CoachCertificateItem[];
}

export interface AdminCoachUpdateRequest extends AdminCoachAddRequest {
  coachId: number;
  version: number;
}

export interface AdminCoachCancelEntryRequest {
  coachId: number;
  reason: string;
}

export interface AdminAccountListRequest extends PageRequest {
  keyword?: string;
  role?: string;
  status?: number | null;
}

export interface AdminAccountListItem {
  adminId: number;
  username: string;
  name: string;
  role: string;
  status: number;
  lastLoginAt: string;
  createdAt: string;
  allowedActions: string[];
}

export interface AdminAccountAuditLog {
  logId: number;
  operatorId: number;
  operatorName: string;
  action: string;
  reason: string;
  ip: string;
  createdAt: string;
}

export interface AdminAccountDetail {
  adminId: number;
  username: string;
  name: string;
  role: string;
  status: number;
  lastLoginAt: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  auditLogs: AdminAccountAuditLog[];
}

export interface AdminAccountAddRequest {
  name: string;
  username: string;
  password: string;
  role: string;
}

export interface AdminAccountUpdateRequest {
  adminId: number;
  name?: string;
  role?: string;
  version: number;
}

export interface AdminAccountToggleStatusRequest {
  adminId: number;
  status: number;
  reason: string;
}

export interface AdminAccountDeleteRequest {
  adminId: number;
  reason: string;
}

export interface AdminAccountResetPasswordRequest {
  adminId: number;
}

export interface AdminAccountResetPasswordResponse {
  tempPassword: string;
}
