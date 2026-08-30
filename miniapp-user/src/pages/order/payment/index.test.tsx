import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import Taro from '@tarojs/taro';
import * as orderApi from '@/api/order';
import PaymentPage from './index';

jest.mock('@tarojs/taro', () => ({
  getSystemInfoSync: jest.fn().mockReturnValue({ statusBarHeight: 20 }),
  getCurrentInstance: jest.fn().mockReturnValue({
    router: { params: { orderId: '100' } },
  }),
  navigateTo: jest.fn().mockResolvedValue(undefined),
  switchTab: jest.fn(),
  showToast: jest.fn(),
  setClipboardData: jest.fn().mockResolvedValue(undefined),
  enableAlertBeforeUnload: jest.fn(),
  disableAlertBeforeUnload: jest.fn(),
}));

jest.mock('@/api/order', () => ({
  fetchOrderDetail: jest.fn(),
  payOrder: jest.fn(),
  mockPaymentCallback: jest.fn(),
}));

jest.mock('@/components/common/Icon', () => ({
  Icon: ({ name }: { name: string }) => (
    <span data-testid={`icon-${name}`}>{name}</span>
  ),
}));

const mockPendingOrder = {
  orderId: 100,
  orderNo: 'O-20260816-001',
  type: 'purchase' as const,
  status: 'pending_payment' as const,
  amount: '1800.00',
  originalPrice: '2000.00',
  paidAmount: '0',
  expireAt: new Date(Date.now() + 3600 * 1000).toISOString(),
  packageName: '10 节正价课',
  packageMode: 'standard' as const,
  coachName: '张明远',
  teachingType: 'one_on_one',
  totalHours: 10,
  durationMinutes: 60,
  validDays: 90,
  createdAt: '2026-08-16T10:00:00Z',
};

const mockExperienceOrder = {
  ...mockPendingOrder,
  packageName: '新人体验课',
  packageMode: 'experience' as const,
  amount: '99.00',
  totalHours: 1,
};

const mockPaidOrder = {
  ...mockPendingOrder,
  status: 'paid' as const,
  paidAmount: '1800.00',
  paidAt: '2026-08-16T10:05:00Z',
};

const mockCancelledOrder = {
  ...mockPendingOrder,
  status: 'cancelled' as const,
};

function setupRouter(orderId?: string) {
  (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
    router: { params: orderId ? { orderId } : {} },
  });
}

