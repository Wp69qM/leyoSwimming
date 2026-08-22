import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { CSSProperties } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { fetchOrderList } from '@/api/order';
import { handleBusinessError } from '@/api/request';
import { Icon } from '@/components/common/Icon';
import type { OrderListItem, OrderStatus, OrderTab } from '@/types/order';
import {
  formatDateTime,
  formatPrice,
  formatTeachingType,
} from '../detail/components';
import './index.scss';

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20;
const PAGE_SIZE = 10;
const PAGE_STYLE = {
  '--status-bar-height': `${STATUS_BAR_HEIGHT}px`,
} as CSSProperties;

const TABS: { key: OrderTab; label: string }[] = [
  { key: 'all', label: '全部' },
  { key: 'pending_payment', label: '待支付' },
  { key: 'completed', label: '已完成' },
  { key: 'refunding', label: '退款中' },
  { key: 'cancelled', label: '已取消' },
];

const STATUS_LABEL_MAP: Record<OrderStatus, string> = {
  pending_payment: '待支付',
  paid: '已支付',
  cancelled: '已取消',
  refund_pending: '退款中',
  refund_processing: '处理中',
  refunded: '已退款',
  rejected: '退款被拒',
  dispute_processing: '争议中',
};

const STATUS_THEME_MAP: Record<OrderStatus, { bg: string; text: string }> = {
  pending_payment: { bg: '#fff7e6', text: '#fa8c16' },
  paid: { bg: '#f6ffed', text: '#52c41a' },
  cancelled: { bg: '#f5f5f5', text: '#8c8c8c' },
  refund_pending: { bg: '#e6f7ff', text: '#1890ff' },
  refund_processing: { bg: '#e6f7ff', text: '#1890ff' },
  refunded: { bg: '#f5f5f5', text: '#8c8c8c' },
  rejected: { bg: '#fff1f0', text: '#ff4d4f' },
  dispute_processing: { bg: '#fff1f0', text: '#ff4d4f' },
};

