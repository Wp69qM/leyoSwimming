import { useCallback, useEffect, useMemo, useState } from 'react';
import { View, Text, Image, Input } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { createFormalOrder, createTrialOrder } from '@/api/order';
import { fetchPackageDetail } from '@/api/package';
import { getProfile } from '@/api/profile';
import { handleBusinessError } from '@/api/request';
import { Icon } from '@/components/common/Icon';
import { getTeachingTypeLabel } from '@/constants/teachingType';
import { getPageQuery } from '@/utils/router';
import type { PackageDetail, PackageDetailCoach } from '@/types/package';
import type { UserProfile } from '@/api/profile';

import './index.scss';

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20;

const PAGE_PATHS = {
  home: '/pages/index/index',
  coachDetail: '/pages/coach/detail/index',
  payment: '/pages/order/payment/index',
  termsUserNotice: '/pages/terms/user-notice',
  termsHealth: '/pages/terms/health',
  termsDisclaimer: '/pages/terms/disclaimer',
  coachSchedule: '/pages/booking/index',
  changeCoach: '/pages/coach/change/index',
};

const AGREEMENT_VERSIONS = {
  userNotice: '1.0.0',
  health: '1.0.0',
  disclaimer: '1.0.0',
};

type AgreementKey = 'userNotice' | 'health' | 'disclaimer';

interface AgreementItem {
  key: AgreementKey;
  label: string;
  url: string;
}

