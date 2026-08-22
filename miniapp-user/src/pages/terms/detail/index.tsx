import { useEffect, useMemo, useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { Icon } from '@/components/common/Icon';

import './index.scss';

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20;

const TYPE_TITLE_MAP: Record<string, string> = {
  'user-notice': '用户须知',
  health: '健康承诺书',
  disclaimer: '免责协议',
};

export default function TermsDetailPage() {
  const [type, setType] = useState<string>('user-notice');

  useEffect(() => {
    const params = Taro.getCurrentInstance().router?.params ?? {};
    const rawType = params.type ?? 'user-notice';
    setType(String(rawType));
  }, []);

  const title = useMemo(() => TYPE_TITLE_MAP[type] ?? '协议详情', [type]);

  function navigateBack() {
    void Taro.navigateBack();
  }

  return (
    <View className='terms-detail-page'>
      <View className='terms-detail-page__header'>
        <View
          className='terms-detail-page__status-bar'
          style={{ height: `${STATUS_BAR_HEIGHT}px` }}
        />
        <View className='terms-detail-page__navbar'>
          <View className='terms-detail-page__back' onClick={navigateBack}>
            <Icon name='arrow-left' className='terms-detail-page__back-icon' />
          </View>
          <Text className='terms-detail-page__title'>{title}</Text>
          <View className='terms-detail-page__navbar-placeholder' />
        </View>
      </View>

      <View className='terms-detail-page__content'>
        <Text className='terms-detail-page__hint'>协议内容开发中</Text>
      </View>
    </View>
  );
}
