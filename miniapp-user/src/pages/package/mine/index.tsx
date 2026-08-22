import { useCallback, useEffect, useMemo, useState } from 'react';
import type { CSSProperties } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { fetchMyPackageList } from '@/api/package';
import { handleBusinessError } from '@/api/request';
import { Icon } from '@/components/common/Icon';
import type { UserPackageListItem, UserPackageStatus } from '@/types/package';

import './index.scss';

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20;
const PAGE_STYLE = {
  '--status-bar-height': `${STATUS_BAR_HEIGHT}px`,
} as CSSProperties;

const PAGE_PATHS = {
  packageList: '/pages/package/list/index',
  packageDetail: '/pages/package/mine/detail/index',
  refundApply: '/pages/refund/apply/index',
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

const GROUP_ORDER: UserPackageStatus[] = [
  'active',
  'exhausted',
  'expired',
  'refunded',
  'frozen',
];

const GROUP_LABELS: Record<UserPackageStatus, string> = {
  active: '可用',
  exhausted: '已耗尽',
  expired: '已过期',
  refunded: '已退款',
  frozen: '已冻结',
};

const MODE_LABELS: Record<UserPackageListItem['packageMode'], string> = {
  standard: '正价套餐',
  experience: '体验课',
  custom: '自定义',
};

export default function MyPackagePage() {
  const [items, setItems] = useState<UserPackageListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<Record<UserPackageStatus, boolean>>({
    active: true,
    exhausted: false,
    expired: false,
    refunded: false,
    frozen: false,
  });

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchMyPackageList();
      setItems(data);
    } catch (err) {
      setError(handleBusinessError(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const grouped = useMemo(() => {
    const map: Record<UserPackageStatus, UserPackageListItem[]> = {
      active: [],
      exhausted: [],
      expired: [],
      refunded: [],
      frozen: [],
    };
    for (const item of items) {
      if (map[item.status]) {
        map[item.status].push(item);
      }
    }
    return map;
  }, [items]);

  const summary = useMemo(() => {
    const activeItems = grouped.active;
    return {
      total: activeItems.reduce((sum, item) => sum + item.totalHours, 0),
      used: activeItems.reduce((sum, item) => sum + item.consumedCount, 0),
      remaining: activeItems.reduce(
        (sum, item) => sum + item.availableCount,
        0
      ),
    };
  }, [grouped]);

  function toggleGroup(status: UserPackageStatus) {
    setExpanded((prev) => ({ ...prev, [status]: !prev[status] }));
  }

  function navigateBack() {
    void Taro.navigateBack();
  }

  function navigateToPackageList() {
    void Taro.navigateTo({ url: PAGE_PATHS.packageList });
  }

  function navigateToPackageDetail(item: UserPackageListItem) {
    const query = new URLSearchParams({
      packageId: String(item.packageId),
    }).toString();
    void Taro.navigateTo({
      url: `${PAGE_PATHS.packageDetail}?${query}`,
    });
  }

  function navigateToRefund(item: UserPackageListItem) {
    const query = new URLSearchParams({
      packageId: String(item.packageId),
    }).toString();
    void Taro.navigateTo({
      url: `${PAGE_PATHS.refundApply}?${query}`,
    });
  }

  if (loading) {
    return <MyPackageSkeleton />;
  }

  if (error) {
    return (
      <View className='my-package-error' style={PAGE_STYLE}>
        <StatusBarAndNavBar title='我的套餐' onBack={navigateBack} />
        <View className='my-package-error__content'>
          <Icon name='error-circle' className='my-package-error__icon' />
          <Text className='my-package-error__text'>{error}</Text>
          <View className='my-package-error__button' onClick={loadData}>
            <Text className='my-package-error__button-text'>重新加载</Text>
          </View>
        </View>
      </View>
    );
  }

  const hasAnyPackage = items.length > 0;

  return (
    <View className='my-package' style={PAGE_STYLE}>
      <StatusBarAndNavBar title='我的套餐' onBack={navigateBack} />

      <View className='my-package__content'>
        {hasAnyPackage && <SummaryBar summary={summary} />}

        {!hasAnyPackage ? (
          <EmptyState onAction={navigateToPackageList} />
        ) : (
          <View className='my-package__groups'>
            {GROUP_ORDER.map((status) => (
              <PackageGroup
                key={status}
                status={status}
                items={grouped[status]}
                expanded={expanded[status]}
                onToggle={() => toggleGroup(status)}
                onItemClick={navigateToPackageDetail}
                onRefund={navigateToRefund}
              />
            ))}
          </View>
        )}
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
    <View className='my-package__header'>
      <View
        className='my-package__status-bar'
        style={{ height: `${STATUS_BAR_HEIGHT}px` }}
      />
      <View className='my-package__navbar'>
        <View className='my-package__back' onClick={onBack}>
          <Icon name='arrow-left' className='my-package__back-icon' />
        </View>
        <Text className='my-package__title'>{title}</Text>
        <View className='my-package__navbar-placeholder' />
      </View>
    </View>
  );
}

function SummaryBar({
  summary,
}: {
  summary: { total: number; used: number; remaining: number };
}) {
  return (
    <View className='summary-bar'>
      <View className='summary-bar__item'>
        <Text className='summary-bar__number' data-testid='summary-total'>
          {summary.total}
        </Text>
        <Text className='summary-bar__label'>总课时</Text>
      </View>
      <View className='summary-bar__divider' />
      <View className='summary-bar__item'>
        <Text className='summary-bar__number' data-testid='summary-used'>
          {summary.used}
        </Text>
        <Text className='summary-bar__label'>已上课时</Text>
      </View>
      <View className='summary-bar__divider' />
      <View className='summary-bar__item'>
        <Text className='summary-bar__number' data-testid='summary-remaining'>
          {summary.remaining}
        </Text>
        <Text className='summary-bar__label'>剩余课时</Text>
      </View>
    </View>
  );
}

function PackageGroup({
  status,
  items,
  expanded,
  onToggle,
  onItemClick,
  onRefund,
}: {
  status: UserPackageStatus;
  items: UserPackageListItem[];
  expanded: boolean;
  onToggle: () => void;
  onItemClick: (item: UserPackageListItem) => void;
  onRefund: (item: UserPackageListItem) => void;
}) {
  if (items.length === 0) {
    return null;
  }

  const showCoachResignedNotice =
    status === 'frozen' &&
    items.some((item) => item.frozenReason === 'coach_resigned');

  return (
    <View className='package-group'>
      <View className='package-group__header' onClick={onToggle}>
        <Text className='package-group__title'>{GROUP_LABELS[status]}</Text>
        <View className='package-group__meta'>
          <View className='package-group__count'>
            <Text className='package-group__count-text'>{items.length}</Text>
          </View>
          <Icon
            name='arrow-left'
            className={`package-group__arrow ${expanded ? 'package-group__arrow--expanded' : ''}`}
          />
        </View>
      </View>

      {expanded && (
        <View className='package-group__content'>
          {showCoachResignedNotice && (
            <View className='coach-resigned-notice'>
              <Icon name='warning' className='coach-resigned-notice__icon' />
              <Text className='coach-resigned-notice__text'>
                教练已离职，请更换教练或申请退款
              </Text>
            </View>
          )}

          {items.map((item) => (
            <PackageCard
              key={item.packageId}
              item={item}
              onClick={() => onItemClick(item)}
              onRefund={() => onRefund(item)}
            />
          ))}
        </View>
      )}
    </View>
  );
}

function PackageCard({
  item,
  onClick,
  onRefund,
}: {
  item: UserPackageListItem;
  onClick: () => void;
  onRefund: () => void;
}) {
  const statusConfig = STATUS_CONFIG[item.status];
  const packageModeLabel = MODE_LABELS[item.packageMode];

  return (
    <View className='package-card' onClick={onClick}>
      <View className='package-card__header'>
        <View className='package-card__title-row'>
          <Text className='package-card__name'>{item.packageName}</Text>
          <View className='package-card__tags'>
            <View
              className={`package-card__mode-tag package-card__mode-tag--${item.packageMode}`}
            >
              <Text className='package-card__mode-tag-text'>
                {packageModeLabel}
              </Text>
            </View>
            <View
              className={`package-card__status-tag ${statusConfig.tagClass}`}
            >
              <Text className='package-card__status-tag-text'>
                {statusConfig.label}
              </Text>
            </View>
          </View>
        </View>
        <View className='package-card__coach'>
          <View className='package-card__coach-avatar'>
            <Text className='package-card__coach-avatar-text'>
              {item.coachName?.charAt(0) || '?'}
            </Text>
          </View>
          <Text className='package-card__coach-info'>
            {item.coachName} · {item.teachingType} · {item.durationMinutes}
            分钟/节
          </Text>
        </View>
      </View>

      <View className='package-card__body'>
        <View className='package-card__hours'>
          <View className='package-card__hours-item'>
            <Text className='package-card__hours-label'>共</Text>
            <Text className='package-card__hours-number'>
              {item.totalHours}
            </Text>
            <Text className='package-card__hours-label'>节</Text>
          </View>
          <View className='package-card__hours-item'>
            <Text className='package-card__hours-label'>已用</Text>
            <Text className='package-card__hours-number package-card__hours-number--used'>
              {item.consumedCount}
            </Text>
            <Text className='package-card__hours-label'>节</Text>
          </View>
          <View className='package-card__hours-item'>
            <Text className='package-card__hours-label'>剩余</Text>
            <Text className='package-card__hours-number package-card__hours-number--remaining'>
              {item.availableCount}
            </Text>
            <Text className='package-card__hours-label'>节</Text>
          </View>
        </View>
        <Text className='package-card__expire'>
          {formatDate(item.expireAt)} 到期
        </Text>
      </View>

      <View className='package-card__footer'>
        <View className='package-card__price-row'>
          <Text className='package-card__price'>
            ¥{formatPrice(item.paidAmount)}
          </Text>
          {compareAmount(item.originalPrice, item.paidAmount) > 0 && (
            <Text className='package-card__original-price'>
              ¥{formatPrice(item.originalPrice)}
            </Text>
          )}
        </View>
        <View className='package-card__actions'>
          {item.canRefund && (
            <View
              className='package-card__refund-button'
              data-testid='refund-button'
              onClick={(e) => {
                e.stopPropagation();
                onRefund();
              }}
            >
              <Text className='package-card__refund-button-text'>申请退款</Text>
            </View>
          )}
          <Icon name='arrow-right' className='package-card__arrow' />
        </View>
      </View>
    </View>
  );
}

function EmptyState({ onAction }: { onAction: () => void }) {
  return (
    <View className='my-package-empty'>
      <View className='my-package-empty__icon-wrap'>
        <Icon name='empty' className='my-package-empty__icon' />
      </View>
      <Text className='my-package-empty__title'>您还没有套餐，去选购吧</Text>
      <Text className='my-package-empty__subtitle'>
        选购心仪教练的课程套餐，开启游泳之旅
      </Text>
      <View className='my-package-empty__button' onClick={onAction}>
        <Text className='my-package-empty__button-text'>去购买套餐</Text>
      </View>
    </View>
  );
}

function MyPackageSkeleton() {
  return (
    <View className='my-package my-package--skeleton' style={PAGE_STYLE}>
      <StatusBarAndNavBar title='我的套餐' onBack={() => {}} />
      <View className='my-package__content'>
        <View className='summary-bar summary-bar--skeleton' />
        <View className='package-group package-group--skeleton'>
          <View className='package-group__header' />
          <View className='package-card package-card--skeleton' />
          <View className='package-card package-card--skeleton' />
        </View>
      </View>
    </View>
  );
}

function formatPrice(price: string): string {
  const num = Number(price);
  if (Number.isNaN(num)) return '0.00';
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function compareAmount(a: string, b: string): number {
  return Math.round(Number(a) * 100) - Math.round(Number(b) * 100);
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  return dateStr.split('T')[0];
}