export default function OrderConfirmPage() {
  const [detail, setDetail] = useState<PackageDetail | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [agreements, setAgreements] = useState<Record<AgreementKey, boolean>>({
    userNotice: false,
    health: false,
    disclaimer: false,
  });
  const [guardianPhone, setGuardianPhone] = useState('');

  const params = useMemo(() => getPageQuery(), []);
  const packageId = Number(params.packageId);
  const coachId = Number(params.coachId);
  const packageType = Number(params.packageType);
  const isExperience = packageType === 0;
  const isFormal = packageType === 1;

  const loadData = useCallback(async () => {
    if (
      Number.isNaN(packageId) ||
      packageId <= 0 ||
      Number.isNaN(coachId) ||
      coachId <= 0 ||
      (!isExperience && !isFormal)
    ) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const [packageData, profileData] = await Promise.all([
        fetchPackageDetail(packageId, coachId),
        getProfile(),
      ]);
      setDetail(packageData);
      setProfile(profileData);
    } catch (err) {
      setError(handleBusinessError(err));
    } finally {
      setLoading(false);
    }
  }, [packageId, coachId, isExperience, isFormal]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const coach = useMemo<PackageDetailCoach | undefined>(() => {
    if (!detail) return undefined;
    return detail.applicableCoaches.find((c) => c.coachId === coachId);
  }, [detail, coachId]);

  const packageInactive = false;
  const coachUnavailable = false;
  const hasConflict = false;
  const hasExperiencePackage = false;
  const isMinor = (profile?.age ?? 18) < 18;
  const guardianRequired = isFormal && isMinor && !profile?.guardianPhone;

  const requiredAgreements = useMemo<AgreementKey[]>(
    () =>
      isExperience ? ['userNotice'] : ['userNotice', 'health', 'disclaimer'],
    [isExperience]
  );

  const allAgreementsSigned = useMemo(
    () => requiredAgreements.every((key) => agreements[key]),
    [agreements, requiredAgreements]
  );

  const submitDisabled =
    loading ||
    submitting ||
    packageInactive ||
    coachUnavailable ||
    hasConflict ||
    (isExperience && hasExperiencePackage) ||
    !allAgreementsSigned ||
    (guardianRequired && guardianPhone.length === 0);

  function navigateBack() {
    void Taro.navigateBack();
  }

  function navigateToHome() {
    void Taro.switchTab({ url: PAGE_PATHS.home });
  }

  function navigateToCoachDetail() {
    void Taro.navigateTo({ url: `${PAGE_PATHS.coachDetail}?id=${coachId}` });
  }

  function navigateToUrl(url: string) {
    void Taro.navigateTo({ url });
  }

  function toggleAgreement(key: AgreementKey) {
    setAgreements((prev) => ({ ...prev, [key]: !prev[key] }));
  }

  function handleGuardianInput(value: string) {
    const digits = value.replace(/\D/g, '').slice(0, 11);
    setGuardianPhone(digits);
  }

  async function handleSubmit() {
    if (submitDisabled || !detail) return;

    if (!allAgreementsSigned) {
      Taro.showToast({ title: '请先同意全部协议', icon: 'none' });
      return;
    }

    if (guardianRequired && !/^1[3-9]\d{9}$/.test(guardianPhone)) {
      Taro.showToast({ title: '请填写正确的监护人手机号', icon: 'none' });
      return;
    }

    setSubmitting(true);
    try {
      const result = isExperience
        ? await createTrialOrder({ coachId, packageId })
        : await createFormalOrder({
            coachId,
            packageId,
            agreementVersions: AGREEMENT_VERSIONS,
            guardianPhone: guardianRequired ? guardianPhone : undefined,
          });
      void Taro.navigateTo({
        url: `${PAGE_PATHS.payment}?orderId=${result.orderId}`,
      });
    } catch (err) {
      Taro.showToast({ title: handleBusinessError(err), icon: 'none' });
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <OrderConfirmSkeleton onBack={navigateBack} />;
  }

  if (error) {
    return (
      <ErrorState
        message={error}
        onBack={navigateBack}
        onRetry={loadData}
        onHome={navigateToHome}
      />
    );
  }

  if (!detail) {
    return <EmptyState onBack={navigateBack} onHome={navigateToHome} />;
  }

  return (
    <View className='order-confirm'>
      <StatusBarAndNavBar title='确认订单' onBack={navigateBack} />

      <View className='order-confirm__content'>
        <WarningBanners
          packageInactive={packageInactive}
          coachUnavailable={coachUnavailable}
          hasConflict={hasConflict}
          hasExperiencePackage={hasExperiencePackage}
          isExperience={isExperience}
          onChangeCoach={() => navigateToUrl(PAGE_PATHS.changeCoach)}
          onBook={() => navigateToUrl(PAGE_PATHS.coachSchedule)}
        />

        <CoachInfoCard coach={coach} onClick={navigateToCoachDetail} />

        <PackageInfoCard
          detail={detail}
          isExperience={isExperience}
          teachingStrokes={coach?.teachingStrokes ?? []}
        />

        <PriceBreakdown detail={detail} />

        <AgreementSection
          agreements={agreements}
          required={requiredAgreements}
          onToggle={toggleAgreement}
          onNavigate={navigateToUrl}
        />

        {guardianRequired && (
          <GuardianSection
            value={guardianPhone}
            onChange={handleGuardianInput}
          />
        )}
      </View>

      <BottomSubmitBar
        amount={detail.price}
        disabled={submitDisabled}
        submitting={submitting}
        onSubmit={handleSubmit}
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
    <View className='order-confirm__header'>
      <View
        className='order-confirm__status-bar'
        style={{ height: `${STATUS_BAR_HEIGHT}px` }}
      />
      <View className='order-confirm__navbar'>
        <View className='order-confirm__back' onClick={onBack}>
          <Icon name='arrow-left' className='order-confirm__back-icon' />
        </View>
        <View className='order-confirm__title'>{title}</View>
        <View className='order-confirm__navbar-placeholder' />
      </View>
    </View>
  );
}

function WarningBanners({
  packageInactive,
  coachUnavailable,
  hasConflict,
  hasExperiencePackage,
  isExperience,
  onChangeCoach,
  onBook,
}: {
  packageInactive: boolean;
  coachUnavailable: boolean;
  hasConflict: boolean;
  hasExperiencePackage: boolean;
  isExperience: boolean;
  onChangeCoach: () => void;
  onBook: () => void;
}) {
  if (
    !packageInactive &&
    !coachUnavailable &&
    !hasConflict &&
    !hasExperiencePackage
  ) {
    return null;
  }

  return (
    <View className='warning-banners'>
      {packageInactive && (
        <View className='warning-banner warning-banner--error'>
          <Text className='warning-banner__text'>该套餐已下架，请重新选择</Text>
        </View>
      )}
      {coachUnavailable && (
        <View className='warning-banner warning-banner--error'>
          <Text className='warning-banner__text'>
            当前教练暂不可预约，请更换教练
          </Text>
        </View>
      )}
      {hasConflict && (
        <View className='warning-banner warning-banner--error'>
          <Text className='warning-banner__text'>
            您已绑定其他教练，需先退订才能购买当前套餐
          </Text>
          <Text className='warning-banner__action' onClick={onChangeCoach}>
            更换教练
          </Text>
        </View>
      )}
      {isExperience && hasExperiencePackage && (
        <View className='warning-banner warning-banner--warning'>
          <Text className='warning-banner__text'>
            您已持有体验套餐，无需重复购买
          </Text>
          <Text className='warning-banner__action' onClick={onBook}>
            去预约
          </Text>
        </View>
      )}
    </View>
  );
}

function CoachInfoCard({
  coach,
  onClick,
}: {
  coach?: PackageDetailCoach;
  onClick: () => void;
}) {
  if (!coach) {
    return (
      <View className='section-card order-coach-card order-coach-card--missing'>
        <Text className='order-coach-card__missing-text'>未找到教练信息</Text>
      </View>
    );
  }

  return (
    <View className='section-card order-coach-card' onClick={onClick}>
      <View className='order-coach-card__body'>
        {coach.avatarUrl ? (
          <Image
            className='order-coach-card__avatar'
            src={coach.avatarUrl}
            mode='aspectFill'
          />
        ) : (
          <View className='order-coach-card__avatar order-coach-card__avatar--placeholder'>
            <View className='order-coach-card__avatar-text'>
              {coach.name.charAt(0)}
            </View>
          </View>
        )}
        <View className='order-coach-card__info'>
          <View className='order-coach-card__name-row'>
            <View className='order-coach-card__name'>{coach.name}</View>
          </View>
          <View className='order-coach-card__meta'>
            任教 {coach.teachingYears} 年 · 评分 {coach.rating}
          </View>
        </View>
        <Icon name='arrow-right' className='order-coach-card__arrow' />
      </View>
    </View>
  );
}

function PackageInfoCard({
  detail,
  isExperience,
  teachingStrokes,
}: {
  detail: PackageDetail;
  isExperience: boolean;
  teachingStrokes: string[];
}) {
  const modeTag = isExperience ? '体验套餐' : '正价套餐';
  const quantity = isExperience ? 1 : detail.totalHours;
  const metaFirstLine = teachingStrokes.length
    ? `${getTeachingTypeLabel(detail.teachingType)} · 泳姿：${teachingStrokes.join('/')}`
    : getTeachingTypeLabel(detail.teachingType);

  return (
    <View className='section-card package-info-card'>
      <View className='package-info-card__header'>
        <View className='package-info-card__badge'>
          <View className='package-info-card__badge-text'>{modeTag}</View>
        </View>
        <View className='package-info-card__name'>{detail.name}</View>
      </View>

      <View className='package-info-card__meta'>
        <Text className='package-info-card__meta-line'>{metaFirstLine}</Text>
        <Text className='package-info-card__meta-line'>
          {detail.durationMinutes} 分钟/节 · 有效期 {detail.validDays} 天
        </Text>
      </View>

      <View className='package-info-card__price-row'>
        <View className='package-info-card__price-left' />
        <View className='package-info-card__price-column'>
          {Number(detail.originalPrice) > 0 &&
            Number(detail.originalPrice) > Number(detail.price) && (
              <View className='package-info-card__original-price'>
                ¥{formatPrice(detail.originalPrice)}
              </View>
            )}
          <View className='package-info-card__price'>
            ¥{formatPrice(detail.price)}
          </View>
          <View className='package-info-card__quantity-value'>
            {quantity} 节
          </View>
        </View>
      </View>
    </View>
  );
}

function PriceBreakdown({ detail }: { detail: PackageDetail }) {
  const original = Number(detail.originalPrice);
  const total = original > 0 ? original : Number(detail.price);
  const discount = Math.max(0, total - Number(detail.price));

  return (
    <View className='section-card price-breakdown'>
      <View className='section-card__title'>价格明细</View>
      <View className='price-breakdown__row'>
        <View className='price-breakdown__label'>商品总价</View>
        <View className='price-breakdown__value'>
          ¥{formatPrice(total.toFixed(2))}
        </View>
      </View>
      <View className='price-breakdown__row'>
        <View className='price-breakdown__label'>优惠减免</View>
        <Text className='price-breakdown__value price-breakdown__value--discount'>
          -¥{formatPrice(discount.toFixed(2))}
        </Text>
      </View>
      <View className='price-breakdown__divider' />
      <View className='price-breakdown__row price-breakdown__row--total'>
        <Text className='price-breakdown__label price-breakdown__label--total'>
          应付总额
        </Text>
        <View className='price-breakdown__total'>
          ¥{formatPrice(detail.price)}
        </View>
      </View>
    </View>
  );
}

const AGREEMENT_ITEMS: AgreementItem[] = [
  {
    key: 'userNotice',
    label: '《用户须知》',
    url: PAGE_PATHS.termsUserNotice,
  },
  {
    key: 'health',
    label: '《健康承诺书》',
    url: PAGE_PATHS.termsHealth,
  },
  {
    key: 'disclaimer',
    label: '《免责协议》',
    url: PAGE_PATHS.termsDisclaimer,
  },
];

function AgreementSection({
  agreements,
  required,
  onToggle,
  onNavigate,
}: {
  agreements: Record<AgreementKey, boolean>;
  required: AgreementKey[];
  onToggle: (key: AgreementKey) => void;
  onNavigate: (url: string) => void;
}) {
  return (
    <View className='section-card agreement-section'>
      <View className='section-card__title'>购买协议</View>
      <View className='agreement-section__list'>
        {AGREEMENT_ITEMS.map((item) => {
          if (!required.includes(item.key)) return null;
          const checked = agreements[item.key];
          return (
            <View key={item.key} className='agreement-item'>
              <View
                className={`agreement-item__checkbox ${checked ? 'agreement-item__checkbox--checked' : ''}`}
                role='checkbox'
                aria-checked={checked}
                onClick={() => onToggle(item.key)}
              >
                {checked && (
                  <Icon name='check' className='agreement-item__check-icon' />
                )}
              </View>
              <Text className='agreement-item__text'>
                我已阅读并同意
                <Text
                  className='agreement-item__link'
                  onClick={(e) => {
                    e.stopPropagation();
                    onNavigate(item.url);
                  }}
                >
                  {item.label}
                </Text>
              </Text>
            </View>
          );
        })}
      </View>
    </View>
  );
}

function GuardianSection({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <View className='section-card guardian-section'>
      <View className='section-card__title'>监护人信息</View>
      <View className='guardian-section__desc'>
        您尚未成年，购买正价课需填写监护人手机号
      </View>
      <View className='guardian-section__input-wrap'>
        <Text className='guardian-section__input-label'>监护人手机号</Text>
        <Input
          className='guardian-section__input'
          type='number'
          placeholder='请输入监护人手机号'
          value={value}
          onInput={(e) => onChange(e.detail.value)}
        />
      </View>
    </View>
  );
}

function BottomSubmitBar({
  amount,
  disabled,
  submitting,
  onSubmit,
}: {
  amount: string;
  disabled: boolean;
  submitting: boolean;
  onSubmit: () => void;
}) {
  return (
    <View className='bottom-submit-bar'>
      <View className='bottom-submit-bar__price-wrap'>
        <View className='bottom-submit-bar__price-label'>应付总额</View>
        <View className='bottom-submit-bar__price'>¥{formatPrice(amount)}</View>
      </View>
      <View
        className={`bottom-submit-bar__button ${disabled ? 'bottom-submit-bar__button--disabled' : ''}`}
        onClick={disabled ? undefined : onSubmit}
      >
        <View className='bottom-submit-bar__button-text'>
          {submitting ? '提交中...' : '提交订单'}
        </View>
      </View>
    </View>
  );
}

function OrderConfirmSkeleton({ onBack }: { onBack: () => void }) {
  return (
    <View className='order-confirm order-confirm--skeleton'>
      <StatusBarAndNavBar title='确认订单' onBack={onBack} />
      <View className='order-confirm__content'>
        <View className='skeleton-card skeleton-card--coach' />
        <View className='skeleton-card skeleton-card--package' />
        <View className='skeleton-card skeleton-card--price' />
        <View className='skeleton-card skeleton-card--agreement' />
      </View>
      <View className='bottom-submit-bar bottom-submit-bar--skeleton'>
        <View className='skeleton-price' />
        <View className='skeleton-button' />
      </View>
    </View>
  );
}

function EmptyState({
  onBack,
  onHome,
}: {
  onBack: () => void;
  onHome: () => void;
}) {
  return (
    <View className='order-confirm-empty'>
      <StatusBarAndNavBar title='确认订单' onBack={onBack} />
      <View className='order-confirm-empty__content'>
        <View className='order-confirm-empty__title'>暂无订单信息</View>
        <Text className='order-confirm-empty__action' onClick={onHome}>
          返回首页
        </Text>
      </View>
    </View>
  );
}

function ErrorState({
  message,
  onBack,
  onRetry,
  onHome,
}: {
  message: string;
  onBack: () => void;
  onRetry: () => void;
  onHome: () => void;
}) {
  return (
    <View className='order-confirm-error'>
      <StatusBarAndNavBar title='确认订单' onBack={onBack} />
      <View className='order-confirm-error__content'>
        <View className='order-confirm-error__title'>加载失败，点击重试</View>
        <Text className='order-confirm-error__text'>{message}</Text>
        <View className='order-confirm-error__actions'>
          <Text className='order-confirm-error__action' onClick={onRetry}>
            重新加载
          </Text>
          <Text className='order-confirm-error__action' onClick={onHome}>
            返回首页
          </Text>
        </View>
      </View>
    </View>
  );
}

function formatPrice(price: string): string {
  const num = Number(price);
  if (Number.isNaN(num)) return price;
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });
}
