export interface PackageListItem {
  id: number;
  name: string;
  packageMode: 'experience' | 'standard';
  teachingType: string;
  totalHours: number;
  durationMinutes: number;
  validDays: number;
  originalPrice: string;
  price: string;
  tags: string[];
  imageUrl: string | null;
  refundPolicySummary: string | null;
}

export interface PackageListData {
  items: PackageListItem[];
  total: number;
  page: number;
  pageSize: number;
}

export interface PackageDetailCoach {
  coachId: number;
  name: string;
  avatarUrl: string | null;
  rating: string;
  teachingYears: number;
  totalStudents: number;
  referencePrice: string;
  teachingStrokes: string[];
  status?: number;
}

export interface PackageDetail {
  id: number;
  name: string;
  packageMode: 'experience' | 'standard' | 'custom';
  teachingType: string;
  totalHours: number;
  durationMinutes: number;
  validDays: number;
  originalPrice: string;
  price: string;
  refundEnabled: boolean;
  refundRatio: string;
  refundValidDays: number;
  refundPolicySummary: string | null;
  tags: string[];
  description: string | null;
  images: string[];
  applicableCoaches: PackageDetailCoach[];
}

export interface CustomPackageConfig {
  minHours: number;
  maxHours: number;
  defaultValidDays: number;
  allowedValidDays: number[];
}

export interface UserActivePackage {
  id: number;
  coachId: number;
  coachName: string;
  status: UserPackageStatus;
}

export type UserPackageStatus =
  'active' | 'exhausted' | 'expired' | 'refunded' | 'frozen';

export interface UserPackageListItem {
  packageId: number;
  packageName: string;
  packageMode: 'standard' | 'experience' | 'custom';
  coachName: string;
  teachingType: string;
  durationMinutes: number;
  validDays: number;
  totalHours: number;
  consumedCount: number;
  availableCount: number;
  paidAmount: string;
  originalPrice: string;
  status: UserPackageStatus;
  frozenReason: string | null;
  refundEnabled: boolean;
  refundValidDays: number;
  expireAt: string;
  createdAt: string;
  canRefund: boolean;
}

export interface UserPackageDetail {
  packageId: number;
  packageName: string;
  packageMode: 'standard' | 'experience' | 'custom';
  coachName: string;
  teachingType: string;
  strokeIds: number[];
  durationMinutes: number;
  validDays: number;
  totalHours: number;
  consumedCount: number;
  availableCount: number;
  paidAmount: string;
  originalPrice: string;
  refundEnabled: boolean;
  refundValidDays: number;
  refundRatio: string;
  refundRuleText: string;
  status: UserPackageStatus;
  frozenReason: string | null;
  expireAt: string;
  createdAt: string;
  canRefund: boolean;
}

export type RefundReason = '教练原因' | '个人原因' | '平台原因';

export const REFUND_REASONS: RefundReason[] = [
  '教练原因',
  '个人原因',
  '平台原因',
];

export interface RefundApplyRequest {
  packageId: number;
  reason: string;
}

export interface RefundApplyResponse {
  orderId: number;
  orderNo: string;
  refundAmount: string;
}
