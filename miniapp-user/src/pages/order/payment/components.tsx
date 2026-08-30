import { useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { Icon } from '@/components/common/Icon';
import type { OrderDetail, OrderStatus } from '@/types/order';

export const PAGE_PATHS = {
  home: '/pages/index/index',
  mine: '/pages/mine/index',
  orderDetail: '/pages/order/detail/index',
  packageList: '/pages/package/list/index',
};

const STATUS_LABEL_MAP: Record<OrderStatus, string> = {
  pending_payment: '待支付',
  paid: '已支付',
  cancelled: '已取消',
  refund_pending: '退款审批中',
  refund_processing: '退款处理中',
  refunded: '已退款',
  rejected: '退款被拒',
  dispute_processing: '纠纷处理中',
};

const TEACHING_TYPE_MAP: Record<string, string> = {
  one_on_one: '一对一',
  one_on_two: '一对二',
  one_on_three: '一对三',
};

export function useStatusBarHeight(): number {
  const [height] = useState(
    () => Taro.getSystemInfoSync().statusBarHeight || 20
  );
  return height;
}

export function formatCountdown(totalSeconds: number | null): string {
  if (totalSeconds === null) return '--:--:--';
  if (totalSeconds <= 0) return '00:00:00';
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return [hours, minutes, seconds]
    .map((v) => String(v).padStart(2, '0'))
    .join(':');
}

export function formatDateTime(isoString?: string): string {
  if (!isoString) return '--';
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return isoString;
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd} ${hh}:${min}`;
}

export function formatTeachingType(type?: string): string {
  return TEACHING_TYPE_MAP[type ?? ''] ?? type ?? '--';
}

export function formatPrice(price: string): string {
  const num = Number(price);
  if (Number.isNaN(num)) return price;
  if (Number.isInteger(num)) return num.toLocaleString('zh-CN');
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

export function getPackageModeLabel(order: OrderDetail): string {
  if (order.packageMode === 'experience') return '体验课';
  return '正价套餐';
}

export function getPackageModeTagClass(order: OrderDetail): string {
  if (order.packageMode === 'experience') {
    return 'payment-page__tag--experience';
  }
  return 'payment-page__tag--standard';
}

export function getStatusLabel(status: OrderStatus): string {
  return STATUS_LABEL_MAP[status] ?? status;
}

export function StatusBarAndNavBar({ title }: { title: string }) {
  const statusBarHeight = useStatusBarHeight();
  return (
    <View className='payment-page__header'>
      <View
        className='payment-page__status-bar'
        style={{ height: `${statusBarHeight}px` }}
      />
      <View className='payment-page__navbar'>
        <View className='payment-page__navbar-placeholder' />
        <View className='payment-page__title'>{title}</View>
        <View className='payment-page__navbar-placeholder' />
      </View>
    </View>
  );
}

export function AmountHeader({
  order,
  remainingSeconds,
  isExpired,
  onCopy,
}: {
  order: OrderDetail;
  remainingSeconds: number | null;
  isExpired?: boolean;
  onCopy: () => void;
}) {
  return (
    <View className='payment-amount-header'>
      <View className='payment-amount-header__order-row'>
        <View className='payment-amount-header__label'>订单编号</View>
        <Text className='payment-amount-header__order-no'>{order.orderNo}</Text>
        <View className='payment-amount-header__copy' onClick={onCopy}>
          复制
        </View>
      </View>
      <View className='payment-amount-header__amount-label'>应付金额</View>
      <Text className='payment-amount-header__amount'>
        ¥{formatPrice(order.amount)}
      </Text>
      <View className='payment-amount-header__countdown'>
        <View className='payment-amount-header__countdown-label'>
          支付剩余时间
        </View>
        <View className='payment-amount-header__countdown-value'>
          {formatCountdown(remainingSeconds)}
        </View>
      </View>
      {isExpired && (
        <View className='payment-amount-header__expired-tag'>
          <View className='payment-amount-header__expired-tag-text'>
            订单已过期
          </View>
        </View>
      )}
    </View>
  );
}

export function OrderInfoCard({
  order,
  isExpired,
  onClick,
}: {
  order: OrderDetail;
  isExpired?: boolean;
  onClick: () => void;
}) {
  const modeLabel = getPackageModeLabel(order);
  const modeClass = getPackageModeTagClass(order);
  const statusClass = isExpired
    ? 'payment-page__status-tag--expired'
    : order.status === 'pending_payment'
      ? 'payment-page__status-tag--pending'
      : 'payment-page__status-tag--default';

  return (
    <View className='payment-order-card' onClick={onClick}>
      <View className='payment-order-card__header'>
        <View className='payment-order-card__icon'>
          <View className='payment-order-card__icon-text'>课</View>
        </View>
        <View className='payment-order-card__info'>
          <View className='payment-order-card__name-row'>
            <View className='payment-order-card__name'>
              {order.packageName}
            </View>
            <View className={`payment-page__tag ${modeClass}`}>
              <View className='payment-page__tag-text'>{modeLabel}</View>
            </View>
          </View>
          <View className='payment-order-card__desc'>
            教练：{order.coachName} · {formatTeachingType(order.teachingType)} ·{' '}
            {order.totalHours} 节
          </View>
        </View>
        <View className={`payment-page__status-tag ${statusClass}`}>
          <Text className='payment-page__status-tag-text'>
            {isExpired ? '已过期' : getStatusLabel(order.status)}
          </Text>
        </View>
      </View>
      <View className='payment-order-card__divider' />
      <View className='payment-order-card__row'>
        <View className='payment-order-card__row-label'>售价</View>
        <View className='payment-order-card__row-value'>
          ¥{formatPrice(order.amount)}
        </View>
      </View>
      <View className='payment-order-card__row'>
        <View className='payment-order-card__row-label'>创建时间</View>
        <View className='payment-order-card__row-value'>
          {formatDateTime(order.createdAt)}
        </View>
      </View>
      <View className='payment-order-card__row'>
        <View className='payment-order-card__row-label'>有效期至</View>
        <View className='payment-order-card__row-value'>
          {formatDateTime(order.expireAt)}
        </View>
      </View>
    </View>
  );
}

function ChannelIcon({ channel }: { channel: number }) {
  if (channel === 0) {
    return (
      <View className='payment-method__icon payment-method__icon--wechat'>
        <View className='payment-method__icon-text'>微</View>
      </View>
    );
  }
  return (
    <View className='payment-method__icon payment-method__icon--alipay'>
      <View className='payment-method__icon-text'>支</View>
    </View>
  );
}

export function PaymentMethodSection({
  selectedChannel,
  onSelect,
}: {
  selectedChannel: number;
  onSelect: (channel: number) => void;
}) {
  return (
    <View className='payment-method-section'>
      <View className='payment-method-section__title'>支付方式</View>
      <View
        className={`payment-method__item ${
          selectedChannel === 0 ? 'payment-method__item--selected' : ''
        }`}
        onClick={() => onSelect(0)}
      >
        <ChannelIcon channel={0} />
        <View className='payment-method__name'>微信支付</View>
        <View className='payment-method__recommend'>
          <View className='payment-method__recommend-text'>推荐</View>
        </View>
        <View className='payment-method__radio'>
          {selectedChannel === 0 ? (
            <View className='payment-method__radio-checked'>
              <Icon name='check' className='payment-method__radio-check-icon' />
            </View>
          ) : (
            <View className='payment-method__radio-unchecked' />
          )}
        </View>
      </View>
      <View
        className={`payment-method__item ${
          selectedChannel === 1 ? 'payment-method__item--selected' : ''
        }`}
        onClick={() => onSelect(1)}
      >
        <ChannelIcon channel={1} />
        <View className='payment-method__name'>支付宝支付</View>
        <View className='payment-method__radio'>
          {selectedChannel === 1 ? (
            <View className='payment-method__radio-checked'>
              <Icon name='check' className='payment-method__radio-check-icon' />
            </View>
          ) : (
            <View className='payment-method__radio-unchecked' />
          )}
        </View>
      </View>
      <View className='payment-method__notice'>
        <View className='payment-method__notice-text'>
          MVP 阶段调用后端 Mock 支付，不真正调起微信/支付宝 SDK
        </View>
      </View>
    </View>
  );
}

export function SecurityNotice() {
  return (
    <View className='payment-security-notice'>
      <Icon name='shield' className='payment-security-notice__icon' />
      <Text className='payment-security-notice__text'>
        支付安全由微信官方保障，请放心支付
      </Text>
    </View>
  );
}

export function BottomPaymentBar({
  amount,
  disabled,
  loading,
  expired,
  onConfirm,
  onCancel,
  onReorder,
}: {
  amount: string;
  disabled: boolean;
  loading: boolean;
  expired?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  onReorder?: () => void;
}) {
  return (
    <View className='payment-bottom-bar'>
      <View className='payment-bottom-bar__amount'>
        <View className='payment-bottom-bar__amount-label'>应付金额</View>
        <View className='payment-bottom-bar__amount-value'>
          ¥{formatPrice(amount)}
        </View>
      </View>
      {expired ? (
        <View className='payment-bottom-bar__reorder' onClick={onReorder}>
          <View className='payment-bottom-bar__reorder-text'>重新下单</View>
        </View>
      ) : (
        <View className='payment-bottom-bar__actions'>
          <View
            className='payment-bottom-bar__cancel'
            onClick={!loading && !disabled ? onCancel : undefined}
          >
            <View className='payment-bottom-bar__cancel-text'>取消支付</View>
          </View>
          <View
            className={`payment-bottom-bar__confirm ${
              disabled || loading ? 'payment-bottom-bar__confirm--disabled' : ''
            }`}
            onClick={!loading && !disabled ? onConfirm : undefined}
          >
            <View className='payment-bottom-bar__confirm-text'>
              {loading ? '支付中...' : '确认支付'}
            </View>
          </View>
        </View>
      )}
    </View>
  );
}

export function PaymentSuccessOverlay({
  totalHours,
  countdown,
}: {
  totalHours: number;
  countdown: number;
}) {
  return (
    <View className='payment-result-overlay payment-result-overlay--success'>
      <View className='payment-result-overlay__icon payment-result-overlay__icon--success'>
        <Icon name='check' className='payment-result-overlay__check' />
      </View>
      <View className='payment-result-overlay__title'>支付成功</View>
      <Text className='payment-result-overlay__subtitle'>
        您已获得 {totalHours} 节课程，快去预约吧
      </Text>
      <Text className='payment-result-overlay__countdown'>
        {countdown} 秒后自动跳转...
      </Text>
    </View>
  );
}

export function PaymentFailureSheet({
  onRetry,
  onBack,
}: {
  onRetry: () => void;
  onBack: () => void;
}) {
  return (
    <View className='payment-result-sheet payment-result-sheet--failure'>
      <View className='payment-result-sheet__icon payment-result-sheet__icon--failure'>
        <Icon
          name='error-circle'
          className='payment-result-sheet__error-icon'
        />
      </View>
      <View className='payment-result-sheet__title'>支付未完成</View>
      <View className='payment-result-sheet__desc'>
        您可在 24 小时内继续支付，逾期订单将自动取消
      </View>
      <View className='payment-result-sheet__actions'>
        <View
          className='payment-result-sheet__btn payment-result-sheet__btn--primary'
          onClick={onRetry}
        >
          <View className='payment-result-sheet__btn-text'>重新支付</View>
        </View>
        <View
          className='payment-result-sheet__btn payment-result-sheet__btn--secondary'
          onClick={onBack}
        >
          <View className='payment-result-sheet__btn-text'>返回订单详情</View>
        </View>
      </View>
    </View>
  );
}

export function CancelConfirmModal({
  visible,
  onContinue,
  onCancel,
}: {
  visible: boolean;
  onContinue: () => void;
  onCancel: () => void;
}) {
  if (!visible) return null;
  return (
    <View className='payment-modal-overlay'>
      <View className='payment-cancel-modal'>
        <View className='payment-cancel-modal__icon'>
          <Icon name='warning' className='payment-cancel-modal__warning-icon' />
        </View>
        <View className='payment-cancel-modal__title'>是否放弃当前支付？</View>
        <View className='payment-cancel-modal__desc'>
          订单将保留 24 小时，可在订单列表中继续支付
        </View>
        <View className='payment-cancel-modal__actions'>
          <View
            className='payment-cancel-modal__btn payment-cancel-modal__btn--secondary'
            onClick={onContinue}
          >
            <View className='payment-cancel-modal__btn-text'>继续支付</View>
          </View>
          <View
            className='payment-cancel-modal__btn payment-cancel-modal__btn--primary'
            onClick={onCancel}
          >
            <View className='payment-cancel-modal__btn-text'>确认取消</View>
          </View>
        </View>
      </View>
    </View>
  );
}

export function PaymentCancelledState({ onBack }: { onBack: () => void }) {
  return (
    <View className='payment-status-state payment-status-state--cancelled'>
      <Icon
        name='error-circle'
        className='payment-status-state__icon payment-status-state__icon--cancelled'
      />
      <View className='payment-status-state__title'>订单已取消</View>
      <View className='payment-status-state__desc'>
        该订单已取消，请重新下单或查看其他订单
      </View>
      <View className='payment-status-state__btn' onClick={onBack}>
        <View className='payment-status-state__btn-text'>返回首页</View>
      </View>
    </View>
  );
}

export function PaymentSkeleton() {
  return (
    <View className='payment-page'>
      <StatusBarAndNavBar title='支付订单' />
      <View className='payment-page__content payment-page__content--loading'>
        <View className='payment-skeleton payment-skeleton--header' />
        <View className='payment-skeleton payment-skeleton--card' />
        <View className='payment-skeleton payment-skeleton--method' />
      </View>
    </View>
  );
}

export function PaymentErrorState({
  message,
  onRetry,
  onHome,
}: {
  message: string;
  onRetry: () => void;
  onHome: () => void;
}) {
  return (
    <View className='payment-page'>
      <StatusBarAndNavBar title='支付订单' />
      <View className='payment-page__content payment-page__content--center'>
        <View className='payment-empty__illustration'>
          <Icon
            name='error-circle'
            className='payment-empty__illustration-icon'
          />
        </View>
        <Text className='payment-empty__text'>{message}</Text>
        <View className='payment-empty__actions'>
          <View className='payment-empty__btn' onClick={onRetry}>
            <View className='payment-empty__btn-text'>重新加载</View>
          </View>
          <View
            className='payment-empty__btn payment-empty__btn--secondary'
            onClick={onHome}
          >
            <View className='payment-empty__btn-text'>返回首页</View>
          </View>
        </View>
      </View>
    </View>
  );
}

export function PaymentEmptyState({ onHome }: { onHome: () => void }) {
  return (
    <View className='payment-page'>
      <StatusBarAndNavBar title='支付订单' />
      <View className='payment-page__content payment-page__content--center'>
        <View className='payment-empty__illustration'>
          <Icon name='empty' className='payment-empty__illustration-icon' />
        </View>
        <Text className='payment-empty__text'>暂无订单信息</Text>
        <View className='payment-empty__btn' onClick={onHome}>
          <View className='payment-empty__btn-text'>返回首页</View>
        </View>
      </View>
    </View>
  );
}
