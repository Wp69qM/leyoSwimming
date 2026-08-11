import { request } from './request';

export interface PolicyInfo {
  version: string;
  content: string;
  effectiveAt: string;
}

export interface ConsentStatus {
  termsVersion: string | null;
  privacyVersion: string | null;
  requiredTermsVersion: string;
  requiredPrivacyVersion: string;
  needsReconsent: boolean;
}

export function getCurrentTerms() {
  return request<PolicyInfo>({
    url: '/common/policy/current/terms',
    method: 'POST',
    needToken: false,
  });
}

export function getCurrentPrivacy() {
  return request<PolicyInfo>({
    url: '/common/policy/current/privacy',
    method: 'POST',
    needToken: false,
  });
}

export function getConsentStatus() {
  return request<ConsentStatus>({
    url: '/user/consent/status',
    method: 'POST',
  });
}

export function recordConsent(termsVersion: string, privacyVersion: string) {
  return request<unknown>({
    url: '/user/consent/record',
    method: 'POST',
    data: { termsVersion, privacyVersion },
  });
}
