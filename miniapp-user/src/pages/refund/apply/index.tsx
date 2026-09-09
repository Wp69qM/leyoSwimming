import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { View, Text, Textarea } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { fetchMyPackageDetail, submitRefund } from '@/api/package';
import { handleBusinessError } from '@/api/request';
import { Icon } from '@/components/common/Icon';
import { getTeachingTypeLabel } from '@/constants/teachingType';
import { getPageQuery } from '@/utils/router';
import {
  REFUND_REASONS,
  type RefundReason,
  type UserPackageDetail,
} from '@/types/package';

import './index.scss';

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20;
const MAX_REASON_LENGTH = 200;

const MODE_LABELS: Record<UserPackageDetail['packageMode'], string> = {
  standard: '正价套餐',
  experience: '体验课',
  custom: '自定义套餐',
};

const STROKE_MAP: Record<number, string> = {
  1: '自由泳',
  2: '蛙泳',
  3: '仰泳',
  4: '蝶泳',
};

export default function RefundApplyPage() {
  const [detail, setDetail] = useState<UserPackageDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedReason, setSelectedReason] = useState<RefundReason | null>(
    null
  );
  const [reasonText, setReasonText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [showReasonPicker, setShowReasonPicker] = useState(false);
  const navigateTimerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (navigateTimerRef.current !== null) {
        clearTimeout(navigateTimerRef.current);
      }
    };
  }, []);

  const params = useMemo(() => getPageQuery(), []);
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
      if (!data.canRefund) {
        setError('当前套餐不符合退款条件');
        setDetail(data);
      } else {
        setDetail(data);
      }
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

  function navigateToOrderDetail(orderId: number) {
    void Taro.redirectTo({
      url: `/pages/order/detail/index?orderId=${orderId}`,
    });
  }

  function handleReasonSelect(reason: RefundReason) {
    setSelectedReason(reason);
    setShowReasonPicker(false);
  }

  async function handleSubmit() {
    if (!detail || submitting) return;
    if (!selectedReason) {
      void Taro.showToast({ title: '请选择退款原因', icon: 'none' });
      return;
    }

    const reason = reasonText.trim()
      ? `${selectedReason}：${reasonText.trim()}`
      : selectedReason;

    setSubmitting(true);
    try {
      const response = await submitRefund({
        packageId: detail.packageId,
        reason,
      });
      void Taro.showToast({ title: '申请已提交', icon: 'success' });
      navigateTimerRef.current = window.setTimeout(() => {
        navigateToOrderDetail(response.orderId);
      }, 1500);
    } catch (err) {
      void Taro.showToast({
        title: handleBusinessError(err),
        icon: 'none',
      });
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <View
        className='refund-apply'
        style={
          {
            '--status-bar-height': `${STATUS_BAR_HEIGHT}px`,
          } as React.CSSProperties
        }
      >
        <StatusBarAndNavBar title='申请退款' onBack={navigateBack} />
        <View className='refund-apply__content refund-apply__content--skeleton'>
          <View className='refund-apply__skeleton-card' />
          <View className='refund-apply__skeleton-card' />
          <View className='refund-apply__skeleton-card' />
        </View>
      </View>
    );
  }

  if (error || !detail) {
    return (
      <View
        className='refund-apply'
        style={
          {
            '--status-bar-height': `${STATUS_BAR_HEIGHT}px`,
          } as React.CSSProperties
        }
      >
        <StatusBarAndNavBar title='申请退款' onBack={navigateBack} />
        <View className='refund-apply__error'>
          <Icon name='error-circle' className='refund-apply__error-icon' />
          <View className='refund-apply__error-text'>
            {error ?? '套餐不存在'}
          </View>
          <View
            className='refund-apply__error-button'
            onClick={navigateToMyPackages}
          >
            <View className='refund-apply__error-button-text'>
              返回我的套餐
            </View>
          </View>
        </View>
      </View>
    );
  }

  return (
    <View
      className='refund-apply'
      style={
        {
          '--status-bar-height': `${STATUS_BAR_HEIGHT}px`,
        } as React.CSSProperties
      }
    >
      <StatusBarAndNavBar title='申请退款' onBack={navigateBack} />

      <View className='refund-apply__content'>
        <PackageSnapshotCard detail={detail} />
        <RefundAmountCard detail={detail} />
        <ReasonSelector
          selectedReason={selectedReason}
          showPicker={showReasonPicker}
          onTogglePicker={() => setShowReasonPicker(!showReasonPicker)}
        />
        <ReasonTextarea value={reasonText} onChange={setReasonText} />
      </View>

      <BottomSubmitBar
        refundAmount={calculateRefundAmount(detail)}
        disabled={!selectedReason || submitting}
        submitting={submitting}
        onSubmit={handleSubmit}
      />

      {showReasonPicker && (
        <ReasonPicker
          selectedReason={selectedReason}
          onSelect={handleReasonSelect}
          onClose={() => setShowReasonPicker(false)}
        />
      )}
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
    <View className='refund-apply__header'>
      <View
        className='refund-apply__status-bar'
        style={{ height: `${STATUS_BAR_HEIGHT}px` }}
      />
      <View className='refund-apply__navbar'>
        <View className='refund-apply__back' onClick={onBack}>
          <Icon name='arrow-left' className='refund-apply__back-icon' />
        </View>
        <View className='refund-apply__title'>{title}</View>
        <View className='refund-apply__navbar-placeholder' />
      </View>
    </View>
  );
}

