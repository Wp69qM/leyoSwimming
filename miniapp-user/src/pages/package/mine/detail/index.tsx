import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { fetchMyPackageDetail } from '@/api/package';
import { handleBusinessError } from '@/api/request';
import { Icon } from '@/components/common/Icon';
import { getTeachingTypeLabel } from '@/constants/teachingType';
import type { UserPackageDetail, UserPackageStatus } from '@/types/package';

import './index.scss';

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20;

const STROKE_MAP: Record<number, string> = {
  1: '自由泳',
  2: '蛙泳',
  3: '仰泳',
  4: '蝶泳',
};

const STATUS_CONFIG: Record<
  UserPackageStatus,
  { label: string; tagClass: string }
> = {
  active: { label: '可用', tagClass: 'status-tag--success' },
  exhausted: { label: '已耗尽', tagClass: 'status-tag--secondary' },
  expired: { label: '已过期', tagClass: 'status-tag--warning' },
  refunded: { label: '已退款', tagClass: 'status-tag--danger' },
  frozen: { label: '已冻结', tagClass: 'status-tag--danger' },
};

const MODE_LABELS: Record<UserPackageDetail['packageMode'], string> = {
  standard: '正价套餐',
  experience: '体验课',
  custom: '自定义套餐',
};

export default function MyPackageDetailPage() {
  const [detail, setDetail] = useState<UserPackageDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const params = useMemo(
    () => Taro.getCurrentInstance().router?.params ?? {},
    []
  );
  const packageId = Number(params.packageId);

  const loadData = useCallback(async () => {
    if (!packageId) {
      setError('套餐参数缺失');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await fetchMyPackageDetail(packageId);
      setDetail(data);
    } catch (err) {
      setError(handleBusinessError(err));
    } finally {
      setLoading(false);
    }
  }, [packageId]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  function navigateBack() {
    void Taro.navigateBack();
  }

  function navigateToMyPackages() {
    void Taro.redirectTo({ url: '/pages/package/mine/index' });
  }

  function navigateToRefund() {
    if (!detail) return;
    void Taro.navigateTo({
      url: `/pages/refund/apply/index?packageId=${detail.packageId}`,
    });
  }

  if (loading) {
    return (
      <View className='my-package-detail my-package-detail--skeleton'>
        <StatusBarAndNavBar title='套餐详情' onBack={navigateBack} />
        <View className='my-package-detail__content'>
          <View className='info-card info-card--skeleton' />
          <View className='usage-card usage-card--skeleton' />
        </View>
      </View>
    );
  }

  if (error || !detail) {
    return (
      <View className='my-package-detail'>
        <StatusBarAndNavBar title='套餐详情' onBack={navigateBack} />
        <View className='my-package-detail__error'>
          <Icon name='error-circle' className='my-package-detail__error-icon' />
          <View className='my-package-detail__error-text'>
            {error ?? '套餐不存在'}
          </View>
          <View
            className='my-package-detail__error-button'
            onClick={navigateToMyPackages}
          >
            <View className='my-package-detail__error-button-text'>
              返回我的套餐
            </View>
          </View>
        </View>
      </View>
    );
  }

  const statusConfig = STATUS_CONFIG[detail.status];

  return (
    <View
      className='my-package-detail'
      style={
        {
          '--status-bar-height': `${STATUS_BAR_HEIGHT}px`,
        } as React.CSSProperties
      }
    >
      <StatusBarAndNavBar title='套餐详情' onBack={navigateBack} />

      <View className='my-package-detail__content'>
        <InfoCard detail={detail} statusConfig={statusConfig} />
        <UsageCard detail={detail} />
        {detail.status === 'frozen' &&
          detail.frozenReason === 'coach_resigned' && <CoachResignedTip />}
        {detail.canRefund && <RefundEntry onClick={navigateToRefund} />}
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
    <View className='my-package-detail__header'>
      <View
        className='my-package-detail__status-bar'
        style={{ height: `${STATUS_BAR_HEIGHT}px` }}
      />
      <View className='my-package-detail__navbar'>
        <View className='my-package-detail__back' onClick={onBack}>
          <Icon name='arrow-left' className='my-package-detail__back-icon' />
        </View>
        <View className='my-package-detail__title'>{title}</View>
        <View className='my-package-detail__navbar-placeholder' />
      </View>
    </View>
  );
}

function InfoCard({
  detail,
  statusConfig,
}: {
  detail: UserPackageDetail;
  statusConfig: { label: string; tagClass: string };
}) {
  const modeLabel = MODE_LABELS[detail.packageMode];
  const strokeText = useMemo(() => {
    if (!detail.strokeIds || detail.strokeIds.length === 0) return '全部泳姿';
    return detail.strokeIds
      .map((id) => STROKE_MAP[id] ?? `泳姿${id}`)
      .join(' / ');
  }, [detail.strokeIds]);

  const rows = [
    { label: '教练', value: detail.coachName },
    { label: '教学类型', value: getTeachingTypeLabel(detail.teachingType) },
    { label: '泳姿', value: strokeText },
    { label: '课时数', value: `${detail.totalHours} 节` },
    { label: '每节课时长', value: `${detail.durationMinutes} 分钟/节` },
    { label: '有效期', value: `${detail.validDays} 天` },
    { label: '退款规则', value: detail.refundRuleText },
    { label: '购买时间', value: formatDateTime(detail.createdAt) },
    { label: '到期时间', value: formatDateTime(detail.expireAt) },
  ];

  return (
    <View className='info-card'>
      <View className='info-card__header'>
        <View className='info-card__title-row'>
          <View className='info-card__name'>{detail.packageName}</View>
          <View className='info-card__tags'>
            <View className='info-card__mode-tag'>
              <View className='info-card__mode-tag-text'>{modeLabel}</View>
            </View>
            <View className={`info-card__status-tag ${statusConfig.tagClass}`}>
              <Text className='info-card__status-tag-text'>
                {statusConfig.label}
              </Text>
            </View>
          </View>
        </View>
      </View>

      <View className='info-card__divider' />

      <View className='info-card__body'>
        {rows.map((row) => (
          <View key={row.label} className='info-card__row'>
            <View className='info-card__row-label'>{row.label}</View>
            <View className='info-card__row-value'>{row.value}</View>
          </View>
        ))}
      </View>

      <View className='info-card__divider' />

      <View className='info-card__footer'>
        <View className='info-card__price-label'>实付</View>
        <View className='info-card__price'>
          ¥{formatPrice(detail.paidAmount)}
        </View>
        {Number(detail.originalPrice) > Number(detail.paidAmount) && (
          <View className='info-card__original-price'>
            ¥{formatPrice(detail.originalPrice)}
          </View>
        )}
      </View>
    </View>
  );
}

function UsageCard({ detail }: { detail: UserPackageDetail }) {
  const isExpired = detail.status === 'expired';
  const isFrozen = detail.status === 'frozen';

  return (
    <View className='usage-card'>
      <View className='usage-card__title'>使用进度</View>

      <View className='usage-card__stats'>
        <View className='usage-card__stat'>
          <Text className='usage-card__stat-number'>{detail.totalHours}</Text>
          <Text className='usage-card__stat-label'>总课时</Text>
        </View>
        <View className='usage-card__stat-divider' />
        <View className='usage-card__stat'>
          <Text className='usage-card__stat-number usage-card__stat-number--used'>
            {detail.consumedCount}
          </Text>
          <Text className='usage-card__stat-label'>已用课时</Text>
        </View>
        <View className='usage-card__stat-divider' />
        <View className='usage-card__stat'>
          <Text className='usage-card__stat-number usage-card__stat-number--remaining'>
            {detail.availableCount}
          </Text>
          <Text className='usage-card__stat-label'>剩余课时</Text>
        </View>
      </View>

      {isExpired && (
        <View className='usage-card__notice usage-card__notice--expired'>
          <Icon name='warning' className='usage-card__notice-icon' />
          <View className='usage-card__notice-text'>
            套餐已过期，无法再预约课程
          </View>
        </View>
      )}

      {isFrozen && (
        <View className='usage-card__notice usage-card__notice--frozen'>
          <Icon name='warning' className='usage-card__notice-icon' />
          <View className='usage-card__notice-text'>
            套餐已冻结，暂时无法预约课程
          </View>
        </View>
      )}

      {detail.status === 'active' && (
        <View className='usage-card__expire-row'>
          <Icon name='time' className='usage-card__expire-icon' />
          <Text className='usage-card__expire-text'>
            有效期至 {formatDateTime(detail.expireAt)}
          </Text>
        </View>
      )}
    </View>
  );
}

function CoachResignedTip() {
  return (
    <View className='coach-resigned-tip'>
      <Icon name='warning' className='coach-resigned-tip__icon' />
      <Text className='coach-resigned-tip__text'>
        教练已离职，本套餐可申请 100% 全额退款
      </Text>
    </View>
  );
}

function RefundEntry({ onClick }: { onClick: () => void }) {
  return (
    <View className='refund-entry' onClick={onClick} data-testid='refund-entry'>
      <View className='refund-entry__left'>
        <View className='refund-entry__icon-wrap'>
          <Icon name='notice' className='refund-entry__icon' />
        </View>
        <View className='refund-entry__text'>
          <View className='refund-entry__title'>申请退款</View>
          <Text className='refund-entry__subtitle'>
            按套餐快照规则计算可退金额
          </Text>
        </View>
      </View>
      <View className='refund-entry__button' data-testid='refund-entry-button'>
        <View className='refund-entry__button-text'>申请退款</View>
      </View>
    </View>
  );
}

function formatPrice(price: string): string {
  const num = Number(price);
  if (Number.isNaN(num)) return price;
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2 });
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return '';
  return dateStr.replace('T', ' ');
}
