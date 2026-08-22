import { useEffect, useMemo, useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { fetchPackageList } from '@/api/package';
import { handleBusinessError } from '@/api/request';
import { Icon } from '@/components/common/Icon';
import type { PackageListItem } from '@/types/package';
import './index.scss';

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20;

const PAGE_PATHS = {
  detail: '/pages/package/detail/index',
  custom: '/pages/package/custom/index',
};

export default function PackageListPage() {
  const [items, setItems] = useState<PackageListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadPackages();
  }, []);

  async function loadPackages() {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchPackageList(1, 50);
      setItems(data.items);
    } catch (err) {
      setError(handleBusinessError(err));
    } finally {
      setLoading(false);
    }
  }

  const experiencePackage = useMemo(
    () => items.find((item) => item.packageMode === 'experience'),
    [items]
  );

  const standardPackages = useMemo(
    () => items.filter((item) => item.packageMode === 'standard'),
    [items]
  );

  function navigateToDetail(item: PackageListItem) {
    void Taro.navigateTo({
      url: `${PAGE_PATHS.detail}?id=${item.id}`,
    });
  }

  function navigateToCustom() {
    void Taro.navigateTo({ url: PAGE_PATHS.custom });
  }

  function navigateBack() {
    void Taro.navigateBack();
  }

  if (loading) {
    return <PackageListSkeleton />;
  }

  if (error) {
    return (
      <View className='package-list-error'>
        <Text className='package-list-error__text'>{error}</Text>
        <Text className='package-list-error__retry' onClick={loadPackages}>
          点击重试
        </Text>
      </View>
    );
  }

  if (items.length === 0) {
    return (
      <View className='package-list-empty'>
        <StatusBarAndNavBar title='全部套餐' onBack={navigateBack} />
        <View className='package-list-empty__content'>
          <Text className='package-list-empty__title'>暂无套餐</Text>
          <Text className='package-list-empty__desc'>敬请期待更多精彩课程</Text>
        </View>
      </View>
    );
  }

  return (
    <View className='package-list'>
      <StatusBarAndNavBar title='全部套餐' onBack={navigateBack} />

      <View className='package-list__content'>
        {experiencePackage && (
          <ExperienceCard
            item={experiencePackage}
            onClick={() => navigateToDetail(experiencePackage)}
          />
        )}

        <View className='package-list__section'>
          <Text className='package-list__section-title'>正价套餐</Text>
          {standardPackages.map((item) => (
            <StandardCard
              key={item.id}
              item={item}
              onClick={() => navigateToDetail(item)}
            />
          ))}
        </View>

        <CustomEntry onClick={navigateToCustom} />
      </View>
    </View>
  );
}

function StatusBarAndNavBar({
  title,
  onBack,
}: {
  title: string;
  onBack: () => void;
}) {
  return (
    <View className='package-list__header'>
      <View
        className='package-list__status-bar'
        style={{ height: `${STATUS_BAR_HEIGHT}px` }}
      />
      <View className='package-list__navbar'>
        <View className='package-list__back' onClick={onBack}>
          <Icon name='arrow-left' className='package-list__back-icon' />
        </View>
        <Text className='package-list__title'>{title}</Text>
        <View className='package-list__navbar-placeholder' />
      </View>
    </View>
  );
}

function ExperienceCard({
  item,
  onClick,
}: {
  item: PackageListItem;
  onClick: () => void;
}) {
  return (
    <View className='experience-card' onClick={onClick}>
      <View className='experience-card__info'>
        <View className='experience-card__badge'>
          <Text className='experience-card__badge-text'>体验课</Text>
        </View>
        <Text className='experience-card__name'>{item.name}</Text>
        <Text className='experience-card__desc'>
          {item.totalHours} 节 · {item.validDays} 天有效
        </Text>
      </View>
      <View className='experience-card__action'>
        <Text className='experience-card__price'>
          ¥{formatPrice(item.price)}
        </Text>
        <View className='experience-card__button'>
          <Text className='experience-card__button-text'>立即购买</Text>
        </View>
      </View>
    </View>
  );
}

function StandardCard({
  item,
  onClick,
}: {
  item: PackageListItem;
  onClick: () => void;
}) {
  return (
    <View className='standard-card' onClick={onClick}>
      <View className='standard-card__header'>
        <View className='standard-card__tag standard-card__tag--primary'>
          <Text className='standard-card__tag-text'>正价套餐</Text>
        </View>
        <Text className='standard-card__name'>{item.name}</Text>
        {item.tags.map((tag) => (
          <View
            key={tag}
            className={`standard-card__tag standard-card__tag--${getTagVariant(tag)}`}
          >
            <Text className='standard-card__tag-text'>{tag}</Text>
          </View>
        ))}
      </View>

      <View className='standard-card__info'>
        <View className='standard-card__info-item'>
          <Icon name='calendar' className='standard-card__info-icon' />
          <Text className='standard-card__info-text'>
            有效期 {item.validDays} 天
          </Text>
        </View>
        <View className='standard-card__info-item'>
          <Icon name='user' className='standard-card__info-icon' />
          <Text className='standard-card__info-text'>{item.teachingType}</Text>
        </View>
        <View className='standard-card__info-item'>
          <Icon name='time' className='standard-card__info-icon' />
          <Text className='standard-card__info-text'>
            {item.durationMinutes} 分钟/节
          </Text>
        </View>
      </View>

      <View className='standard-card__footer'>
        <View className='standard-card__price-row'>
          <Text className='standard-card__price'>
            ¥{formatPrice(item.price)}
          </Text>
          {item.originalPrice &&
            Number(item.originalPrice) > Number(item.price) && (
              <Text className='standard-card__original-price'>
                ¥{formatPrice(item.originalPrice)}
              </Text>
            )}
          <Text className='standard-card__unit'>/ {item.totalHours} 节</Text>
        </View>
        <Icon name='arrow-right' className='standard-card__arrow' />
      </View>
    </View>
  );
}

function CustomEntry({ onClick }: { onClick: () => void }) {
  return (
    <View className='custom-entry' onClick={onClick}>
      <View className='custom-entry__icon-wrap'>
        <Icon name='stack' className='custom-entry__icon' />
      </View>
      <View className='custom-entry__info'>
        <Text className='custom-entry__title'>自定义课时</Text>
        <Text className='custom-entry__desc'>按教练参考单价灵活选择</Text>
      </View>
      <Text className='custom-entry__price'>参考 ¥200/节</Text>
      <Icon name='arrow-right' className='custom-entry__arrow' />
    </View>
  );
}

function PackageListSkeleton() {
  return (
    <View className='package-list package-list--skeleton'>
      <StatusBarAndNavBar title='全部套餐' onBack={() => {}} />
      <View className='package-list__content'>
        <View className='skeleton-card skeleton-card--experience' />
        <View className='skeleton-title' />
        <View className='skeleton-card' />
        <View className='skeleton-card' />
        <View className='skeleton-card' />
        <View className='skeleton-card skeleton-card--custom' />
      </View>
    </View>
  );
}

function formatPrice(price: string): string {
  const num = Number(price);
  if (Number.isNaN(num)) return price;
  return num.toLocaleString('zh-CN');
}

function getTagVariant(tag: string): string {
  if (tag.includes('热销')) return 'hot';
  if (tag.includes('推荐')) return 'recommend';
  return 'primary';
}
