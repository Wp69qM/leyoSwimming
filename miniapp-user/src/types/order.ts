export interface AgreementVersions {
  userNotice: string;
  health: string;
  disclaimer: string;
}

export interface CreateTrialOrderParams {
  coachId: number;
}

export interface CreateFormalOrderParams {
  coachId: number;
  packageId: number;
  agreementVersions: AgreementVersions;
  guardianPhone?: string;
  hours?: number;
  validDays?: number;
  strokeIds?: number[];
}

export interface OrderCreateResult {
  orderId: number;
  orderNo: string;
  amount: string;
  expireAt: string;
}

export type OrderStatus =
  | 'pending_payment'
  | 'paid'
  | 'cancelled'
  | 'refund_pending'
  | 'refund_processing'
  | 'refunded'
  | 'rejected'
  | 'dispute_processing';

export interface OrderDetail {
  orderId: number;
  orderNo: string;
  type: 'purchase' | 'refund';
  status: OrderStatus;
  amount: string;
  paidAmount: string;
  paidAt?: string;
  expireAt: string;
  packageId?: number;
  packageName: string;
  packageMode?: 'standard' | 'experience' | 'custom';
  originalPrice: string;
  coachName: string;
  teachingType: string;
  totalHours: number;
  durationMinutes: number;
  validDays: number;
  createdAt: string;
  strokeNames?: string[];
  coachAvatar?: string;
  packageStatus?: 'active' | 'exhausted' | 'expired' | 'frozen' | 'refunded';
  availableHours?: number;
  reservedHours?: number;
  usedHours?: number;
  packageExpireAt?: string;
  refundAmount?: string;
  refundReason?: string;
  refundPaidAt?: string;
  channel?: 'wechat' | 'alipay';
  refundEnabled?: boolean;
  refundRatio?: number;
  refundValidDays?: number;
  rejectedReason?: string;
  frozenReason?: string;
}

export interface OrderPayResult {
  paymentId: number;
  channelTradeNo: string;
  status: string;
}

export interface PaymentCallbackResult {
  code: string;
}

export interface PaymentCallbackParams {
  channel: number;
  orderId: number;
  channelTradeNo: string;
  amount: string;
  success: boolean;
}

export interface Page<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface OrderListItem {
  orderId: number;
  orderNo: string;
  status: OrderStatus;
  amount: string;
  paidAmount: string;
  expireAt: string;
  packageName: string;
  packageMode?: 'standard' | 'experience' | 'custom';
  coachName: string;
  teachingType: string;
  totalHours: number;
  createdAt: string;
}

export type OrderTab =
  'all' | 'pending_payment' | 'completed' | 'refunding' | 'cancelled';
