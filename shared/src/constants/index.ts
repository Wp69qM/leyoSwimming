/**
 * 业务常量与枚举键（不包含展示文案）。
 */

export const API_PREFIX = '/api/v1';

export const UserStatus = {
  ACTIVE: 1,
  BANNED: 2,
  DELETED: 3,
} as const;

export const CoachStatus = {
  PENDING: 0,
  ACTIVE: 1,
  REJECTED: 2,
  RESIGNED: 3,
} as const;

export const OrderStatus = {
  PENDING_PAYMENT: 1,
  PAID: 2,
  CANCELED: 3,
  REFUND_APPROVING: 4,
  DISPUTE_REFUND: 5,
  REFUNDED: 6,
  REFUND_REJECTED: 7,
} as const;
