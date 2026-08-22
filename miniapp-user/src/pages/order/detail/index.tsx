import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { fetchOrderDetail } from '@/api/order';
import { handleBusinessError } from '@/api/request';
import type { OrderDetail } from '@/types/order';

import {
  BottomActionBar,
  OrderDetailEmptyState,
  OrderDetailErrorState,
  OrderDetailSkeleton,
  OrderInfoCard,
  PackageUsageCard,
  PAGE_PATHS,
  PurchaseSnapshotCard,
  RefundAmountInfo,
  RefundProgressCard,
  StatusBarAndNavBar,
  StatusHeader,
  computeRemainingSeconds,
} from './components';

import './index.scss';

const REFUND_STATUSES = [
  'refund_pending',
  'refund_processing',
  'dispute_processing',
  'refunded',
  'rejected',
];

const REFUND_AMOUNT_VISIBLE_STATUSES = [
  'paid',
  'refund_pending',
  'refund_processing',
  'dispute_processing',
  'refunded',
  'rejected',
];

export default function OrderDetailPage() {
  const [orderId, setOrderId] = useState<number | null>(null);
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);

  const params = useMemo(
    () => Taro.getCurrentInstance().router?.params ?? {},
    []
  );

  const refundProgressRef = useRef<HTMLDivElement | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  function clearCountdownTimer() {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  }

  const navigateBack = useCallback(() => {
    void Taro.navigateBack({ delta: 1 });
  }, []);

  const navigateToOrderList = useCallback(() => {
    void Taro.navigateTo({ url: PAGE_PATHS.orderList });
  }, []);

  const navigateToPayment = useCallback(() => {
    if (!orderId) return;
    void Taro.navigateTo({
      url: `${PAGE_PATHS.orderPayment}?orderId=${orderId}`,
    });
  }, [orderId]);

  const navigateToPackageDetail = useCallback(() => {
    if (!order?.packageId) {
      void Taro.showToast({ title: '暂无套餐信息', icon: 'none' });
      return;
    }
    void Taro.navigateTo({
      url: `${PAGE_PATHS.packageDetail}?packageId=${order.packageId}`,
    });
  }, [order]);

  const loadOrder = useCallback(async () => {
    if (!orderId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await fetchOrderDetail(orderId);
      setOrder(data);
      if (data.status === 'pending_payment') {
        setRemainingSeconds(computeRemainingSeconds(data.expireAt));
      } else {
        setRemainingSeconds(null);
      }
    } catch (err) {
      setError(handleBusinessError(err));
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  const updateRemainingSeconds = useCallback(() => {
    if (!order) return;
    const next = computeRemainingSeconds(order.expireAt);
    setRemainingSeconds(next);
    if (next <= 0) {
      clearCountdownTimer();
      void loadOrder();
    }
  }, [order, loadOrder]);

  useEffect(() => {
    const id = Number(params.orderId);
    if (!Number.isNaN(id) && id > 0) {
      setOrderId(id);
    } else {
      setLoading(false);
    }
  }, [params]);

  useEffect(() => {
    void loadOrder();
  }, [loadOrder]);

  useEffect(() => {
    if (!order || order.status !== 'pending_payment') return;
    if (remainingSeconds !== null && remainingSeconds <= 0) return;
    updateRemainingSeconds();
    timerRef.current = setInterval(updateRemainingSeconds, 1000);
    return () => {
      clearCountdownTimer();
    };
  }, [order, remainingSeconds, updateRemainingSeconds]);

  useEffect(() => {
    return () => {
      clearCountdownTimer();
    };
  }, []);

  // TODO: 后端未提供删除/取消订单 API，当前仅做 UI 提示
  const handleDelete = useCallback(() => {
    void Taro.showModal({
      title: '确认删除',
      content: '删除后订单记录将无法恢复',
      confirmText: '删除',
      confirmColor: '#ff4d4f',
    }).then((res) => {
      if (res.confirm) {
        void Taro.showToast({
          title: '删除功能开发中',
          icon: 'none',
        });
      }
    });
  }, []);

  const handleViewProgress = useCallback(() => {
    refundProgressRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  const handleContactService = useCallback(() => {
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const openChat = (Taro as any).openCustomerServiceChat;
      if (typeof openChat === 'function') {
        openChat({});
        return;
      }
    } catch {
      // ignore unsupported environments
    }
    void Taro.showToast({ title: '客服功能开发中', icon: 'none' });
  }, []);

  if (!orderId && !loading) {
    return <OrderDetailEmptyState onBack={navigateToOrderList} />;
  }

  if (loading) {
    return <OrderDetailSkeleton />;
  }

  if (error || !order) {
    return (
      <OrderDetailErrorState
        message={error ?? '订单加载失败'}
        onRetry={loadOrder}
        onBack={navigateToOrderList}
      />
    );
  }

  const showRefundProgress = REFUND_STATUSES.includes(order.status);
  const showRefundAmountInfo =
    REFUND_AMOUNT_VISIBLE_STATUSES.includes(order.status) ||
    order.packageStatus === 'expired';

  return (
    <View className='order-detail-page'>
      <StatusBarAndNavBar title='订单详情' onBack={navigateBack} />

      <View className='order-detail-page__content'>
        <StatusHeader order={order} remainingSeconds={remainingSeconds} />

        <View className='order-detail-page__body'>
          <PurchaseSnapshotCard order={order} />
          <PackageUsageCard order={order} />
          <OrderInfoCard order={order} />

          {showRefundProgress && (
            <View ref={refundProgressRef}>
              <RefundProgressCard status={order.status} />
            </View>
          )}

          {showRefundAmountInfo && <RefundAmountInfo order={order} />}
        </View>
      </View>

      <BottomActionBar
        order={order}
        onPay={navigateToPayment}
        onViewPackage={navigateToPackageDetail}
        onDelete={handleDelete}
        onViewProgress={handleViewProgress}
        onContactService={handleContactService}
      />
    </View>
  );
}
