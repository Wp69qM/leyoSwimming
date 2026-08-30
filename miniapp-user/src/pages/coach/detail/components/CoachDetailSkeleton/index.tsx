import Taro from '@tarojs/taro';
import { View } from '@tarojs/components';

import '../CoachHeader/index.scss';
import './index.scss';

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20;

export function CoachDetailSkeleton() {
  return (
    <View className='coach-detail coach-detail--skeleton'>
      <View
        className='coach-detail__header'
        style={{ paddingTop: `${STATUS_BAR_HEIGHT}px` }}
      >
        <View className='coach-detail__navbar'>
          <View className='coach-detail__back-icon coach-detail__back-icon--skeleton' />
          <View className='coach-detail__title coach-detail__title--skeleton' />
          <View className='coach-detail__navbar-placeholder' />
        </View>
        <View className='coach-detail-header coach-detail-header--skeleton'>
          <View className='coach-detail-header__avatar coach-detail-header__avatar--skeleton' />
          <View className='coach-detail-header__skeleton-line coach-detail-header__skeleton-line--name' />
          <View className='coach-detail-header__skeleton-line coach-detail-header__skeleton-line--meta' />
          <View className='coach-detail-header__skeleton-line coach-detail-header__skeleton-line--stats' />
        </View>
      </View>
      {[1, 2, 3].map((i) => (
        <View key={i} className='coach-detail-card coach-detail-card--skeleton'>
          <View className='coach-detail-card__title coach-detail-card__title--skeleton' />
          <View className='coach-detail-card__body coach-detail-card__body--skeleton' />
        </View>
      ))}
    </View>
  );
}
