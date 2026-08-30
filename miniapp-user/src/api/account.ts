import { request } from './request';

export interface CancelCheckResult {
  canCancel: boolean;
  checks: {
    noActivePackage: boolean;
    noPendingOrder: boolean;
    noOngoingBooking: boolean;
  };
}

export interface CancelAccountResult {
  cancelled: boolean;
  anonymousAfter: string;
}

export function checkAccountCancel() {
  return request<CancelCheckResult>({
    url: '/user/account/cancel-check',
    method: 'POST',
  });
}

export function cancelAccount() {
  return request<CancelAccountResult>({
    url: '/user/account/cancel',
    method: 'POST',
  });
}