function PackageSnapshotCard({ detail }: { detail: UserPackageDetail }) {
  const modeLabel = MODE_LABELS[detail.packageMode];
  const strokeText = useMemo(() => {
    if (!detail.strokeIds || detail.strokeIds.length === 0) return '全部泳姿';
    return detail.strokeIds
      .map((id) => STROKE_MAP[id] ?? `泳姿${id}`)
      .join('/');
  }, [detail.strokeIds]);

  return (
    <View className='snapshot-card'>
      <View className='snapshot-card__header'>
        <View className='snapshot-card__icon-wrap'>
          <View className='snapshot-card__icon-text'>课</View>
        </View>
        <View className='snapshot-card__info'>
          <View className='snapshot-card__name-row'>
            <View className='snapshot-card__name'>{detail.packageName}</View>
            <View className='snapshot-card__mode-tag'>
              <View className='snapshot-card__mode-tag-text'>{modeLabel}</View>
            </View>
          </View>
          <View className='snapshot-card__meta'>
            教练：{detail.coachName} · {getTeachingTypeLabel(detail.teachingType)}
          </View>
          <View className='snapshot-card__meta'>
            泳姿：{strokeText} · {detail.durationMinutes} 分钟/节
          </View>
        </View>
      </View>

      <View className='snapshot-card__divider' />

      <View className='snapshot-card__rows'>
        <View className='snapshot-card__row'>
          <View className='snapshot-card__row-label'>购买时间</View>
          <View className='snapshot-card__row-value'>
            {formatDateTime(detail.createdAt)}
          </View>
        </View>
        <View className='snapshot-card__row'>
          <View className='snapshot-card__row-label'>已用 / 总课时</View>
          <View className='snapshot-card__row-value'>
            {detail.consumedCount} / {detail.totalHours} 节
          </View>
        </View>
      </View>
    </View>
  );
}

function RefundAmountCard({ detail }: { detail: UserPackageDetail }) {
  const refundAmount = calculateRefundAmount(detail);
  const isFullRefund =
    detail.status === 'frozen' && detail.frozenReason === 'coach_resigned';

  return (
    <View className='amount-card'>
      <View className='amount-card__title'>退款规则</View>

      <View className='amount-card__formula'>
        <View className='amount-card__formula-text'>
          {isFullRefund
            ? '教练离职，按实付金额全额退款'
            : '退款金额 = 实付金额 × (总课时 - 已用课时) / 总课时 × 退款比例'}
        </View>
        {!isFullRefund && (
          <View className='amount-card__formula-example'>
            = ¥{formatPrice(detail.paidAmount)} × ({detail.totalHours} -{' '}
            {detail.consumedCount}) / {detail.totalHours} ×{' '}
            {Math.round(Number(detail.refundRatio) * 100)}%
          </View>
        )}
      </View>

      <View className='amount-card__amount-row'>
        <View className='amount-card__amount-label'>可退金额</View>
        <View className='amount-card__amount-value'>¥{refundAmount}</View>
      </View>

      <View className='amount-card__tip'>
        <Icon name='warning' className='amount-card__tip-icon' />
        <View className='amount-card__tip-text'>{detail.refundRuleText}</View>
      </View>
    </View>
  );
}

