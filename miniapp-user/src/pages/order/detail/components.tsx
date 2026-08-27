import { useMemo, useState } from 'react';
import { View, Text, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { Icon } from '@/components/common/Icon';
import type { IconName } from '@/components/common/Icon';
import type { OrderDetail, OrderStatus } from '@/types/order';

export const PAGE_PATHS = {
  orderList: '/pages/order/list/index',
  orderPayment: '/pages/order/payment/index',
  packageDetail: '/pages/my-package/detail/index',
  home: '/pages/index/index',
};

const STATUS_LABEL_MAP: Record<OrderStatus, string> = {
  pending_payment: '待支付',
  paid: '已支付',
  cancelled: '已取消',
  refund_pending: '退款审批中',
  refund_processing: '退款处理中',
  refunded: '已退款',
  rejected: '退款未通过',
  dispute_processing: '争议处理中',
};

const STATUS_DESC_MAP: Record<OrderStatus, string> = {
  pending_payment: '请在有效期内完成支付，逾期订单将自动取消',
  paid: '套餐已生效，可预约上课',
  cancelled: '订单已取消，可重新购买',
  refund_pending: '退款申请已提交，等待管理员审批',
  refund_processing: '管理员已批准，等待渠道原路退回',
  refunded: '退款已原路退回，预计 3-7 个工作日到账',
  rejected: '退款申请被驳回，可联系客服或前往套餐详情页查看',
  dispute_processing: '客服将在 3 个工作日内反馈结果',
};

const STATUS_THEME_MAP: Record<
  OrderStatus,
  { gradient: string; icon: IconName; iconText: string }
> = {
  pending_payment: {
    gradient: 'linear-gradient(90deg, #1890ff 0%, #0050b3 100%)',
    icon: 'time',
    iconText: '待',
  },
  paid: {
    gradient: 'linear-gradient(90deg, #52c41a 0%, #389e0d 100%)',
    icon: 'check',
    iconText: '成',
  },
  cancelled: {
    gradient: 'linear-gradient(90deg, #8c8c8c 0%, #595959 100%)',
    icon: 'error-circle',
    iconText: '取',
  },
  refund_pending: {
    gradient: 'linear-gradient(90deg, #faad14 0%, #fa8c16 100%)',
    icon: 'warning',
    iconText: '退',
  },
  refund_processing: {
    gradient: 'linear-gradient(90deg, #faad14 0%, #fa8c16 100%)',
    icon: 'warning',
    iconText: '退',
  },
  refunded: {
    gradient: 'linear-gradient(90deg, #1890ff 0%, #0050b3 100%)',
    icon: 'check',
    iconText: '完',
  },
  rejected: {
    gradient: 'linear-gradient(90deg, #ff4d4f 0%, #cf1322 100%)',
    icon: 'error-circle',
    iconText: '拒',
  },
  dispute_processing: {
    gradient: 'linear-gradient(90deg, #faad14 0%, #fa8c16 100%)',
    icon: 'warning',
    iconText: '议',
  },
};

const TEACHING_TYPE_MAP: Record<string, string> = {
  one_on_one: '一对一',
  one_on_two: '一对二',
  one_on_three: '一对三',
};

const CHANNEL_LABEL_MAP: Record<string, string> = {
  wechat: '微信支付',
  alipay: '支付宝支付',
};

export function useStatusBarHeight(): number {
  const [height] = useState(
    () => Taro.getSystemInfoSync().statusBarHeight || 20
  );
  return height;
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

export function formatPrice(price: string): string {
  const num = Number(price);
  if (Number.isNaN(num)) return price;
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

export function formatTeachingType(type?: string): string {
  return TEACHING_TYPE_MAP[type ?? ''] ?? type ?? '--';
}

export function getStatusLabel(status: OrderStatus): string {
  return STATUS_LABEL_MAP[status] ?? status;
}

export function getStatusDesc(status: OrderStatus): string {
  return STATUS_DESC_MAP[status] ?? '';
}

export function getPackageModeLabel(order: OrderDetail): string {
  if (order.packageMode === 'experience') return '体验课';
  if (order.packageMode === 'custom') return '自定义套餐';
  return '正价套餐';
}

export function getChannelLabel(channel?: string): string {
  return CHANNEL_LABEL_MAP[channel ?? ''] ?? '--';
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

export function computeRemainingSeconds(expireAt: string): number {
  const expireTime = new Date(expireAt).getTime();
  if (Number.isNaN(expireTime)) return 0;
  return Math.floor((expireTime - Date.now()) / 1000);
}

export function StatusBarAndNavBar({
  title,
  onBack,
}: {
  title: string;
  onBack: () => void;
}) {
  const statusBarHeight = useStatusBarHeight();
  return (
    <View className='order-detail-page__header'>
      <View
        className='order-detail-page__status-bar'
        style={{ height: `${statusBarHeight}px` }}
      />
      <View className='order-detail-page__navbar'>
        <View className='order-detail-page__back' onClick={onBack}>
          <Icon name='arrow-left' className='order-detail-page__back-icon' />
        </View>
        <View className='order-detail-page__title'>{title}</View>
        <View className='order-detail-page__navbar-placeholder' />
      </View>
    </View>
  );
}

export function StatusHeader({
  order,
  remainingSeconds,
}: {
  order: OrderDetail;
  remainingSeconds: number | null;
}) {
  const theme = STATUS_THEME_MAP[order.status];
  const statusLabel = getStatusLabel(order.status);
  const displayAmount =
    order.status === 'pending_payment'
      ? order.amount
      : (order.paidAmount ?? order.amount);

  const desc = useMemo(() => {
    if (order.status === 'pending_payment') {
      return `剩余支付时间：${formatCountdown(remainingSeconds)}`;
    }
    if (order.status === 'rejected' && order.rejectedReason) {
      return `原因：${order.rejectedReason}`;
    }
    return getStatusDesc(order.status);
  }, [order.status, order.rejectedReason, remainingSeconds]);

  return (
    <View
      className='order-detail-header'
      style={{ background: theme.gradient }}
    >
      <View className='order-detail-header__main'>
        <View className='order-detail-header__icon-wrap'>
          <Icon name={theme.icon} className='order-detail-header__icon' />
        </View>
        <View className='order-detail-header__text'>
          <View className='order-detail-header__status'>{statusLabel}</View>
          <Text className='order-detail-header__amount'>
            ¥{formatPrice(displayAmount)}
          </Text>
        </View>
      </View>
      <View className='order-detail-header__desc'>
        <Text className='order-detail-header__desc-text'>{desc}</Text>
      </View>
    </View>
  );
}

export function PurchaseSnapshotCard({ order }: { order: OrderDetail }) {
  const strokeText =
    order.strokeNames && order.strokeNames.length > 0
      ? order.strokeNames.join('、')
      : '全部泳姿';
  const refundRuleText =
    order.refundEnabled === false
      ? '不支持退款'
      : order.refundRatio !== undefined && order.refundValidDays !== undefined
        ? `开课后 ${order.refundValidDays} 天内可退，退款比例 ${Math.round(order.refundRatio * 100)}%`
        : '--';

  const rows = [
    { label: '套餐名称', value: order.packageName },
    { label: '套餐模式', value: getPackageModeLabel(order) },
    { label: '教练', value: order.coachName },
    { label: '教学类型', value: formatTeachingType(order.teachingType) },
    { label: '泳姿', value: strokeText },
    { label: '课时数', value: `${order.totalHours} 节` },
    { label: '每节课时长', value: `${order.durationMinutes} 分钟` },
    { label: '有效期', value: `${order.validDays} 天` },
    { label: '原价', value: `¥${formatPrice(order.originalAmount)}` },
    {
      label: '实付价',
      value: `¥${formatPrice(order.amount)}`,
      highlight: true,
    },
    { label: '退款规则', value: refundRuleText },
  ];

  return (
    <View className='order-detail-card'>
      <View className='order-detail-card__header'>
        <View className='order-detail-card__title'>购买时套餐信息</View>
        <View className='order-detail-card__tag'>
          <View className='order-detail-card__tag-text'>购买时快照</View>
        </View>
      </View>
      {rows.map((row) => (
        <View key={row.label} className='order-detail-info-row'>
          <View className='order-detail-info-row__label'>{row.label}</View>
          <Text
            className={`order-detail-info-row__value ${
              row.highlight ? 'order-detail-info-row__value--highlight' : ''
            }`}
          >
            {row.value}
          </Text>
        </View>
      ))}
    </View>
  );
}

function PackageStatusTag({
  status,
}: {
  status?: OrderDetail['packageStatus'];
}) {
  if (status === 'expired') {
    return (
      <View className='package-status-tag package-status-tag--expired'>
        <Text className='package-status-tag__text'>已过期</Text>
      </View>
    );
  }
  if (status === 'frozen') {
    return (
      <View className='package-status-tag package-status-tag--frozen'>
        <Text className='package-status-tag__text'>冻结中</Text>
      </View>
    );
  }
  if (status === 'exhausted') {
    return (
      <View className='package-status-tag package-status-tag--exhausted'>
        <Text className='package-status-tag__text'>已用完</Text>
      </View>
    );
  }
  return (
    <View className='package-status-tag package-status-tag--active'>
      <Text className='package-status-tag__text'>可用</Text>
    </View>
  );
}

const ORDER_STATUS_TAG_THEME: Record<
  OrderStatus,
  { text: string; className: string }
> = {
  pending_payment: { text: '待支付', className: 'package-status-tag--warning' },
  paid: { text: '已支付', className: 'package-status-tag--success' },
  cancelled: { text: '已取消', className: 'package-status-tag--default' },
  refund_pending: { text: '退款审批中', className: 'package-status-tag--warning' },
  refund_processing: { text: '退款处理中', className: 'package-status-tag--warning' },
  refunded: { text: '已退款', className: 'package-status-tag--default' },
  rejected: { text: '退款未通过', className: 'package-status-tag--danger' },
  dispute_processing: { text: '争议处理中', className: 'package-status-tag--warning' },
};

function OrderStatusTag({ status }: { status: OrderStatus }) {
  const theme = ORDER_STATUS_TAG_THEME[status];
  return (
    <View className={`package-status-tag ${theme.className}`}>
      <Text className='package-status-tag__text'>{theme.text}</Text>
    </View>
  );
}

export function PackageUsageCard({ order }: { order: OrderDetail }) {
  const hasExpiredNotice = order.packageStatus === 'expired';
  const hasFrozenNotice =
    order.packageStatus === 'frozen' ||
    ['refund_pending', 'refund_processing', 'dispute_processing'].includes(
      order.status
    );

  const showOrderStatus = order.status !== 'paid';

  return (
    <View className='order-detail-card'>
      <View className='order-detail-card__title'>套餐状态</View>

      {hasExpiredNotice && (
        <View className='order-detail-notice order-detail-notice--expired'>
          <Icon name='warning' className='order-detail-notice__icon' />
          <Text className='order-detail-notice__text'>
            套餐已过期，退款申请请前往套餐详情页
          </Text>
        </View>
      )}

      {hasFrozenNotice && !hasExpiredNotice && (
        <View className='order-detail-notice order-detail-notice--warning'>
          <Icon name='warning' className='order-detail-notice__icon' />
          <Text className='order-detail-notice__text'>
            退款处理中，暂不可预约
          </Text>
        </View>
      )}

      <View className='package-usage-card__coach'>
        {order.coachAvatar ? (
          <Image
            className='package-usage-card__avatar'
            src={order.coachAvatar}
            mode='aspectFill'
          />
        ) : (
          <View className='package-usage-card__avatar package-usage-card__avatar--placeholder'>
            <View className='package-usage-card__avatar-text'>
              {order.coachName.slice(0, 1)}
            </View>
          </View>
        )}
        <View className='package-usage-card__coach-info'>
          <View className='package-usage-card__name-row'>
            <View className='package-usage-card__name'>{order.coachName}</View>
          </View>
          <View className='package-usage-card__desc'>
            {formatTeachingType(order.teachingType)} · {order.durationMinutes} 分钟/节
          </View>
        </View>
        {showOrderStatus ? (
          <OrderStatusTag status={order.status} />
        ) : (
          <PackageStatusTag status={order.packageStatus} />
        )}
      </View>

      <View className='package-usage-card__hours'>
        <View className='package-usage-card__hour-item'>
          <Text className='package-usage-card__hour-label'>共</Text>
          <Text className='package-usage-card__hour-value'>
            {order.totalHours ?? '--'}
          </Text>
          <Text className='package-usage-card__hour-unit'>节</Text>
        </View>
        <View className='package-usage-card__hour-item'>
          <Text className='package-usage-card__hour-label'>已用</Text>
          <Text className='package-usage-card__hour-value'>
            {order.usedHours ?? '--'}
          </Text>
          <Text className='package-usage-card__hour-unit'>节</Text>
        </View>
        <View className='package-usage-card__hour-item'>
          <Text className='package-usage-card__hour-label'>剩余</Text>
          <Text className='package-usage-card__hour-value'>
            {order.availableHours ?? '--'}
          </Text>
          <Text className='package-usage-card__hour-unit'>节</Text>
        </View>
      </View>

      <View className='package-usage-card__validity'>
        <Icon name='time' className='package-usage-card__validity-icon' />
        <Text className='package-usage-card__validity-text'>
          {order.packageExpireAt
            ? `有效期至 ${formatDateTime(order.packageExpireAt)}`
            : '支付后生效'}
        </Text>
      </View>
    </View>
  );
}

export function OrderInfoCard({ order }: { order: OrderDetail }) {
  const baseRows = [
    { label: '订单编号', value: order.orderNo },
    { label: '创建时间', value: formatDateTime(order.createdAt) },
  ];

  const paymentRows =
    order.status !== 'pending_payment' && order.status !== 'cancelled'
      ? [
          { label: '支付时间', value: formatDateTime(order.paidAt) },
          { label: '支付方式', value: getChannelLabel(order.channel) },
        ]
      : [];

  const refundRows = [
    'refund_pending',
    'refund_processing',
    'dispute_processing',
    'refunded',
    'rejected',
  ].includes(order.status)
    ? [
        {
          label: '退款金额',
          value: `¥${formatPrice(order.refundAmount ?? '0')}`,
        },
        { label: '退款原因', value: order.refundReason ?? '--' },
      ]
    : [];

  const refundPaidRow =
    order.status === 'refunded'
      ? [
          {
            label: '退款到账',
            value: `${getChannelLabel(order.channel)} · ${formatDateTime(order.refundPaidAt)}`,
          },
        ]
      : [];

  const rows = [...baseRows, ...paymentRows, ...refundRows, ...refundPaidRow];

  return (
    <View className='order-detail-card'>
      <View className='order-detail-card__title'>订单信息</View>
      {rows.map((row) => (
        <View key={row.label} className='order-detail-info-row'>
          <View className='order-detail-info-row__label'>{row.label}</View>
          <View className='order-detail-info-row__value'>{row.value}</View>
        </View>
      ))}
    </View>
  );
}

const REFUND_STEPS = ['提交申请', '管理员审批', '渠道退款', '退款到账'];

function getRefundStepInfo(status: OrderStatus): {
  activeIndex: number;
  failedIndex?: number;
  description: string;
} {
  switch (status) {
    case 'refund_pending':
      return {
        activeIndex: 1,
        description: '退款申请已提交，等待管理员审批',
      };
    case 'dispute_processing':
      return {
        activeIndex: 1,
        description: '管理员正在审核您提交的证明材料，3 个工作日内反馈',
      };
    case 'refund_processing':
      return {
        activeIndex: 2,
        description: '管理员已批准退款，等待微信/支付宝渠道原路退回',
      };
    case 'refunded':
      return {
        activeIndex: 3,
        description: '退款已成功，预计 3-7 个工作日到账',
      };
    case 'rejected':
      return {
        activeIndex: 1,
        failedIndex: 1,
        description: '退款申请被管理员驳回，可联系客服或前往套餐详情页查看',
      };
    default:
      return { activeIndex: 0, description: '' };
  }
}

export function RefundProgressCard({ status }: { status: OrderStatus }) {
  const { activeIndex, failedIndex, description } = getRefundStepInfo(status);

  return (
    <View className='order-detail-card'>
      <View className='order-detail-card__title'>退款进度</View>
      <View className='refund-progress'>
        <View className='refund-progress__track'>
          {REFUND_STEPS.map((_, index) => {
            const isActive = index <= activeIndex;
            const isFailed = failedIndex !== undefined && index === failedIndex;
            return (
              <View
                key={index}
                className={`refund-progress__node ${
                  isActive ? 'refund-progress__node--active' : ''
                } ${isFailed ? 'refund-progress__node--failed' : ''}`}
              >
                <Text className='refund-progress__node-text'>{index + 1}</Text>
              </View>
            );
          })}
          <View
            className='refund-progress__bar'
            style={{
              width: `${(activeIndex / (REFUND_STEPS.length - 1)) * 100}%`,
            }}
          />
        </View>
        <View className='refund-progress__labels'>
          {REFUND_STEPS.map((label) => (
            <View key={label} className='refund-progress__label'>
              {label}
            </View>
          ))}
        </View>
      </View>
      <View className='refund-progress__desc'>
        <Icon name='notice' className='refund-progress__desc-icon' />
        <Text className='refund-progress__desc-text'>{description}</Text>
      </View>
    </View>
  );
}

export function RefundAmountInfo({ order }: { order: OrderDetail }) {
  const total = order.totalHours || 1;
  const available = order.availableHours ?? 0;
  const reserved = order.reservedHours ?? 0;

  const displayRefundAmount = useMemo(() => {
    if (order.refundAmount) {
      return order.refundAmount;
    }
    // 后端未返回预计退款金额时的兜底示例（非最终金额）
    const ratio = order.refundRatio ?? 1;
    const unitPrice = Number(order.paidAmount || order.amount) / total;
    const fallbackAmount = unitPrice * (available + reserved) * ratio;
    return fallbackAmount.toFixed(2);
  }, [
    order.refundAmount,
    order.amount,
    order.paidAmount,
    order.refundRatio,
    total,
    available,
    reserved,
  ]);

  return (
    <View className='order-detail-card'>
      <View className='order-detail-card__title'>退款金额计算</View>
      <View className='refund-amount-info__box'>
        <View className='refund-amount-info__formula'>
          <Icon name='notice' className='refund-amount-info__icon' />
          <Text className='refund-amount-info__formula-text'>
            退款金额 = 套餐价 ×（剩余课时 + 已预约课时）/ 总课时 × 退款比例
          </Text>
        </View>
        <Text className='refund-amount-info__example'>
          示例：{total} 节课共 ¥{formatPrice(order.amount)}，当前剩余{' '}
          {available} 节、已预约 {reserved} 节，预计可退 ¥
          {formatPrice(displayRefundAmount)}
        </Text>
        <View className='refund-amount-info__tip'>
          MVP 阶段不收取退款手续费，最终以管理员审批结果为准。
        </View>
        {order.frozenReason === 'coach_resigned' && (
          <Text className='refund-amount-info__tip refund-amount-info__tip--highlight'>
            教练离职场景下可申请 100% 全额退款。
          </Text>
        )}
      </View>
    </View>
  );
}

export function BottomActionBar({
  order,
  onPay,
  onViewPackage,
  onDelete,
  onViewProgress,
  onContactService,
}: {
  order: OrderDetail;
  onPay: () => void;
  onViewPackage: () => void;
  onDelete: () => void;
  onViewProgress: () => void;
  onContactService: () => void;
}) {
  const { status } = order;
  const isExpired = order.packageStatus === 'expired';

  const primaryButton = useMemo(() => {
    if (status === 'pending_payment') {
      return { text: '去支付', action: onPay };
    }
    if (status === 'paid' || isExpired) {
      return { text: '查看套餐', action: onViewPackage };
    }
    if (status === 'cancelled' || status === 'refunded') {
      return { text: '删除记录', action: onDelete };
    }
    if (
      status === 'refund_pending' ||
      status === 'refund_processing' ||
      status === 'dispute_processing'
    ) {
      return { text: '查看进度', action: onViewProgress };
    }
    if (status === 'rejected') {
      return { text: '联系客服', action: onContactService };
    }
    return null;
  }, [
    status,
    isExpired,
    onPay,
    onViewPackage,
    onDelete,
    onViewProgress,
    onContactService,
  ]);

  const secondaryButton = useMemo(() => {
    if (status === 'pending_payment') {
      return { text: '取消订单', action: onDelete };
    }
    if (
      status === 'refund_pending' ||
      status === 'refund_processing' ||
      status === 'dispute_processing'
    ) {
      return { text: '联系客服', action: onContactService };
    }
    if (status === 'rejected') {
      return { text: '查看套餐', action: onViewPackage };
    }
    return null;
  }, [status, onDelete, onContactService, onViewPackage]);

  if (!primaryButton && !secondaryButton) return null;

  return (
    <View className='order-detail-bottom-bar'>
      {secondaryButton && (
        <View
          className='order-detail-bottom-bar__secondary'
          onClick={secondaryButton.action}
        >
          <Text className='order-detail-bottom-bar__secondary-text'>
            {secondaryButton.text}
          </Text>
        </View>
      )}
      {primaryButton && (
        <View
          className='order-detail-bottom-bar__primary'
          onClick={primaryButton.action}
        >
          <Text className='order-detail-bottom-bar__primary-text'>
            {primaryButton.text}
          </Text>
        </View>
      )}
    </View>
  );
}

export function OrderDetailSkeleton() {
  return (
    <View className='order-detail-page'>
      <View className='order-detail-page__header order-detail-page__header--skeleton'>
        <View
          className='order-detail-page__status-bar'
          style={{ height: '20px' }}
        />
        <View className='order-detail-page__navbar'>
          <View className='order-detail-page__back' />
          <View className='order-detail-skeleton order-detail-skeleton--title' />
          <View className='order-detail-page__navbar-placeholder' />
        </View>
      </View>
      <View className='order-detail-page__content order-detail-page__content--loading'>
        <View className='order-detail-skeleton order-detail-skeleton--header' />
        <View className='order-detail-skeleton order-detail-skeleton--card' />
        <View className='order-detail-skeleton order-detail-skeleton--card' />
        <View className='order-detail-skeleton order-detail-skeleton--card' />
      </View>
    </View>
  );
}

export function OrderDetailErrorState({
  message,
  onRetry,
  onBack,
}: {
  message: string;
  onRetry: () => void;
  onBack: () => void;
}) {
  return (
    <View className='order-detail-page'>
      <StatusBarAndNavBar title='订单详情' onBack={onBack} />
      <View className='order-detail-page__content order-detail-page__content--center'>
        <View className='order-detail-empty__illustration'>
          <Icon name='error-circle' className='order-detail-empty__icon' />
        </View>
        <Text className='order-detail-empty__text'>{message}</Text>
        <View className='order-detail-empty__actions'>
          <View className='order-detail-empty__btn' onClick={onRetry}>
            <View className='order-detail-empty__btn-text'>重新加载</View>
          </View>
          <View
            className='order-detail-empty__btn order-detail-empty__btn--secondary'
            onClick={onBack}
          >
            <View className='order-detail-empty__btn-text'>返回订单列表</View>
          </View>
        </View>
      </View>
    </View>
  );
}

export function OrderDetailEmptyState({ onBack }: { onBack: () => void }) {
  return (
    <View className='order-detail-page'>
      <StatusBarAndNavBar title='订单详情' onBack={onBack} />
      <View className='order-detail-page__content order-detail-page__content--center'>
        <View className='order-detail-empty__illustration'>
          <Icon name='empty' className='order-detail-empty__icon' />
        </View>
        <Text className='order-detail-empty__text'>暂无订单信息</Text>
        <View className='order-detail-empty__btn' onClick={onBack}>
          <View className='order-detail-empty__btn-text'>返回订单列表</View>
        </View>
      </View>
    </View>
  );
}
