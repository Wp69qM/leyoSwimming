import { useCallback, useEffect, useMemo, useState } from 'react';
import { View, Text, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { fetchPackageDetail } from '@/api/package';
import { handleBusinessError } from '@/api/request';
import { Icon } from '@/components/common/Icon';
import { getTeachingTypeLabel } from '@/constants/teachingType';
import { strokeCodesToLabels } from '@/constants/swimStrokes';
import { useAuthStore } from '@/stores/authStore';
import type { PackageDetail, PackageDetailCoach } from '@/types/package';

import './index.scss';

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20;
const EXPERIENCE_VALID_DAYS = 30;

const PAGE_PATHS = {
  orderConfirm: '/pages/order/confirm/index',
  customConfig: '/pages/package/custom/index',
  wechatAuth: '/pages/login/wechat/index',
  home: '/pages/index/index',
  coachDetail: '/pages/coach/detail/index',
};

export default function PackageDetailPage() {
  const [detail, setDetail] = useState<PackageDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCoach, setSelectedCoach] = useState<PackageDetailCoach | null>(
    null
  );

  const params = useMemo(
    () => Taro.getCurrentInstance().router?.params ?? {},
    []
  );
  const packageId = Number(params.id);
  const coachId = params.coachId ? Number(params.coachId) : undefined;
  const source = params.source ?? 'list';

  const loadDetail = useCallback(async () => {
    const isValidPackageId = packageId === -1 || packageId > 0;
    if (Number.isNaN(packageId) || !isValidPackageId) {
      setError('套餐 ID 无效');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const data = await fetchPackageDetail(packageId, coachId);
      setDetail(data);
      if (coachId) {
        const matched = data.applicableCoaches.find(
          (c) => c.coachId === coachId
        );
        if (matched) setSelectedCoach(matched);
      }
    } catch (err) {
      setError(handleBusinessError(err));
    } finally {
      setLoading(false);
    }
  }, [packageId, coachId]);

  useEffect(() => {
    useAuthStore.getState().restoreFromStorage();
    void loadDetail();
  }, [loadDetail]);

  function navigateBack() {
    void Taro.navigateBack();
  }

  function navigateToHome() {
    void Taro.switchTab({ url: PAGE_PATHS.home });
  }

  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);

  function handlePurchase() {
    if (!detail) return;
    if (source === 'list' && !selectedCoach) return;

    const targetCoachId = selectedCoach?.coachId ?? coachId;
    if (!targetCoachId) return;

    const loggedIn = useAuthStore.getState().isLoggedIn;
    if (!loggedIn) {
      void Taro.navigateTo({
        url: `${PAGE_PATHS.wechatAuth}?sourcePage=package_detail`,
      });
      return;
    }

    if (detail.packageMode === 'custom') {
      void Taro.navigateTo({
        url: `${PAGE_PATHS.customConfig}?packageId=${detail.id}&coachId=${targetCoachId}`,
      });
      return;
    }

    void Taro.navigateTo({
      url: `${PAGE_PATHS.orderConfirm}?packageId=${detail.id}&coachId=${targetCoachId}&packageType=${detail.packageMode === 'experience' ? 0 : 1}`,
    });
  }

  if (loading) {
    return <PackageDetailSkeleton />;
  }

  if (error) {
    return (
      <View className='package-detail-error'>
        <StatusBarAndNavBar title='套餐详情' onBack={navigateBack} />
        <View className='package-detail-error__content'>
          <Text className='package-detail-error__text'>{error}</Text>
          <View className='package-detail-error__retry' onClick={loadDetail}>
            重新加载
          </View>
        </View>
      </View>
    );
  }

  if (!detail) {
    return (
      <View className='package-detail-empty'>
        <StatusBarAndNavBar title='套餐详情' onBack={navigateBack} />
        <View className='package-detail-empty__content'>
          <View className='package-detail-empty__title'>
            套餐不存在或已下架
          </View>
          <Text
            className='package-detail-empty__action'
            onClick={navigateToHome}
          >
            返回首页
          </Text>
        </View>
      </View>
    );
  }

  const primaryCoach = selectedCoach ??
    detail.applicableCoaches[0] ?? {
      coachId: 0,
      name: '',
      avatarUrl: null,
      rating: '0',
      teachingYears: 0,
      totalStudents: 0,
      referencePrice: detail.price,
      teachingStrokes: [],
    };

  return (
    <View className='package-detail'>
      <StatusBarAndNavBar title='套餐详情' onBack={navigateBack} />

      <View className='package-detail__content'>
        <MainInfoCard detail={detail} />
        <IncludedSection detail={detail} />
        {(detail.description || detail.images.length > 0) && (
          <DescriptionSection detail={detail} />
        )}
        <NoticeSection detail={detail} />

        {source === 'list' && detail.applicableCoaches.length > 0 && (
          <CoachSelection
            coaches={detail.applicableCoaches}
            selected={selectedCoach}
            onSelect={setSelectedCoach}
          />
        )}

        {(source === 'coach' || source === 'detail') && (
          <SelectedCoachCard
            coach={primaryCoach}
            teachingType={detail.teachingType}
          />
        )}
      </View>

      <BottomBar
        detail={detail}
        selectedCoach={selectedCoach}
        source={source}
        onPurchase={handlePurchase}
      />
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
    <View className='package-detail__header'>
      <View
        className='package-detail__status-bar'
        style={{ height: `${STATUS_BAR_HEIGHT}px` }}
      />
      <View className='package-detail__navbar'>
        <View className='package-detail__back' onClick={onBack}>
          <Icon name='arrow-left' className='package-detail__back-icon' />
        </View>
        <View className='package-detail__title'>{title}</View>
        <View className='package-detail__navbar-placeholder' />
      </View>
    </View>
  );
}

