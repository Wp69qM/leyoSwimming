import { useEffect, useState, useCallback } from 'react';
import Taro from '@tarojs/taro';
import { View, Text } from '@tarojs/components';
import { useAuthStore } from '@/stores/authStore';
import { APP_NAME, APP_SLOGAN } from '@/constants';
import './index.scss';

export default function Index() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const initialize = useCallback(async () => {
    try {
      const { restoreFromStorage, isLoggedIn, userInfo } =
        useAuthStore.getState();
      restoreFromStorage();

      // 保证启动页至少展示 300ms，避免视觉闪跳
      await new Promise((resolve) => setTimeout(resolve, 300));

      if (!isLoggedIn) {
        Taro.redirectTo({ url: '/pages/login/wechat/index' });
        return;
      }

      if (userInfo && !userInfo.profileCompleted) {
        Taro.redirectTo({ url: '/pages/profile/complete/index' });
        return;
      }

      // 已登录且资料完整：当前 MVP 暂无独立首页，停留在启动页并结束加载态
      setLoading(false);
    } catch {
      setError(true);
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    initialize();
  }, [initialize]);

  function handleRetry() {
    setError(false);
    setLoading(true);
    initialize();
  }

  return (
    <View className='splash'>
      <View className='splash__content'>
        <View className='splash__logo'>
          <View className='splash__logo-ring splash__logo-ring--outer' />
          <View className='splash__logo-ring splash__logo-ring--inner' />
          <View className='splash__logo-dot' />
          <View className='splash__logo-stars'>
            <View className='splash__logo-star splash__logo-star--1' />
            <View className='splash__logo-star splash__logo-star--2' />
            <View className='splash__logo-star splash__logo-star--3' />
          </View>
        </View>

        <Text className='splash__brand'>{APP_NAME}</Text>
        <Text className='splash__slogan'>{APP_SLOGAN}</Text>

        {error ? (
          <View className='splash__error'>
            <Text className='splash__error-text'>网络异常，请重试</Text>
            <View className='splash__retry' onClick={handleRetry}>
              <Text className='splash__retry-text'>重新加载</Text>
            </View>
          </View>
        ) : (
          <View className='splash__loading'>
            <View className='splash__spinner' />
            <Text className='splash__loading-text'>加载中…</Text>
          </View>
        )}
      </View>
    </View>
  );
}