describe('PaymentPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    setupRouter('100');
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('renders loading skeleton initially', () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockReturnValue(
      new Promise(() => {})
    );
    render(<PaymentPage />);
    expect(document.querySelector('.payment-skeleton')).toBeInTheDocument();
  });

  test('renders empty state for invalid order id', async () => {
    setupRouter('invalid');
    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('暂无订单信息')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('返回首页'));
    expect(Taro.switchTab).toHaveBeenCalledWith({ url: '/pages/index/index' });
  });

  test('renders pending order detail', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(
      mockPendingOrder
    );
    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    expect(screen.getByText('O-20260816-001')).toBeInTheDocument();
    expect(
      document.querySelector('.payment-amount-header__amount')
    ).toHaveTextContent('1,800');
    expect(screen.getByText('微信支付')).toBeInTheDocument();
    expect(screen.getByText('确认支付')).toBeInTheDocument();
  });

  test('renders experience package tag', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(
      mockExperienceOrder
    );
    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('体验课')).toBeInTheDocument();
    });
  });

  test('copies order number', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(
      mockPendingOrder
    );
    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('复制')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('复制'));
    await waitFor(() => {
      expect(Taro.setClipboardData).toHaveBeenCalledWith({
        data: 'O-20260816-001',
      });
    });
  });

  test('disables pay button when countdown expires', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue({
      ...mockPendingOrder,
      expireAt: new Date(Date.now() - 1000).toISOString(),
    });
    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('重新下单')).toBeInTheDocument();
    });

    expect(screen.queryByText('确认支付')).not.toBeInTheDocument();
  });

  test('navigates to package list when reorder button is clicked', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue({
      ...mockPendingOrder,
      expireAt: new Date(Date.now() - 1000).toISOString(),
    });
    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('重新下单')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('重新下单'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/package/list/index',
      });
    });
  });

  test('redirects to order detail when order is cancelled', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(
      mockCancelledOrder
    );
    render(<PaymentPage />);

    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '订单已取消',
        icon: 'none',
      });
    });

    expect(Taro.navigateTo).toHaveBeenCalledWith({
      url: '/pages/order/detail/index?orderId=100',
    });
  });

  test('redirects after success when order is already paid', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(mockPaidOrder);
    render(<PaymentPage />);

    await waitFor(() => {
      expect(Taro.switchTab).toHaveBeenCalled();
    });
  });

  test('completes payment flow on confirm', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(
      mockPendingOrder
    );
    (orderApi.payOrder as jest.Mock).mockResolvedValue({
      paymentId: 1,
      channelTradeNo: 'MOCK-WECHAT-001',
      status: 'pending',
    });
    (orderApi.mockPaymentCallback as jest.Mock).mockResolvedValue({
      code: 'SUCCESS',
    });

    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('确认支付')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('确认支付'));

    await waitFor(() => {
      expect(orderApi.payOrder).toHaveBeenCalledWith(100, 0);
    });

    await waitFor(() => {
      expect(orderApi.mockPaymentCallback).toHaveBeenCalledWith({
        channel: 0,
        orderId: 100,
        channelTradeNo: 'MOCK-WECHAT-001',
        amount: '1800.00',
        success: true,
      });
    });

    await waitFor(() => {
      expect(screen.getByText('支付成功')).toBeInTheDocument();
    });
  });

  test('shows failure sheet on callback failure', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(
      mockPendingOrder
    );
    (orderApi.payOrder as jest.Mock).mockResolvedValue({
      paymentId: 1,
      channelTradeNo: 'MOCK-WECHAT-001',
      status: 'pending',
    });
    (orderApi.mockPaymentCallback as jest.Mock).mockResolvedValue({
      code: 'FAIL',
    });

    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('确认支付')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('确认支付'));

    await waitFor(() => {
      expect(screen.getByText('支付未完成')).toBeInTheDocument();
    });
  });

  test('shows failure sheet on payment error', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(
      mockPendingOrder
    );
    (orderApi.payOrder as jest.Mock).mockRejectedValue(
      new Error('支付服务繁忙')
    );

    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('确认支付')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('确认支付'));

    await waitFor(() => {
      expect(screen.getByText('支付未完成')).toBeInTheDocument();
    });
  });

  test('shows custom cancel modal and can continue payment', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(
      mockPendingOrder
    );
    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('取消支付')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('取消支付'));

    await waitFor(() => {
      expect(screen.getByText('是否放弃当前支付？')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('继续支付'));
    expect(screen.queryByText('是否放弃当前支付？')).not.toBeInTheDocument();
    expect(screen.getByText('确认支付')).toBeInTheDocument();
  });

  test('confirms cancel payment and navigates to order detail', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(
      mockPendingOrder
    );
    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('取消支付')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('取消支付'));

    await waitFor(() => {
      expect(screen.getByText('是否放弃当前支付？')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('确认取消'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/order/detail/index?orderId=100',
      });
    });
  });

  test('shows error state and allows retry', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockRejectedValueOnce(
      new Error('加载失败')
    );
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValueOnce(
      mockPendingOrder
    );
    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('加载失败')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('重新加载'));

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });
  });

  test('switches payment channel to alipay', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(
      mockPendingOrder
    );
    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.getByText('支付宝支付')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('支付宝支付'));

    (orderApi.payOrder as jest.Mock).mockResolvedValue({
      paymentId: 2,
      channelTradeNo: 'MOCK-ALIPAY-001',
      status: 'pending',
    });
    (orderApi.mockPaymentCallback as jest.Mock).mockResolvedValue({
      code: 'SUCCESS',
    });

    fireEvent.click(screen.getByText('确认支付'));

    await waitFor(() => {
      expect(orderApi.payOrder).toHaveBeenCalledWith(100, 1);
    });
  });
});
