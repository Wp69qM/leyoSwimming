import type {
  CreateFormalOrderParams,
  CreateTrialOrderParams,
  OrderCreateResult,
  OrderDetail,
  OrderListItem,
  OrderPayResult,
  Page,
  PaymentCallbackParams,
  PaymentCallbackResult,
} from '@/types/order';

import { request } from './request';

export function createTrialOrder(
  params: CreateTrialOrderParams
): Promise<OrderCreateResult> {
  return request<OrderCreateResult>({
    url: '/order/trial',
    method: 'POST',
    data: params,
  });
}

export function createFormalOrder(
  params: CreateFormalOrderParams
): Promise<OrderCreateResult> {
  return request<OrderCreateResult>({
    url: '/order/formal',
    method: 'POST',
    data: params,
  });
}

export function fetchOrderDetail(orderId: number): Promise<OrderDetail> {
  return request<OrderDetail>({
    url: '/order/detail',
    method: 'POST',
    data: { orderId },
  });
}

export function payOrder(
  orderId: number,
  channel: number
): Promise<OrderPayResult> {
  return request<OrderPayResult>({
    url: '/order/pay',
    method: 'POST',
    data: { orderId, channel },
  });
}

export function mockPaymentCallback(
  params: PaymentCallbackParams
): Promise<PaymentCallbackResult> {
  return request<PaymentCallbackResult>({
    url: '/payment/mock-callback',
    method: 'POST',
    data: params,
  });
}

export function fetchOrderList(
  tab: string,
  page: number,
  size: number
): Promise<Page<OrderListItem>> {
  return request<Page<OrderListItem>>({
    url: '/order/list',
    method: 'POST',
    data: { tab, page, size },
  });
}
