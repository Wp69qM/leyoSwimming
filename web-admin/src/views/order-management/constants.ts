import type { OrderType, OrderStatus, PackageStatus } from '@/types/api';

export const orderTypeMap: Record<
  OrderType,
  { label: string; color: string; bgColor: string }
> = {
  purchase: { label: '购买订单', color: '#1890FF', bgColor: '#E6F7FF' },
  refund: { label: '退款订单', color: '#FA541C', bgColor: '#FFF2E8' },
};

export const orderStatusMap: Record<
  OrderStatus,
  { label: string; color: string; bgColor: string }
> = {
  pending_payment: { label: '待支付', color: '#FAAD14', bgColor: '#FFFBE6' },
  paid: { label: '已支付', color: '#52C41A', bgColor: '#F6FFED' },
  cancelled: { label: '已取消', color: '#8C8C8C', bgColor: '#F5F5F5' },
  refund_pending: { label: '退款审批中', color: '#FAAD14', bgColor: '#FFFBE6' },
  refund_processing: {
    label: '退款处理中',
    color: '#1890FF',
    bgColor: '#E6F7FF',
  },
  refunded: { label: '已退款', color: '#52C41A', bgColor: '#F6FFED' },
  rejected: { label: '退款被拒', color: '#FF4D4F', bgColor: '#FFF1F0' },
  dispute_processing: {
    label: '争议处理中',
    color: '#722ED1',
    bgColor: '#F9F0FF',
  },
};

export const packageStatusMap: Record<
  PackageStatus,
  { label: string; color: string; bgColor: string }
> = {
  active: { label: '生效中', color: '#52C41A', bgColor: '#F6FFED' },
  exhausted: { label: '已耗尽', color: '#8C8C8C', bgColor: '#F5F5F5' },
  expired: { label: '已过期', color: '#FAAD14', bgColor: '#FFFBE6' },
  frozen: { label: '已冻结', color: '#722ED1', bgColor: '#F9F0FF' },
  refunded: { label: '已退款', color: '#FF4D4F', bgColor: '#FFF1F0' },
};

export const paymentMethodMap: Record<string, string> = {
  wechat: '微信支付',
  alipay: '支付宝',
  mock: 'Mock支付',
};

export function formatOrderAmount(
  amount: string | number | null | undefined
): string {
  if (amount === undefined || amount === null || amount === '') return '-';
  const num = Number(amount);
  if (Number.isNaN(num)) return '-';
  return `¥${num.toFixed(2)}`;
}

export function formatPaymentMethod(method: string | null | undefined): string {
  if (!method) return '-';
  return paymentMethodMap[method] || method;
}
