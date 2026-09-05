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
  gender?: number | null;
  minAge?: number | null;
  maxAge?: number | null;
  source?: string | null;
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
  lastLoginAt?: string;
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
  referencePrice: number;
}

export interface AdminCoachCertificate {
  certificateId: number;
  certType: string;
  imageUrl: string;
  sortOrder: number;
}

export interface AdminCoachAuditLog {
  logId: number;
  adminId: number;
  action: string;
  fromStatus?: string;
  toStatus?: string;
  reason?: string;
  createdAt: string;
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
  auditLogs: AdminCoachAuditLog[];
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
  phone: string;
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
  phone: string;
  role: string;
  status: number;
  lastLoginAt: string;
  lastLoginIp?: string;
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

export type OrderType = 'purchase' | 'refund';
export type OrderStatus =
  | 'pending_payment'
  | 'paid'
  | 'cancelled'
  | 'refund_pending'
  | 'refund_processing'
  | 'refunded'
  | 'rejected'
  | 'dispute_processing';

export interface AdminOrderListRequest extends PageRequest {
  type?: OrderType | '';
  status?: OrderStatus | '';
  paymentMethod?: string;
  startDate?: string;
  endDate?: string;
  keyword?: string;
}

export interface AdminOrderListItem {
  orderId: number;
  orderNo: string;
  type: OrderType;
  status: OrderStatus;
  userId: number;
  userName: string;
  coachId: number | null;
  coachName: string | null;
  packageId: number | null;
  originalAmount: string;
  discountAmount: string;
  paidAmount: string;
  calculatedRefundAmount: string | null;
  paymentMethod: string | null;
  reason: string | null;
  createdAt: string;
}

export interface AdminOrderListResponse {
  list: AdminOrderListItem[];
  total: number;
  page: number;
  pageSize: number;
}

export interface AdminOrderStatusLog {
  status: string;
  time: string;
  description: string;
}

export interface AdminOrderPackageSnapshot {
  packageId: number;
  packageNo: string;
  status: PackageStatus;
  totalHours: number;
  availableCount: number;
  expireAt: string;
}

export interface AdminOrderDetail {
  orderId: number;
  orderNo: string;
  type: OrderType;
  status: OrderStatus;
  userId: number;
  userName: string;
  userPhone: string | null;
  coachId: number | null;
  coachName: string | null;
  teachingType: string | null;
  packageId: number | null;
  purchaseOrderId: number | null;
  purchaseOrderNo: string | null;
  originalAmount: string;
  discountAmount: string;
  paidAmount: string;
  calculatedRefundAmount: string | null;
  paymentMethod: string | null;
  channelTradeNo: string | null;
  reason: string | null;
  rejectedReason: string | null;
  adjustReason: string | null;
  approvedBy: number | null;
  approvedAt: string | null;
  refundedAt: string | null;
  paidAt: string | null;
  createdAt: string;
  statusTimeline: AdminOrderStatusLog[];
  packageSnapshot: AdminOrderPackageSnapshot | null;
}

export interface AdminOrderDetailRequest {
  orderId: number;
}

export interface AdminOrderRefundApproveRequest {
  orderId: number;
  refundAmount: string;
  adjustReason?: string;
}

export interface AdminOrderRefundRejectRequest {
  orderId: number;
  rejectedReason: string;
}

export type PackageStatus =
  'active' | 'exhausted' | 'expired' | 'frozen' | 'refunded';
export type PackageMode = 'standard' | 'experience' | 'custom';

export interface AdminPackageListRequest extends PageRequest {
  status?: PackageStatus | '';
  teachingType?: string;
  startExpireAt?: string;
  endExpireAt?: string;
  keyword?: string;
}

export interface AdminPackageListItem {
  packageId: number;
  packageNo: string;
  userId: number;
  userName: string;
  coachId: number | null;
  coachName: string | null;
  packageMode: PackageMode;
  teachingType: string;
  status: PackageStatus;
  totalHours: number;
  consumedCount: number;
  reservedCount: number;
  availableCount: number;
  expireAt: string;
  createdAt: string;
  refundEnabled: boolean;
  refundValidDays: number;
  refundRatio: string;
  refundAmount: string;
  version: number;
}

export interface AdminPackageListResponse {
  list: AdminPackageListItem[];
  total: number;
  page: number;
  pageSize: number;
}

export interface AdminPackageDetail {
  packageId: number;
  packageNo: string;
  packageName: string;
  userId: number;
  userName: string;
  coachId: number | null;
  coachName: string | null;
  packageMode: PackageMode;
  teachingType: string;
  strokeIds: number[];
  durationMinutes: number;
  validDays: number;
  status: PackageStatus;
  frozenReason: string | null;
  totalHours: number;
  consumedCount: number;
  reservedCount: number;
  availableCount: number;
  pricePerHour: string;
  paidAmount: string;
  originalPrice: string;
  refundEnabled: boolean;
  refundRatio: string;
  refundValidDays: number;
  refundAmount: string;
  expireAt: string;
  exhaustedAt: string | null;
  refundedAt: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface AdminPackageDetailRequest {
  packageId: number;
}

export interface AdminPackageFreezeRequest {
  packageId: number;
  reasonDetail: string;
  version: number;
}

export interface AdminPackageUnfreezeRequest {
  packageId: number;
  version: number;
}

export interface AdminPackageExtendRequest {
  packageId: number;
  newExpireAt: string;
  reason: string;
  version: number;
}

export interface AdminPackageRefundRequest {
  packageId: number;
  reason: string;
  refundAmount?: string;
  adjustReason?: string;
  version: number;
}

export interface AdminPackageOperationResponse {
  message: string;
  orderNo?: string;
}

export type PackageTemplateStatus = 'active' | 'inactive';
export type PackageTemplateMode = 'standard' | 'experience';

export interface AdminPackageTemplateListRequest extends PageRequest {
  packageMode?: PackageTemplateMode | '';
  status?: PackageTemplateStatus | '';
  keyword?: string;
}

export interface AdminPackageTemplateListItem {
  packageTemplateId: number;
  name: string;
  packageMode: PackageTemplateMode;
  teachingType: string;
  coachIds: number[];
  coachNames: string[];
  totalHours: number;
  durationMinutes: number;
  validDays: number;
  originalPrice: number;
  price: number;
  refundEnabled: boolean;
  status: PackageTemplateStatus;
  createdAt: string;
}

export interface AdminPackageTemplateListResponse {
  items: AdminPackageTemplateListItem[];
  total: number;
  page: number;
  pageSize: number;
}

export interface CoachBrief {
  coachId: number;
  name: string;
  avatarUrl: string;
}

export interface AdminPackageTemplateDetail {
  packageTemplateId: number;
  name: string;
  packageMode: PackageTemplateMode;
  teachingType: string;
  coachIds: number[];
  coachList: CoachBrief[];
  strokeIds: number[];
  totalHours: number;
  durationMinutes: number;
  validDays: number;
  originalPrice: number;
  price: number;
  refundEnabled: boolean;
  refundRatio: number;
  refundValidDays: number;
  tags: string[];
  description: string;
  images: string[];
  status: PackageTemplateStatus;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface AdminPackageTemplateAddRequest {
  name: string;
  packageMode: PackageTemplateMode;
  coachIds: number[];
  teachingType: string;
  strokeIds: number[];
  totalHours: number;
  durationMinutes: number;
  validDays: number;
  originalPrice: number;
  price: number;
  refundEnabled: boolean;
  refundRatio?: number;
  refundValidDays?: number;
  tags?: string[];
  description?: string;
  images?: string[];
  idempotencyKey?: string;
}

export interface AdminPackageTemplateUpdateRequest {
  packageTemplateId: number;
  name?: string;
  packageMode?: PackageTemplateMode;
  coachIds: number[];
  teachingType?: string;
  strokeIds?: number[];
  totalHours?: number;
  durationMinutes?: number;
  validDays?: number;
  originalPrice?: number;
  price?: number;
  refundEnabled?: boolean;
  refundRatio?: number;
  refundValidDays?: number;
  tags?: string[];
  description?: string;
  images?: string[];
  version: number;
}

export interface AdminPackageTemplateToggleRequest {
  packageTemplateId: number;
}

export interface AdminPackageTemplateCustomConfigRequest {
  minHours: number;
  maxHours: number;
  defaultValidDays: number;
}

export interface AdminPackageTemplateCustomConfigResponse {
  configId: number;
  minHours: number;
  maxHours: number;
  defaultValidDays: number;
}

export type KnowledgeCategory = 'safety' | 'technique' | 'emergency' | 'other';
export type KnowledgeContentType = 'text' | 'markdown';

export interface KnowledgeAddRequest {
  title: string;
  category: KnowledgeCategory;
  contentType: KnowledgeContentType;
  content: string;
}

export interface KnowledgeUploadRequest {
  title: string;
  category: KnowledgeCategory;
  file: File;
}

export interface KnowledgeListRequest extends PageRequest {
  category?: KnowledgeCategory;
  status?: number | null;
  keyword?: string;
}

export interface KnowledgeListItem {
  documentId: number;
  title: string;
  category: KnowledgeCategory;
  contentType: KnowledgeContentType;
  sourceType: KnowledgeSourceType;
  status: number;
  createdBy: string;
  createdAt: string;
}

export interface KnowledgeListResponse {
  list: KnowledgeListItem[];
  total: number;
  page: number;
  pageSize: number;
}

export interface KnowledgeToggleRequest {
  documentId: number;
  status: number;
}

export interface KnowledgeDeleteRequest {
  documentId: number;
}

export interface KnowledgeDetailRequest {
  documentId: number;
}

export type KnowledgeSourceType = 'manual' | 'file';

export interface KnowledgeDetail {
  documentId: number;
  title: string;
  category: KnowledgeCategory;
  contentType: KnowledgeContentType;
  sourceType: KnowledgeSourceType;
  content: string;
  status: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}
