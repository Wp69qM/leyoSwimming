import { useEffect, useState } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, ScrollView } from '@tarojs/components';
import { getCurrentPrivacy, getCurrentTerms, type PolicyInfo } from '@/api/policy';
import './index.scss';

export default function ProtocolPage() {
  const [policy, setPolicy] = useState<PolicyInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { type } = Taro.getCurrentInstance().router?.params || {};
  const title = type === 'privacy' ? '隐私协议' : '用户须知';

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const res = type === 'privacy' ? await getCurrentPrivacy() : await getCurrentTerms();
        if (cancelled) return;
        setPolicy(res);
      } catch {
        if (cancelled) return;
        setError('协议加载失败，请返回重试');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [type]);

  function handleBack() {
    Taro.navigateBack();
  }

  return (
    <View className='protocol-page'>
      <View className='protocol-page__header'>
        <Text className='protocol-page__back' onClick={handleBack}>
          ← 返回
        </Text>
        <Text className='protocol-page__title'>{title}</Text>
        <Text className='protocol-page__placeholder' />
      </View>
      <ScrollView className='protocol-page__body' scrollY>
        {loading && (
          <View className='protocol-page__status'>
            <Text className='protocol-page__status-text'>加载中…</Text>
          </View>
        )}
        {error && (
          <View className='protocol-page__status'>
            <Text className='protocol-page__status-text'>{error}</Text>
          </View>
        )}
        {policy && (
          <View>
            <Text className='protocol-page__version'>版本：{policy.version}</Text>
            <Text className='protocol-page__content'>{policy.content}</Text>
          </View>
        )}
      </ScrollView>
    </View>
  );
}