function MainInfoCard({ detail }: { detail: PackageDetail }) {
  const isExperience = detail.packageMode === 'experience';
  const isCustom = detail.packageMode === 'custom';
  const tagText = isExperience ? '体验课' : isCustom ? '自定义' : '正价课';

  return (
    <View
      className={`main-info-card main-info-card--${isExperience ? 'experience' : isCustom ? 'custom' : 'standard'}`}
    >
      <View className='main-info-card__badge'>
        <View className='main-info-card__badge-text'>{tagText}</View>
      </View>
      <View className='main-info-card__name'>{detail.name}</View>
      {detail.tags.length > 0 && (
        <View className='main-info-card__tags'>
          {detail.tags.map((tag) => (
            <View key={tag} className='main-info-card__tag'>
              <View className='main-info-card__tag-text'>{tag}</View>
            </View>
          ))}
        </View>
      )}

      <View className='main-info-card__info-row'>
        <View className='main-info-card__info-item'>
          <Icon name='calendar' className='main-info-card__info-icon' />
          <View className='main-info-card__info-text'>
            有效期 {isExperience ? EXPERIENCE_VALID_DAYS : detail.validDays} 天
          </View>
        </View>
        <View className='main-info-card__info-item'>
          <Icon name='user' className='main-info-card__info-icon' />
          <View className='main-info-card__info-text'>
            {getTeachingTypeLabel(detail.teachingType)}
          </View>
        </View>
        <View className='main-info-card__info-item'>
          <Icon name='time' className='main-info-card__info-icon' />
          <View className='main-info-card__info-text'>
            {detail.durationMinutes} 分钟/节
          </View>
        </View>
      </View>

      {!isCustom && (
        <View className='main-info-card__price-row'>
          <View className='main-info-card__price'>
            ¥{formatPrice(detail.price)}
          </View>
          {Number(detail.originalPrice) > 0 &&
            Number(detail.originalPrice) > Number(detail.price) && (
              <View className='main-info-card__original-price'>
                ¥{formatPrice(detail.originalPrice)}
              </View>
            )}
          <View className='main-info-card__unit'>/ {detail.totalHours} 节</View>
        </View>
      )}
    </View>
  );
}

function IncludedSection({ detail }: { detail: PackageDetail }) {
  const isExperience = detail.packageMode === 'experience';
  const isCustom = detail.packageMode === 'custom';

  const items = useMemo(() => {
    if (isExperience) {
      return [
        `1 节一对一游泳私教课（${detail.durationMinutes} 分钟）`,
        `体验课有效期 ${EXPERIENCE_VALID_DAYS} 天`,
        '体验课不可退款',
      ];
    }
    if (isCustom) {
      return [
        '按教练参考单价灵活选择课时数',
        `自购买起 ${detail.validDays} 天有效`,
        '课前 24 小时外可取消并释放课时',
      ];
    }
    return [
      `${detail.totalHours} 节一对一游泳私教课（${detail.durationMinutes} 分钟/节）`,
      `自购买起 ${detail.validDays} 天有效`,
      '课前 24 小时外可取消并释放课时',
    ];
  }, [detail, isExperience, isCustom]);

  return (
    <View className='section-card'>
      <View className='section-card__title'>套餐包含</View>
      <View className='section-card__list'>
        {items.map((item, index) => (
          <View key={index} className='content-item'>
            <View className='content-item__icon-wrap'>
              <Icon name='check' className='content-item__icon' />
            </View>
            <Text className='content-item__text'>{item}</Text>
          </View>
        ))}
      </View>
    </View>
  );
}

