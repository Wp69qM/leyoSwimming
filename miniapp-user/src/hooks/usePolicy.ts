import { useEffect, useState } from 'react';
import { getCurrentPrivacy, getCurrentTerms, type PolicyInfo } from '@/api/policy';

export interface PolicyVersions {
  termsVersion: string;
  privacyVersion: string;
  termsContent: string;
  privacyContent: string;
  loading: boolean;
  error: string | null;
}

export function usePolicyVersions(): PolicyVersions {
  const [terms, setTerms] = useState<PolicyInfo | null>(null);
  const [privacy, setPrivacy] = useState<PolicyInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const [termsRes, privacyRes] = await Promise.all([
          getCurrentTerms(),
          getCurrentPrivacy(),
        ]);
        if (!cancelled) {
          setTerms(termsRes);
          setPrivacy(privacyRes);
        }
      } catch (e) {
        if (!cancelled) {
          setError('协议加载失败');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  return {
    termsVersion: terms?.version || '',
    privacyVersion: privacy?.version || '',
    termsContent: terms?.content || '',
    privacyContent: privacy?.content || '',
    loading,
    error,
  };
}
