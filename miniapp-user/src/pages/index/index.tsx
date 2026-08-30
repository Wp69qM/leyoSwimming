import { useEffect, useState, useCallback } from 'react';
import Taro from '@tarojs/taro';
import { View, Image } from '@tarojs/components';
import { useAuthStore } from '@/stores/authStore';
import { APP_NAME } from '@/constants';
import logoRibbon from '@/assets/logo-ribbon.svg';
import './index.scss';

export default function Index() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const initialize = useCallback(async () => {
    try {
      const { restoreFromStorage } = useAuthStore.getState();
      restoreFromStorage();

      // 保证启动页至少展示 300ms，避免视觉闪跳
      await new Promise((resolve) => setTimeout(resolve, 300));

      const { isLoggedIn, userInfo } = useAuthStore.getState();

      // 用户端首页支持游客模式；仅已登录但资料未完善时跳转
      if (isLoggedIn && userInfo && !userInfo.profileCompleted) {
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

  if (loading) {
    return (
      <View className='splash'>
        <View className='splash__content'>
          <View className='splash__logo'>
            <Image
              className='splash__logo-icon'
              src={logoRibbon}
              mode='aspectFit'
            />
          </View>
          <View className='splash__brand'>{APP_NAME}</View>
          <View className='splash__slogan'>专业游泳约课，从这里开始</View>
          <View className='splash__loading'>
            <View className='splash__loading-dot' />
            <View className='splash__loading-text'>加载中…</View>
          </View>
        </View>
      </View>
    );
  }

  if (error) {
    return (
      <View className='splash'>
        <View className='splash__content'>
          <View className='splash__logo'>
            <Image
              className='splash__logo-icon'
              src={logoRibbon}
              mode='aspectFit'
            />
          </View>
          <View className='splash__brand'>{APP_NAME}</View>
          <View className='splash__slogan'>专业游泳约课，从这里开始</View>
          <View className='splash__error'>
            <View className='splash__error-illustration' />
            <View className='splash__error-text'>网络异常，请重试</View>
            <View className='splash__retry' onClick={handleRetry}>
              <View className='splash__retry-text'>重新加载</View>
            </View>
          </View>
        </View>
      </View>
    );
  }

  return (
    <View className='splash'>
      <View className='splash__content'>
        <View
          className={`splash__brand-wrap ${error ? 'splash__brand-wrap--dimmed' : ''}`}
        >
          <View className='splash__logo'>
            <Image
              className='splash__logo-icon'
              src={logoRibbon}
              mode='aspectFit'
            />
          </View>
          <View className='splash__brand'>{APP_NAME}</View>
          <View className='splash__slogan'>专业游泳约课，从这里开始</View>
        </View>
      </View>
    </View>
  );
}
