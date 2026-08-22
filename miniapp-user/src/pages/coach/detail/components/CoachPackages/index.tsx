import { View, Text, Image } from '@tarojs/components';
import { Icon } from '@/components/common/Icon';
import type { CoachDetailPackage } from '@/types/coach';

import './index.scss';

type CoachPackagesProps = {
  packages: CoachDetailPackage[];
  canPurchase: boolean;
  isBoundOtherCoach: boolean;
  referencePrice: string;
  onPackageClick: (pkg: CoachDetailPackage) => void;
  onMore: () => void;
  onCustom: () => void;
};

export function CoachPackages({
  packages,
  canPurchase,
  isBoundOtherCoach,
  referencePrice,
  onPackageClick,
  onMore,
  onCustom,
}: CoachPackagesProps) {
  const hasPrice = referencePrice && Number(referencePrice) > 0;
  const showCustom = hasPrice && !isBoundOtherCoach;
  const displayPackages = packages.slice(0, 3);
  const experiencePackage = displayPackages.find(
    (p) => p.packageMode === 'experience'
  );
  const standardPackages = displayPackages.filter(
    (p) => p.packageMode === 'standard'
  );

  return (
    <View className='coach-detail-card'>
      <View className='coach-detail-card__header'>
        <Text className='coach-detail-card__title'>可选套餐</Text>
        {packages.length > 3 && (
          <View className='coach-detail-card__more' onClick={onMore}>
            <Text className='coach-detail-card__more-text'>更多套餐</Text>
            <Icon name='arrow-right' className='coach-detail-card__more-icon' />
          </View>
        )}
      </View>

      {experiencePackage && (
        <View
          className={`coach-detail-package coach-detail-package--experience ${canPurchase ? '' : 'coach-detail-package--disabled'}`}
          onClick={
            canPurchase ? () => onPackageClick(experiencePackage) : undefined
          }
        >
          <View className='coach-detail-package__info'>
            <Text className='coach-detail-package__name'>新人体验课</Text>
            <Text className='coach-detail-package__desc'>
              {experiencePackage.totalHours} 节 · 30 天有效
            </Text>
          </View>
          <View className='coach-detail-package__action'>
            <Text className='coach-detail-package__price'>
              ¥{experiencePackage.price}起
            </Text>
            <View className='coach-detail-package__btn'>
              <Text className='coach-detail-package__btn-text'>立即购买</Text>
            </View>
          </View>
        </View>
      )}

      {standardPackages.map((pkg) => (
        <View
          key={pkg.id}
          className={`coach-detail-package ${canPurchase ? '' : 'coach-detail-package--disabled'}`}
          onClick={canPurchase ? () => onPackageClick(pkg) : undefined}
        >
          <Image
            className='coach-detail-package__image'
            src={pkg.imageUrl || ''}
            mode='aspectFill'
          />
          <View className='coach-detail-package__info'>
            <Text className='coach-detail-package__name'>{pkg.name}</Text>
            <Text className='coach-detail-package__desc'>
              {pkg.totalHours} 节 · 单价 ¥{unitPrice(pkg.price, pkg.totalHours)}
              /节
            </Text>
          </View>
          <View className='coach-detail-package__price-wrap'>
            <Text className='coach-detail-package__price'>¥{pkg.price}</Text>
            <Icon name='arrow-right' className='coach-detail-package__arrow' />
          </View>
        </View>
      ))}

      {showCustom && (
        <View className='coach-detail-custom' onClick={onCustom}>
          <View className='coach-detail-custom__icon'>
            <Icon name='stack' className='coach-detail-custom__icon-inner' />
          </View>
          <View className='coach-detail-custom__info'>
            <Text className='coach-detail-custom__title'>自定义课时</Text>
            <Text className='coach-detail-custom__desc'>
              按参考单价灵活选择
            </Text>
          </View>
          <View className='coach-detail-custom__price-wrap'>
            <Text className='coach-detail-custom__price'>
              ¥{referencePrice}/节
            </Text>
            <Icon name='arrow-right' className='coach-detail-custom__arrow' />
          </View>
        </View>
      )}

      {!experiencePackage && standardPackages.length === 0 && !showCustom && (
        <Text className='coach-detail-card__empty'>暂无可选套餐</Text>
      )}
    </View>
  );
}

function unitPrice(price: string, hours: number): string {
  const total = Number(price);
  if (!total || hours <= 0) return '0';
  return (total / hours).toFixed(0);
}
