import { useEffect, useState } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Button } from '@tarojs/components';
import { handleBusinessError } from '@/api/request';
import './PolicyView.scss';

export type PolicyFetcher = () => Promise<{
  version: string;
  content: string;
  effectiveAt?: string;
}>;

interface PolicyViewProps {
  title: string;
  fetchPolicy: PolicyFetcher;
  emptyText: string;
}

const SYSTEM_INFO = Taro.getSystemInfoSync();
const STATUS_BAR_HEIGHT = SYSTEM_INFO.statusBarHeight || 0;
const NAV_BAR_HEIGHT = 44;

export function PolicyView({ title, fetchPolicy, emptyText }: PolicyViewProps) {
  const [policy, setPolicy] = useState<{ version: string; content: string } | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError('');
      try {
        const res = await fetchPolicy();
        if (!cancelled) {
          setPolicy(res);
        }
      } catch (err) {
        if (!cancelled) {
          setError(handleBusinessError(err));
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
  }, [fetchPolicy, retryKey]);

  function handleBack() {
    Taro.navigateBack();
  }

  function handleRetry() {
    setRetryKey((k) => k + 1);
  }

  function renderVersion() {
    if (!policy?.version) return null;
    const label = policy.version.startsWith('v')
      ? policy.version
      : `v${policy.version}`;
    return (
      <View className='policy-view__version'>
        <Text className='policy-view__version-text'>版本：{label}</Text>
      </View>
    );
  }

  return (
    <View className='policy-view'>
      <View
        className='policy-view__navbar'
        style={{ paddingTop: `${STATUS_BAR_HEIGHT}px` }}
      >
        <View
          className='policy-view__navbar-inner'
          style={{ height: `${NAV_BAR_HEIGHT}px` }}
        >
          <View className='policy-view__navbar-back' onClick={handleBack}>
            <Text className='policy-view__navbar-back-icon'>&#8249;</Text>
          </View>
          <Text className='policy-view__navbar-title'>{title}</Text>
        </View>
      </View>

      <View
        className='policy-view__content'
        style={{ paddingTop: `${STATUS_BAR_HEIGHT + NAV_BAR_HEIGHT}px` }}
      >
        {loading && (
          <View className='policy-view__status'>
            <View className='policy-view__loading-icon' />
            <Text className='policy-view__status-text'>加载中…</Text>
          </View>
        )}

        {!loading && error && (
          <View className='policy-view__status'>
            <View className='policy-view__error-icon' />
            <Text className='policy-view__status-text'>内容加载失败，请重试</Text>
            <Button
              className='policy-view__retry'
              onClick={handleRetry}
            >
              重试
            </Button>
          </View>
        )}

        {!loading && !error && !policy && (
          <View className='policy-view__status'>
            <View className='policy-view__empty-icon' />
            <Text className='policy-view__status-text'>{emptyText}</Text>
          </View>
        )}

        {!loading && !error && policy && (
          <>
            {renderVersion()}
            <View className='policy-view__card'>
              <Text className='policy-view__title'>{title}</Text>
              <Text className='policy-view__text'>{policy.content}</Text>
            </View>
          </>
        )}
      </View>
    </View>
  );
}
