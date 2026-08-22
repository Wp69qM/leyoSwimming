import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { fetchOrderDetail, mockPaymentCallback, payOrder } from '@/api/order';
import { handleBusinessError } from '@/api/request';
import type { OrderDetail } from '@/types/order';

import {
  AmountHeader,
  BottomPaymentBar,
  CancelConfirmModal,
  OrderInfoCard,
  PAGE_PATHS,
  PaymentEmptyState,
  PaymentErrorState,
  PaymentFailureSheet,
  PaymentMethodSection,
  PaymentSkeleton,
  PaymentSuccessOverlay,
  SecurityNotice,
  StatusBarAndNavBar,
} from './components';

import './index.scss';

const CHANNEL_WECHAT = 0;
const SUCCESS_REDIRECT_SECONDS = 3;

export default function PaymentPage() {
  const [orderId, setOrderId] = useState<number | null>(null);
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedChannel, setSelectedChannel] = useState(CHANNEL_WECHAT);
  const [paying, setPaying] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [showFailure, setShowFailure] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [successCountdown, setSuccessCountdown] = useState(
    SUCCESS_REDIRECT_SECONDS
  );
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);

  const params = useMemo(
    () => Taro.getCurrentInstance().router?.params ?? {},
    []
  );

  const isExpired = remainingSeconds !== null && remainingSeconds <= 0;
  const canPay =
    order?.status === 'pending_payment' &&
    !isExpired &&
    !paying &&
    !showFailure;

  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const successTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  function clearCountdownTimer() {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  }

  function clearSuccessTimer() {
    if (successTimerRef.current) {
      clearInterval(successTimerRef.current);
      successTimerRef.current = null;
    }
  }

  const navigateToHome = useCallback(() => {
    void Taro.switchTab({ url: PAGE_PATHS.home });
  }, []);

  const navigateToPackageList = useCallback(() => {
    void Taro.navigateTo({ url: PAGE_PATHS.packageList });
  }, []);

  const navigateToOrderDetail = useCallback(() => {
    if (!orderId) return;
    void Taro.navigateTo({
      url: `${PAGE_PATHS.orderDetail}?orderId=${orderId}`,
    });
  }, [orderId]);

  const navigateAfterSuccess = useCallback(() => {
    // TODO: 体验课/首次正价课跳转 U-预约成功页，加课跳转 U-我的套餐页
    void Taro.switchTab({ url: PAGE_PATHS.home });
  }, []);

  const startSuccessRedirect = useCallback(() => {
    clearSuccessTimer();
    setSuccessCountdown(SUCCESS_REDIRECT_SECONDS);
    successTimerRef.current = setInterval(() => {
      setSuccessCountdown((prev) => {
        if (prev <= 1) {
          clearSuccessTimer();
          navigateAfterSuccess();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, [navigateAfterSuccess]);

  const loadOrder = useCallback(async () => {
    if (!orderId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await fetchOrderDetail(orderId);
      setOrder(data);
      const seconds = Math.floor(
        (new Date(data.expireAt).getTime() - Date.now()) / 1000
      );
      setRemainingSeconds(seconds);
      if (data.status === 'paid') {
        navigateAfterSuccess();
        return;
      }
      if (data.status === 'cancelled') {
        void Taro.showToast({ title: '订单已取消', icon: 'none' });
        navigateToOrderDetail();
        return;
      }
    } catch (err) {
      setError(handleBusinessError(err));
    } finally {
      setLoading(false);
    }
  }, [orderId, navigateAfterSuccess, navigateToOrderDetail]);

  const updateRemainingSeconds = useCallback(() => {
    if (!order) return;
    const expireAt = new Date(order.expireAt).getTime();
    const now = Date.now();
    const diff = Math.floor((expireAt - now) / 1000);
    setRemainingSeconds(diff);
    if (diff <= 0) {
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
    if (isExpired) return;
    updateRemainingSeconds();
    timerRef.current = setInterval(updateRemainingSeconds, 1000);
    return () => {
      clearCountdownTimer();
    };
  }, [order, isExpired, updateRemainingSeconds]);

  useEffect(() => {
    if (!orderId) return;
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (Taro as any).enableAlertBeforeUnload?.({
        message: '是否放弃当前支付？订单将保留 24 小时，可在订单列表中继续支付',
      });
    } catch {
      // ignore unsupported environments
    }
    return () => {
      try {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (Taro as any).disableAlertBeforeUnload?.();
      } catch {
        // ignore
      }
    };
  }, [orderId]);

  useEffect(() => {
    return () => {
      clearCountdownTimer();
      clearSuccessTimer();
    };
  }, []);

  const handleCopyOrderNo = useCallback(() => {
    if (!order) return;
    void Taro.setClipboardData({ data: order.orderNo }).then(() => {
      void Taro.showToast({ title: '订单号已复制', icon: 'none' });
    });
  }, [order]);

  const handlePay = useCallback(async () => {
    if (!order || !canPay) return;
    setPaying(true);
    try {
      const payResult = await payOrder(order.orderId, selectedChannel);
      const callbackResult = await mockPaymentCallback({
        channel: selectedChannel,
        orderId: order.orderId,
        channelTradeNo: payResult.channelTradeNo,
        amount: order.amount,
        success: true,
      });
      if (callbackResult.code === 'SUCCESS') {
        setShowSuccess(true);
        startSuccessRedirect();
      } else {
        setShowFailure(true);
      }
    } catch (err) {
      void Taro.showToast({
        title: handleBusinessError(err),
        icon: 'none',
      });
      setShowFailure(true);
    } finally {
      setPaying(false);
    }
  }, [order, canPay, selectedChannel, startSuccessRedirect]);

  const handleCancel = useCallback(() => {
    setShowCancelModal(true);
  }, []);

  const handleContinuePay = useCallback(() => {
    setShowCancelModal(false);
  }, []);

  const handleConfirmCancelPay = useCallback(() => {
    setShowCancelModal(false);
    navigateToOrderDetail();
  }, [navigateToOrderDetail]);

  const handleRetryPay = useCallback(() => {
    setShowFailure(false);
    void handlePay();
  }, [handlePay]);

  if (!orderId && !loading) {
    return <PaymentEmptyState onHome={navigateToHome} />;
  }

  if (loading) {
    return <PaymentSkeleton />;
  }

  if (error || !order) {
    return (
      <PaymentErrorState
        message={error ?? '订单加载失败'}
        onRetry={loadOrder}
        onHome={navigateToHome}
      />
    );
  }

  const isExpiredPending = isExpired && order.status === 'pending_payment';

  return (
    <View
      className={`payment-page ${
        isExpiredPending ? 'payment-page--expired' : ''
      }`}
    >
      <StatusBarAndNavBar title='支付订单' />

      <View className='payment-page__content'>
        <AmountHeader
          order={order}
          remainingSeconds={remainingSeconds}
          isExpired={isExpiredPending}
          onCopy={handleCopyOrderNo}
        />

        <View className='payment-page__body'>
          <OrderInfoCard
            order={order}
            isExpired={isExpiredPending}
            onClick={() =>
              void Taro.navigateTo({
                url: `${PAGE_PATHS.orderDetail}?orderId=${order.orderId}`,
              })
            }
          />

          {order.status === 'pending_payment' && !isExpiredPending && (
            <>
              <PaymentMethodSection
                selectedChannel={selectedChannel}
                onSelect={setSelectedChannel}
              />
              <SecurityNotice />
            </>
          )}
        </View>
      </View>

      {order.status === 'pending_payment' && !showSuccess && (
        <BottomPaymentBar
          amount={order.amount}
          disabled={!canPay}
          loading={paying}
          expired={isExpiredPending}
          onConfirm={handlePay}
          onCancel={handleCancel}
          onReorder={navigateToPackageList}
        />
      )}

      {showSuccess && (
        <PaymentSuccessOverlay
          totalHours={order.totalHours}
          countdown={successCountdown}
        />
      )}

      {showFailure && !showSuccess && (
        <PaymentFailureSheet
          onRetry={handleRetryPay}
          onBack={navigateToOrderDetail}
        />
      )}

      <CancelConfirmModal
        visible={showCancelModal}
        onContinue={handleContinuePay}
        onCancel={handleConfirmCancelPay}
      />
    </View>
  );
}
