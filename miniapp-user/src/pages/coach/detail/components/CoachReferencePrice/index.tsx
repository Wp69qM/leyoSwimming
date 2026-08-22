import { View, Text } from '@tarojs/components';
import { Icon } from '@/components/common/Icon';

import './index.scss';

type CoachReferencePriceProps = {
  price: string;
};

export function CoachReferencePrice({ price }: CoachReferencePriceProps) {
  const hasPrice = price && Number(price) > 0;
  return (
    <View className='coach-detail-card coach-detail-card--row'>
      <View className='coach-detail-price__icon'>
        <Icon name='stack' className='coach-detail-price__icon-inner' />
      </View>
      <View className='coach-detail-price__info'>
        <Text className='coach-detail-card__title'>参考单价</Text>
        {hasPrice ? (
          <Text className='coach-detail-price__value'>¥{price}/节</Text>
        ) : (
          <Text className='coach-detail-price__empty'>
            教练尚未设置参考单价
          </Text>
        )}
      </View>
    </View>
  );
}