function DescriptionSection({ detail }: { detail: PackageDetail }) {
  return (
    <View className='section-card'>
      <View className='section-card__title'>套餐详情</View>
      {detail.description && (
        <View className='section-card__desc'>{detail.description}</View>
      )}
      {detail.images.length > 0 && (
        <View className='section-card__images'>
          {detail.images.map((url, index) => (
            <Image
              key={index}
              className='section-card__image'
              src={url}
              mode='aspectFill'
              onClick={() => {
                void Taro.previewImage({
                  urls: detail.images,
                  current: url,
                });
              }}
            />
          ))}
        </View>
      )}
    </View>
  );
}

function NoticeSection({ detail }: { detail: PackageDetail }) {
  const isExperience = detail.packageMode === 'experience';
  const isCustom = detail.packageMode === 'custom';

  const items = useMemo(() => {
    if (isExperience) {
      return [
        '每位用户仅限购买 1 份体验课',
        '体验课过期或退款后可再次购买',
        '体验课购买后不可退款',
        `自购买起 ${EXPERIENCE_VALID_DAYS} 天内有效`,
      ];
    }

    if (isCustom) {
      return [
        '购买后即绑定该教练，不可跨教练使用',
        '按实际购买课时与教练参考单价计算总价',
        '课时数、有效期可在下单页自主选择',
        `自购买起 ${detail.validDays} 天内有效`,
      ];
    }

    const notices = [
      '购买后即绑定该教练，不可跨教练使用',
      '退款金额 = 套餐价 × (剩余课时 + 预占课时) / 总课时',
      '过期且剩余课时 > 0 的套餐可申请后台延期',
    ];

    if (detail.refundEnabled) {
      notices.push(
        `开课后 ${detail.refundValidDays} 天内可申请退款，退款比例 ${Math.round(Number(detail.refundRatio) * 100)}%`
      );
    } else {
      notices.push('本套餐购买后不可退款');
    }

    notices.push(`自购买起 ${detail.validDays} 天内有效`);
    return notices;
  }, [detail, isExperience, isCustom]);

  return (
    <View className='section-card'>
      <View className='section-card__title'>购买须知</View>
      <View className='section-card__list'>
        {items.map((item, index) => (
          <View key={index} className='notice-item'>
            <View className='notice-item__icon-wrap'>
              <Icon name='notice' className='notice-item__icon' />
            </View>
            <View className='notice-item__text'>{item}</View>
          </View>
        ))}
      </View>
    </View>
  );
}

function CoachSelection({
  coaches,
  selected,
  onSelect,
}: {
  coaches: PackageDetailCoach[];
  selected: PackageDetailCoach | null;
  onSelect: (coach: PackageDetailCoach) => void;
}) {
  return (
    <View className='section-card coach-selection'>
      <View className='section-card__title'>选择教练</View>
      <View className='coach-selection__list'>
        {coaches.map((coach) => {
          const isSelected = selected?.coachId === coach.coachId;
          return (
            <View
              key={coach.coachId}
              className={`package-coach-card ${isSelected ? 'package-coach-card--selected' : ''}`}
              onClick={() => onSelect(coach)}
            >
              {coach.avatarUrl ? (
                <Image
                  className='package-coach-card__avatar'
                  src={coach.avatarUrl}
                  mode='aspectFill'
                />
              ) : (
                <View className='package-coach-card__avatar package-coach-card__avatar--placeholder'>
                  <View className='package-coach-card__avatar-text'>
                    {coach.name.charAt(0)}
                  </View>
                </View>
              )}
              <View className='package-coach-card__name'>{coach.name}</View>
              <View className='package-coach-card__rating'>
                <Icon name='star' className='package-coach-card__star' />
                <View className='package-coach-card__rating-text'>{coach.rating}</View>
              </View>
              <Text className='package-coach-card__stats'>
                教龄 {coach.teachingYears} 年 · 学员 {coach.totalStudents} 人
              </Text>
              <View className='package-coach-card__price'>
                ¥{formatPrice(coach.referencePrice)}/节
              </View>
            </View>
          );
        })}
      </View>
    </View>
  );
}