function ReasonSelector({
  selectedReason,
  showPicker,
  onTogglePicker,
}: {
  selectedReason: RefundReason | null;
  showPicker: boolean;
  onTogglePicker: () => void;
}) {
  return (
    <View className='reason-selector'>
      <View className='reason-selector__label-row'>
        <View className='reason-selector__label'>退款原因</View>
        <Text className='reason-selector__required'>*</Text>
      </View>
      <View
        className={`reason-selector__input ${
          showPicker ? 'reason-selector__input--active' : ''
        }`}
        onClick={onTogglePicker}
      >
        <Text
          className={`reason-selector__placeholder ${
            selectedReason ? 'reason-selector__placeholder--selected' : ''
          }`}
        >
          {selectedReason ?? '请选择退款原因'}
        </Text>
        <Icon
          name={showPicker ? 'arrow-up' : 'arrow-down'}
          className='reason-selector__arrow'
        />
      </View>
    </View>
  );
}

function ReasonPicker({
  selectedReason,
  onSelect,
  onClose,
}: {
  selectedReason: RefundReason | null;
  onSelect: (reason: RefundReason) => void;
  onClose: () => void;
}) {
  return (
    <View className='reason-picker' onClick={onClose}>
      <View
        className='reason-picker__content'
        onClick={(e) => e.stopPropagation()}
      >
        <View className='reason-picker__header'>
          <View className='reason-picker__title'>选择退款原因</View>
          <View className='reason-picker__close' onClick={onClose}>
            <Icon name='close' className='reason-picker__close-icon' />
          </View>
        </View>
        {REFUND_REASONS.map((reason) => (
          <View
            key={reason}
            className={`reason-picker__item ${
              selectedReason === reason ? 'reason-picker__item--selected' : ''
            }`}
            onClick={() => onSelect(reason)}
          >
            <Text className='reason-picker__item-text'>{reason}</Text>
            {selectedReason === reason && (
              <Icon name='check' className='reason-picker__check-icon' />
            )}
          </View>
        ))}
      </View>
    </View>
  );
}

function ReasonTextarea({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  function handleInput(e: { detail: { value: string } }) {
    onChange(e.detail.value.slice(0, MAX_REASON_LENGTH));
  }

  return (
    <View className='reason-textarea'>
      <View className='reason-textarea__header'>
        <View className='reason-textarea__label'>退款原因说明</View>
        <Text className='reason-textarea__optional'>（选填）</Text>
        <Text className='reason-textarea__count'>
          {value.length}/{MAX_REASON_LENGTH}
        </Text>
      </View>
      <Textarea
        className='reason-textarea__input'
        value={value}
        onInput={handleInput}
        placeholder='请简要说明退款原因（可选）'
        placeholderClass='reason-textarea__placeholder'
        maxlength={MAX_REASON_LENGTH}
      />
    </View>
  );
}

function BottomSubmitBar({
  refundAmount,
  disabled,
  submitting,
  onSubmit,
}: {
  refundAmount: string;
  disabled: boolean;
  submitting: boolean;
  onSubmit: () => void;
}) {
  return (
    <View className='submit-bar'>
      <View className='submit-bar__amount'>
        <View className='submit-bar__amount-label'>可退金额</View>
        <View className='submit-bar__amount-value'>¥{refundAmount}</View>
      </View>
      <View
        className={`submit-bar__button ${
          disabled ? 'submit-bar__button--disabled' : ''
        }`}
        onClick={onSubmit}
      >
        <View className='submit-bar__button-text'>
          {submitting ? '提交中...' : '提交申请'}
        </View>
      </View>
    </View>
  );
}

function calculateRefundAmount(detail: UserPackageDetail): string {
  if (detail.status === 'frozen' && detail.frozenReason === 'coach_resigned') {
    return formatPrice(detail.paidAmount);
  }

  const paidAmount = Number(detail.paidAmount);
  const refundRatio = Number(detail.refundRatio);
  const remainingHours = detail.totalHours - detail.consumedCount;

  if (
    Number.isNaN(paidAmount) ||
    Number.isNaN(refundRatio) ||
    detail.totalHours <= 0
  ) {
    return '0.00';
  }

  const amount =
    paidAmount * (remainingHours / detail.totalHours) * refundRatio;
  return amount.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatPrice(price: string): string {
  const num = Number(price);
  if (Number.isNaN(num)) return '0.00';
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return '';
  return dateStr.replace('T', ' ');
}
