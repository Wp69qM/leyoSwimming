import { useCallback, useEffect, useMemo, useState } from 'react';
import { View, Text, Image, ScrollView } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { fetchCoachDetail, fetchUserPackageQualification } from '@/api/coach';
import { handleBusinessError } from '@/api/request';
import { Icon } from '@/components/common/Icon';
import { useAuthStore } from '@/stores/authStore';
import type {
  CoachDetail,
  CoachDetailPackage,
  UserPackageQualification,
} from '@/types/coach';

import {
  CoachHeader,
  CoachBio,
  CoachReferencePrice,
  CoachContact,
  CoachAvailableTimes,
  CoachPackages,
  CoachReviews,
  CoachDetailSkeleton,
  getVisiblePackages,
  resolveCtaState,
} from './components';

import './index.scss';

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20;

const PAGE_PATHS = {
  login: '/pages/login/wechat/index',
  packageList: '/pages/package/list/index',
  packageDetail: '/pages/package/detail/index',
  orderConfirm: '/pages/order/confirm/index',
  customConfig: '/pages/package/custom/index',
  booking: '/pages/booking/index',
};

export default function CoachDetailPage() {
  const [coach, setCoach] = useState<CoachDetail | null>(null);
  const [qualification, setQualification] =
    useState<UserPackageQualification | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showWechat, setShowWechat] = useState(false);
  const [isFavorite, setIsFavorite] = useState(false);

  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);

  const params = useMemo(
    () => Taro.getCurrentInstance().router?.params ?? {},
    []
  );
  const coachId = Number(params.id);

  const loadData = useCallback(async () => {
    if (Number.isNaN(coachId) || coachId <= 0) {
      setError('教练 ID 无效');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const [detailData, qualificationData] = await Promise.all([
        fetchCoachDetail(coachId),
        isLoggedIn ? fetchUserPackageQualification() : Promise.resolve(null),
      ]);
      setCoach(detailData);
      setQualification(qualificationData);
    } catch (err) {
      setError(handleBusinessError(err));
    } finally {
      setLoading(false);
    }
  }, [coachId, isLoggedIn]);

  useEffect(() => {
    useAuthStore.getState().restoreFromStorage();
    void loadData();
  }, [loadData]);

  function navigateBack() {
    void Taro.navigateBack();
  }

  function navigateToLogin() {
    void Taro.navigateTo({
      url: `${PAGE_PATHS.login}?redirect=${encodeURIComponent(`/pages/coach/detail/index?id=${coachId}`)}`,
    });
  }

  function navigateToPackageList() {
    void Taro.navigateTo({ url: PAGE_PATHS.packageList });
  }

  function navigateToPackageDetail(pkg: CoachDetailPackage) {
    void Taro.navigateTo({
      url: `${PAGE_PATHS.packageDetail}?id=${pkg.id}&coachId=${coachId}&source=detail`,
    });
  }

  function navigateToOrderConfirm(packageId: number, packageType: number) {
    void Taro.navigateTo({
      url: `${PAGE_PATHS.orderConfirm}?packageId=${packageId}&coachId=${coachId}&packageType=${packageType}`,
    });
  }

  function navigateToCustomConfig() {
    void Taro.navigateTo({
      url: `${PAGE_PATHS.packageDetail}?id=-1&coachId=${coachId}&source=detail`,
    });
  }

  function navigateToBooking() {
    void Taro.navigateTo({ url: `${PAGE_PATHS.booking}?coachId=${coachId}` });
  }

  function handleViewAllSchedule() {
    void Taro.switchTab({ url: PAGE_PATHS.booking });
  }

  function handlePhoneCall() {
    if (!coach?.contact.phone) return;
    void Taro.makePhoneCall({ phoneNumber: coach.contact.phone });
  }

  function handleCtaPrimary() {
    if (!coach) return;
    if (!useAuthStore.getState().isLoggedIn) {
      navigateToLogin();
      return;
    }
    if (isBoundThisCoach) {
      navigateToBooking();
      return;
    }
    const experiencePackage = coach.packages.find(
      (p) => p.packageMode === 'experience'
    );
    if (experiencePackage && !qualification?.hasExperiencePackage) {
      navigateToOrderConfirm(experiencePackage.id, 0);
      return;
    }
    navigateToPackageList();
  }

  function handleCtaSecondary() {
    if (!useAuthStore.getState().isLoggedIn) {
      navigateToLogin();
      return;
    }
    navigateToPackageList();
  }

  function handleFavorite() {
    if (!useAuthStore.getState().isLoggedIn) {
      navigateToLogin();
      return;
    }
    setIsFavorite((prev) => !prev);
    void Taro.showToast({
      title: isFavorite ? '已取消收藏' : '收藏成功',
      icon: 'none',
    });
  }

  if (loading) {
    return <CoachDetailSkeleton />;
  }

  if (error || !coach) {
    return (
      <View className='coach-detail-error'>
        <Icon name='error-circle' className='coach-detail-error__icon' />
        <Text className='coach-detail-error__text'>
          {error || '教练信息不存在'}
        </Text>
        <View className='coach-detail-error__btn' onClick={loadData}>
          重新加载
        </View>
      </View>
    );
  }

  const isBoundThisCoach = Boolean(
    qualification?.hasActivePackage && qualification.activeCoachId === coach.id
  );
  const isBoundOtherCoach = Boolean(
    qualification?.hasActivePackage && qualification.activeCoachId !== coach.id
  );
  const canPurchase = coach.status === 1 && !isBoundOtherCoach;
  const isOnLeave = coach.realTimeStatus === '请假中';
  const visiblePackages = getVisiblePackages(
    coach.packages,
    isLoggedIn,
    qualification
  );

  const {
    primaryText,
    primaryDisabled,
    secondaryText,
    secondaryDisabled,
    showCta,
  } = resolveCtaState(
    isLoggedIn,
    qualification,
    coach,
    isBoundThisCoach,
    isBoundOtherCoach
  );

  return (
    <View className='coach-detail'>
      <ScrollView className='coach-detail__scroll' scrollY>
        <View
          className='coach-detail__header'
          style={{ paddingTop: `${STATUS_BAR_HEIGHT}px` }}
        >
          <View className='coach-detail__navbar'>
            <View className='coach-detail__back' onClick={navigateBack}>
              <Icon name='arrow-left' className='coach-detail__back-icon' />
            </View>
            <View className='coach-detail__title'>教练详情</View>
            <View className='coach-detail__navbar-placeholder' />
          </View>
          <CoachHeader coach={coach} />
        </View>

        {isBoundOtherCoach && (
          <View className='coach-detail__notice'>
            <Icon name='notice' className='coach-detail__notice-icon' />
            <View className='coach-detail__notice-text'>
              您已绑定其他教练，暂不可购买本教练套餐
            </View>
          </View>
        )}

        {isOnLeave && !isBoundOtherCoach && (
          <View className='coach-detail__notice'>
            <Icon name='notice' className='coach-detail__notice-icon' />
            <View className='coach-detail__notice-text'>
              教练请假中，暂不可预约
            </View>
          </View>
        )}

        <CoachBio coach={coach} />
        <CoachReferencePrice price={coach.referencePrice} />
        <CoachContact
          phone={coach.contact.phone}
          wechatQrUrl={coach.contact.wechatQrUrl}
          isLoggedIn={isLoggedIn}
          isBoundOtherCoach={isBoundOtherCoach}
          onLogin={navigateToLogin}
          onPhoneCall={handlePhoneCall}
          onWechatPreview={() => setShowWechat(true)}
        />
        <CoachAvailableTimes
          times={coach.availableTimes}
          onViewAll={handleViewAllSchedule}
        />
        <CoachPackages
          packages={visiblePackages}
          canPurchase={canPurchase}
          isBoundOtherCoach={isBoundOtherCoach}
          referencePrice={coach.referencePrice}
          onPackageClick={navigateToPackageDetail}
          onMore={navigateToPackageList}
          onCustom={navigateToCustomConfig}
        />
        <CoachReviews
          rating={coach.rating}
          totalCount={coach.reviews.length}
          reviews={coach.reviews}
          tags={coach.reviewTags}
        />

        {showCta && <View className='coach-detail__cta-spacer' />}
      </ScrollView>

      {showCta && (
        <View className='coach-detail__cta'>
          <View className='coach-detail__cta-favorite' onClick={handleFavorite}>
            <Icon
              name={isFavorite ? 'star-fill' : 'star'}
              className={`coach-detail__cta-favorite-icon ${isFavorite ? 'coach-detail__cta-favorite-icon--active' : ''}`}
            />
            <View className='coach-detail__cta-favorite-text'>
              {isFavorite ? '已收藏' : '收藏'}
            </View>
          </View>
          {secondaryText && (
            <View
              className={`coach-detail__cta-secondary ${secondaryDisabled ? 'coach-detail__cta-secondary--disabled' : ''}`}
              onClick={secondaryDisabled ? undefined : handleCtaSecondary}
            >
              <View className='coach-detail__cta-secondary-text'>
                {secondaryText}
              </View>
            </View>
          )}
          <View
            className={`coach-detail__cta-primary ${primaryDisabled ? 'coach-detail__cta-primary--disabled' : ''}`}
            onClick={primaryDisabled ? undefined : handleCtaPrimary}
          >
            <View className='coach-detail__cta-primary-text'>
              {primaryText}
            </View>
          </View>
        </View>
      )}

      {showWechat && coach.contact.wechatQrUrl && (
        <View
          className='coach-detail__modal'
          onClick={() => setShowWechat(false)}
        >
          <Image
            className='coach-detail__modal-image'
            src={coach.contact.wechatQrUrl}
            mode='aspectFit'
          />
        </View>
      )}
    </View>
  );
}