function SelectedCoachCard({
  coach,
  teachingType,
}: {
  coach: PackageDetailCoach;
  teachingType: string;
}) {
  function navigateToCoachDetail() {
    void Taro.navigateTo({
      url: `${PAGE_PATHS.coachDetail}?id=${coach.coachId}`,
    });
  }

  return (
    <View className='section-card selected-coach'>
      <View className='section-card__title'>执教教练</View>
      <View className='selected-coach__card' onClick={navigateToCoachDetail}>
        {coach.avatarUrl ? (
          <Image
            className='selected-coach__avatar'
            src={coach.avatarUrl}
            mode='aspectFill'
          />
        ) : (
          <View className='selected-coach__avatar selected-coach__avatar--placeholder'>
            <View className='selected-coach__avatar-text'>
              {coach.name.charAt(0)}
            </View>
          </View>
        )}
        <View className='selected-coach__info'>
          <View className='selected-coach__name'>教练 {coach.name}</View>
          <View className='selected-coach__meta'>
            <Icon name='star' className='selected-coach__star' />
            <Text className='selected-coach__meta-text'>{coach.rating} 分</Text>
            <Text className='selected-coach__meta-text'>
              · ¥{formatPrice(coach.referencePrice)}/节
            </Text>
          </View>
          <Text className='selected-coach__strokes'>
            {getTeachingTypeLabel(teachingType)}
            {coach.teachingStrokes.length > 0 &&
              ` · 泳姿：${strokeCodesToLabels(coach.teachingStrokes).join('/')}`}
          </Text>
        </View>
        <Icon name='arrow-right' className='selected-coach__arrow' />
      </View>
    </View>
  );
}

function BottomBar({
  detail,
  selectedCoach,
  source,
  onPurchase,
}: {
  detail: PackageDetail;
  selectedCoach: PackageDetailCoach | null;
  source: string;
  onPurchase: () => void;
}) {
  const isExperience = detail.packageMode === 'experience';
  const isCustom = detail.packageMode === 'custom';
  const unitPrice = selectedCoach
    ? selectedCoach.referencePrice
    : detail.totalHours > 0
      ? (Number(detail.price) / detail.totalHours).toFixed(2)
      : detail.price;

  const canPurchase = source === 'coach' || source === 'detail' || selectedCoach !== null;
  const buttonText = canPurchase ? '立即购买' : '请选择教练';

  return (
    <View className='bottom-bar'>
      {!isCustom && (
        <View className='bottom-bar__price-wrap'>
          <View className='bottom-bar__price'>¥{formatPrice(detail.price)}</View>
          <View className='bottom-bar__subtext'>
            {isExperience
              ? `1 节 · ${EXPERIENCE_VALID_DAYS} 天有效`
              : `${detail.totalHours} 节 · ¥${formatPrice(unitPrice)}/节`}
          </View>
        </View>
      )}
      {isCustom && (
        <View className='bottom-bar__price-wrap'>
          <View className='bottom-bar__price bottom-bar__price--custom'>
            按课时灵活计价
          </View>
          <View className='bottom-bar__subtext'>
            选择教练后确定单价
          </View>
        </View>
      )}
      <View
        className={`bottom-bar__button ${canPurchase ? '' : 'bottom-bar__button--disabled'}`}
        onClick={canPurchase ? onPurchase : undefined}
      >
        <View className='bottom-bar__button-text'>{buttonText}</View>
      </View>
    </View>
  );
}

function PackageDetailSkeleton() {
  return (
    <View className='package-detail package-detail--skeleton'>
      <StatusBarAndNavBar title='套餐详情' onBack={() => {}} />
      <View className='package-detail__content'>
        <View className='skeleton-main-card' />
        <View className='skeleton-section' />
        <View className='skeleton-section' />
        <View className='skeleton-section' />
      </View>
    </View>
  );
}

function formatPrice(price: string): string {
  const num = Number(price);
  if (Number.isNaN(num)) return price;
  return num.toLocaleString('zh-CN');
}