export default function OrderListPage() {
  const [items, setItems] = useState<OrderListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<OrderTab>('all');
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [loadMoreError, setLoadMoreError] = useState<string | null>(null);
  const latestRequestRef = useRef<{ tab: OrderTab; page: number } | null>(null);

  Taro.useReachBottom(() => {
    if (!loadingMore && hasMore && !loading) {
      void loadOrders(activeTab, page + 1, true);
    }
  });

  const loadOrders = useCallback(
    async (tab: OrderTab, targetPage: number, append: boolean) => {
      const requestKey = { tab, page: targetPage };
      latestRequestRef.current = requestKey;
      if (targetPage === 1) {
        setLoading(true);
        setLoadMoreError(null);
      } else {
        setLoadingMore(true);
        setLoadMoreError(null);
      }
      setError(null);
      try {
        const result = await fetchOrderList(tab, targetPage, PAGE_SIZE);
        if (
          latestRequestRef.current?.tab !== requestKey.tab ||
          latestRequestRef.current?.page !== requestKey.page
        ) {
          return;
        }
        const records = result.records ?? [];
        setItems((prev) => (append ? [...prev, ...records] : records));
        setPage(targetPage);
        setHasMore(targetPage < result.pages);
      } catch (err) {
        if (
          latestRequestRef.current?.tab !== requestKey.tab ||
          latestRequestRef.current?.page !== requestKey.page
        ) {
          return;
        }
        if (append) {
          setLoadMoreError(handleBusinessError(err));
        } else {
          setError(handleBusinessError(err));
          setItems([]);
        }
      } finally {
        if (
          latestRequestRef.current?.tab === requestKey.tab &&
          latestRequestRef.current?.page === requestKey.page
        ) {
          setLoading(false);
          setLoadingMore(false);
        }
      }
    },
    []
  );

  useEffect(() => {
    void loadOrders(activeTab, 1, false);
  }, [activeTab, loadOrders]);

  const handleTabChange = useCallback((tab: OrderTab) => {
    setActiveTab(tab);
  }, []);

  const handleRetry = useCallback(() => {
    void loadOrders(activeTab, 1, false);
  }, [activeTab, loadOrders]);

  const navigateBack = useCallback(() => {
    void Taro.navigateBack();
  }, []);

  if (loading && items.length === 0 && !error) {
    return (
      <View
        className='order-list-page order-list-page--skeleton'
        style={PAGE_STYLE}
      >
        <StatusBarAndNavBar title='我的订单' onBack={navigateBack} />
        <TabBar activeTab={activeTab} onChange={handleTabChange} />
        <OrderListSkeleton />
      </View>
    );
  }

  if (error && items.length === 0) {
    return (
      <View className='order-list-page' style={PAGE_STYLE}>
        <StatusBarAndNavBar title='我的订单' onBack={navigateBack} />
        <TabBar activeTab={activeTab} onChange={handleTabChange} />
        <ErrorState message={error} onRetry={handleRetry} />
      </View>
    );
  }

  return (
    <View className='order-list-page' style={PAGE_STYLE}>
      <StatusBarAndNavBar title='我的订单' onBack={navigateBack} />
      <TabBar activeTab={activeTab} onChange={handleTabChange} />

      {items.length === 0 && !loading ? (
        <EmptyState tab={activeTab} />
      ) : (
        <View className='order-list-page__content'>
          {items.map((item) => (
            <OrderCard
              key={item.orderId}
              order={item}
              onRefresh={() => {
                void loadOrders(activeTab, 1, false);
              }}
            />
          ))}
          <ListFooter
            loading={loadingMore}
            hasMore={hasMore}
            loadMoreError={loadMoreError}
            onRetry={() => {
              void loadOrders(activeTab, page + 1, true);
            }}
          />
        </View>
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
    <View className='order-list-page__header'>
      <View
        className='order-list-page__status-bar'
        style={{ height: `${STATUS_BAR_HEIGHT}px` }}
      />
      <View className='order-list-page__navbar'>
        <View className='order-list-page__back' onClick={onBack}>
          <Icon name='arrow-left' className='order-list-page__back-icon' />
        </View>
        <Text className='order-list-page__title'>{title}</Text>
        <View className='order-list-page__navbar-placeholder' />
      </View>
    </View>
  );
}

function TabBar({
  activeTab,
  onChange,
}: {
  activeTab: OrderTab;
  onChange: (tab: OrderTab) => void;
}) {
  return (
    <View className='order-list-page__tabs'>
      {TABS.map((tab) => {
        const isActive = tab.key === activeTab;
        return (
          <View
            key={tab.key}
            className={`order-list-page__tab ${
              isActive ? 'order-list-page__tab--active' : ''
            }`}
            onClick={() => onChange(tab.key)}
          >
            <Text
              className={`order-list-page__tab-text ${
                isActive ? 'order-list-page__tab-text--active' : ''
              }`}
            >
              {tab.label}
            </Text>
          </View>
        );
      })}
    </View>
  );
}

function OrderCard({
  order,
  onRefresh,
}: {
  order: OrderListItem;
  onRefresh: () => void;
}) {
  const hasNotifiedRef = useRef(false);
  const onRefreshRef = useRef(onRefresh);
  onRefreshRef.current = onRefresh;

  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(
    () => {
      if (order.status !== 'pending_payment') return null;
      const expireTime = new Date(order.expireAt).getTime();
      if (Number.isNaN(expireTime)) return 0;
      return Math.floor((expireTime - Date.now()) / 1000);
    }
  );

  useEffect(() => {
    if (order.status !== 'pending_payment') return undefined;
    if (hasNotifiedRef.current) return undefined;
    const interval = setInterval(() => {
      const expireTime = new Date(order.expireAt).getTime();
      const seconds = Number.isNaN(expireTime)
        ? 0
        : Math.floor((expireTime - Date.now()) / 1000);
      setRemainingSeconds(seconds);
      if (seconds <= 0) {
        clearInterval(interval);
        if (!hasNotifiedRef.current) {
          hasNotifiedRef.current = true;
          onRefreshRef.current();
        }
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [order.status, order.expireAt]);

  const navigateToDetail = useCallback(() => {
    void Taro.navigateTo({
      url: `/pages/order/detail/index?orderId=${order.orderId}`,
    });
  }, [order.orderId]);

  return (
    <View className='order-card' onClick={navigateToDetail}>
      <View className='order-card__header'>
        <Text className='order-card__no'>订单号 {order.orderNo}</Text>
        <StatusBadge
          status={order.status}
          pulse={order.status === 'refund_processing'}
        />
      </View>
      <View className='order-card__divider' />
      <View className='order-card__body'>
        <View className='order-card__avatar'>
          <Text className='order-card__avatar-text'>
            {order.coachName.slice(0, 1)}
          </Text>
        </View>
        <View className='order-card__info'>
          <View className='order-card__title-row'>
            <Text className='order-card__name'>{order.packageName}</Text>
            <PackageModeTag mode={order.packageMode} />
          </View>
          <Text className='order-card__coach'>
            {order.coachName} · {formatTeachingType(order.teachingType)}
          </Text>
          <Text className='order-card__hours'>{order.totalHours} 节</Text>
          <View className='order-card__meta'>
            <Text className='order-card__time'>
              {formatDateTime(order.createdAt)}
            </Text>
            {remainingSeconds !== null && (
              <CountdownTag seconds={remainingSeconds} />
            )}
          </View>
        </View>
        <View className='order-card__amount'>
          <Text className='order-card__amount-label'>订单金额</Text>
          <Text className='order-card__amount-value'>
            ¥{formatPrice(order.amount)}
          </Text>
        </View>
      </View>
      <ActionButtons order={order} onRefresh={onRefresh} />
    </View>
  );
}

function StatusBadge({
  status,
  pulse,
}: {
  status: OrderStatus;
  pulse?: boolean;
}) {
  const theme = STATUS_THEME_MAP[status];
  const label = STATUS_LABEL_MAP[status];
  return (
    <View
      className={`status-badge ${pulse ? 'status-badge--pulse' : ''}`}
      style={{ backgroundColor: theme.bg }}
    >
      <Text className='status-badge__text' style={{ color: theme.text }}>
        {label}
      </Text>
    </View>
  );
}

function PackageModeTag({ mode }: { mode: OrderListItem['packageMode'] }) {
  const label = useMemo(() => {
    if (mode === 'experience') return '体验课';
    if (mode === 'custom') return '自定义套餐';
    return '正价套餐';
  }, [mode]);
  return (
    <View className='package-mode-tag'>
      <Text className='package-mode-tag__text'>{label}</Text>
    </View>
  );
}

function CountdownTag({ seconds }: { seconds: number }) {
  const display = seconds <= 0 ? '00:00:00' : formatCountdown(seconds);
  const urgent = seconds > 0 && seconds <= 3600;
  return (
    <View className={`countdown-tag ${urgent ? 'countdown-tag--urgent' : ''}`}>
      <Icon name='time' className='countdown-tag__icon' />
      <Text className='countdown-tag__text'>剩 {display}</Text>
    </View>
  );
}

function ActionButtons({
  order,
  onRefresh,
}: {
  order: OrderListItem;
  onRefresh: () => void;
}) {
  const primary = useMemo(() => {
    switch (order.status) {
      case 'pending_payment':
        return {
          text: '去支付',
          action: () => {
            void Taro.navigateTo({
              url: `/pages/order/payment/index?orderId=${order.orderId}`,
            });
          },
        };
      case 'paid':
      case 'refunded':
      case 'rejected':
        return {
          text: '查看详情',
          action: () => {
            void Taro.navigateTo({
              url: `/pages/order/detail/index?orderId=${order.orderId}`,
            });
          },
        };
      case 'cancelled':
        return {
          text: '删除记录',
          action: async () => {
            const res = await Taro.showModal({
              title: '确认删除',
              content: '删除后订单记录将无法恢复',
              confirmText: '删除',
              confirmColor: '#ff4d4f',
            });
            if (res.confirm) {
              void Taro.showToast({
                title: '删除功能开发中',
                icon: 'none',
              });
              onRefresh();
            }
          },
        };
      case 'refund_pending':
      case 'refund_processing':
      case 'dispute_processing':
        return {
          text: '查看进度',
          action: () => {
            void Taro.navigateTo({
              url: `/pages/order/detail/index?orderId=${order.orderId}`,
            });
          },
        };
      default:
        return null;
    }
  }, [order.status, order.orderId, onRefresh]);

  const secondary = useMemo(() => {
    switch (order.status) {
      case 'pending_payment':
        return {
          text: '取消订单',
          action: async () => {
            const res = await Taro.showModal({
              title: '确认取消',
              content: '取消后订单将无法恢复',
              confirmText: '取消订单',
              confirmColor: '#ff4d4f',
            });
            if (res.confirm) {
              void Taro.showToast({
                title: '取消功能开发中',
                icon: 'none',
              });
              onRefresh();
            }
          },
        };
      case 'refunded':
        return {
          text: '删除记录',
          action: async () => {
            const res = await Taro.showModal({
              title: '确认删除',
              content: '删除后订单记录将无法恢复',
              confirmText: '删除',
              confirmColor: '#ff4d4f',
            });
            if (res.confirm) {
              void Taro.showToast({
                title: '删除功能开发中',
                icon: 'none',
              });
              onRefresh();
            }
          },
        };
      case 'refund_pending':
      case 'refund_processing':
      case 'dispute_processing':
        return {
          text: '取消退款',
          action: async () => {
            const res = await Taro.showModal({
              title: '确认取消退款',
              content: '取消退款后订单将恢复为已支付状态',
              confirmText: '取消退款',
              confirmColor: '#ff4d4f',
            });
            if (res.confirm) {
              void Taro.showToast({
                title: '取消退款功能开发中',
                icon: 'none',
              });
              onRefresh();
            }
          },
        };
      default:
        return null;
    }
  }, [order.status, onRefresh]);

  if (!primary && !secondary) return null;

  return (
    <View className='order-card__actions'>
      {secondary && (
        <View
          className='order-card__btn order-card__btn--secondary'
          onClick={(e) => {
            e.stopPropagation();
            void secondary.action();
          }}
        >
          <Text className='order-card__btn-text order-card__btn-text--secondary'>
            {secondary.text}
          </Text>
        </View>
      )}
      {primary && (
        <View
          className='order-card__btn order-card__btn--primary'
          onClick={(e) => {
            e.stopPropagation();
            void primary.action();
          }}
        >
          <Text className='order-card__btn-text order-card__btn-text--primary'>
            {primary.text}
          </Text>
        </View>
      )}
    </View>
  );
}

function ListFooter({
  loading,
  hasMore,
  loadMoreError,
  onRetry,
}: {
  loading: boolean;
  hasMore: boolean;
  loadMoreError: string | null;
  onRetry: () => void;
}) {
  return (
    <View className='order-list-page__footer'>
      {loadMoreError && (
        <View className='order-list-page__footer-error' onClick={onRetry}>
          <Text className='order-list-page__footer-error-text'>
            {loadMoreError}，点击重试
          </Text>
        </View>
      )}
      {loading && (
        <Text className='order-list-page__footer-text'>加载中...</Text>
      )}
      {!loading && !loadMoreError && hasMore && (
        <Text className='order-list-page__footer-text'>上拉加载更多</Text>
      )}
      {!loading && !loadMoreError && !hasMore && (
        <Text className='order-list-page__footer-text'>没有更多了</Text>
      )}
    </View>
  );
}

function EmptyState({ tab }: { tab: OrderTab }) {
  const title = useMemo(() => {
    switch (tab) {
      case 'pending_payment':
        return '暂无待支付订单';
      case 'completed':
        return '暂无已完成订单';
      case 'refunding':
        return '暂无退款中订单';
      case 'cancelled':
        return '暂无已取消订单';
      default:
        return '暂无订单';
    }
  }, [tab]);

  const navigateToPackageList = useCallback(() => {
    void Taro.navigateTo({ url: '/pages/package/list/index' });
  }, []);

  return (
    <View className='order-list-empty'>
      <View className='order-list-empty__illustration'>
        <Icon name='empty' className='order-list-empty__icon' />
      </View>
      <Text className='order-list-empty__title'>{title}</Text>
      <Text className='order-list-empty__desc'>去看看有哪些合适的课程吧</Text>
      <View className='order-list-empty__btn' onClick={navigateToPackageList}>
        <Text className='order-list-empty__btn-text'>去购买套餐</Text>
      </View>
    </View>
  );
}

function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <View className='order-list-error'>
      <View className='order-list-error__illustration'>
        <Icon name='error-circle' className='order-list-error__icon' />
      </View>
      <Text className='order-list-error__text'>{message}</Text>
      <View className='order-list-error__btn' onClick={onRetry}>
        <Text className='order-list-error__btn-text'>重新加载</Text>
      </View>
    </View>
  );
}

function OrderListSkeleton() {
  return (
    <View className='order-list-page__content order-list-page__content--skeleton'>
      <View className='order-card-skeleton' />
      <View className='order-card-skeleton' />
      <View className='order-card-skeleton' />
    </View>
  );
}

function formatCountdown(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return [hours, minutes, seconds]
    .map((v) => String(v).padStart(2, '0'))
    .join(':');
}
