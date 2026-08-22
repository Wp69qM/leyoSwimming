import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import Taro from '@tarojs/taro';
import * as orderApi from '@/api/order';
import OrderDetailPage from './index';

jest.mock('@tarojs/taro', () => ({
  getSystemInfoSync: jest.fn().mockReturnValue({ statusBarHeight: 20 }),
  getCurrentInstance: jest.fn().mockReturnValue({
    router: { params: { orderId: '100' } },
  }),
  navigateBack: jest.fn(),
  navigateTo: jest.fn().mockResolvedValue(undefined),
  switchTab: jest.fn(),
  showToast: jest.fn(),
  showModal: jest.fn(),
}));

jest.mock('@/api/order', () => ({
  fetchOrderDetail: jest.fn(),
}));

jest.mock('@/components/common/Icon', () => ({
  Icon: ({ name }: { name: string }) => (
    <span data-testid={`icon-${name}`}>{name}</span>
  ),
}));

const baseOrder = {
  orderId: 100,
  orderNo: 'O-20260816-001',
  type: 'purchase' as const,
  status: 'pending_payment' as const,
  amount: '1800.00',
  paidAmount: '0',
  expireAt: new Date(Date.now() + 3600 * 1000).toISOString(),
  packageId: 200,
  packageName: '10 节正价课',
  packageMode: 'standard' as const,
  originalPrice: '2000.00',
  coachName: '张明远',
  teachingType: 'one_on_one',
  totalHours: 10,
  durationMinutes: 60,
  validDays: 90,
  createdAt: '2026-08-16T10:00:00Z',
};

function setupRouter(orderId?: string) {
  (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
    router: { params: orderId ? { orderId } : {} },
  });
}

function mockModalConfirm() {
  (Taro.showModal as jest.Mock).mockResolvedValue({ confirm: true });
}

describe('OrderDetailPage', () => {
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
    render(<OrderDetailPage />);
    expect(
      document.querySelector('.order-detail-skeleton')
    ).toBeInTheDocument();
  });

  test('renders empty state for invalid order id', async () => {
    setupRouter('invalid');
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('暂无订单信息')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('返回订单列表'));
    expect(Taro.navigateTo).toHaveBeenCalledWith({
      url: '/pages/order/list/index',
    });
  });

  test('renders pending payment order and navigates to payment', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(baseOrder);
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('待支付')).toBeInTheDocument();
    });

    expect(screen.getAllByText('10 节正价课').length).toBeGreaterThan(0);
    expect(screen.getByText('去支付')).toBeInTheDocument();
    expect(screen.getByText('取消订单')).toBeInTheDocument();

    fireEvent.click(screen.getByText('去支付'));
    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/order/payment/index?orderId=100',
      });
    });
  });

  test('cancels pending payment order shows placeholder toast', async () => {
    mockModalConfirm();
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(baseOrder);
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('取消订单')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('取消订单'));

    await waitFor(() => {
      expect(Taro.showModal).toHaveBeenCalledWith({
        title: '确认删除',
        content: '删除后订单记录将无法恢复',
        confirmText: '删除',
        confirmColor: '#ff4d4f',
      });
    });

    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '删除功能开发中',
        icon: 'none',
      });
    });
  });

  test('renders paid order and navigates to package detail', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue({
      ...baseOrder,
      status: 'paid' as const,
      paidAmount: '1800.00',
      paidAt: '2026-08-16T10:05:00Z',
      channel: 'wechat' as const,
      packageStatus: 'active' as const,
      availableHours: 10,
      usedHours: 0,
      packageExpireAt: '2026-11-14T10:05:00Z',
    });
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('已支付')).toBeInTheDocument();
    });

    expect(screen.getByText('查看套餐')).toBeInTheDocument();
    expect(screen.getByText('微信支付')).toBeInTheDocument();
    expect(screen.getByText('可用')).toBeInTheDocument();

    fireEvent.click(screen.getByText('查看套餐'));
    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/my-package/detail/index?packageId=200',
      });
    });
  });

  test('renders cancelled order and shows delete placeholder', async () => {
    mockModalConfirm();
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue({
      ...baseOrder,
      status: 'cancelled' as const,
    });
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('已取消')).toBeInTheDocument();
    });

    const deleteButton = screen.getByText('删除记录');
    expect(deleteButton).toBeInTheDocument();

    fireEvent.click(deleteButton);
    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '删除功能开发中',
        icon: 'none',
      });
    });
  });

  test('renders refund pending order with progress and contact service', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue({
      ...baseOrder,
      status: 'refund_pending' as const,
      refundAmount: '1800.00',
      refundReason: '个人原因',
      packageStatus: 'frozen' as const,
      availableHours: 10,
      usedHours: 0,
    });
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('退款审批中')).toBeInTheDocument();
    });

    expect(screen.getByText('退款进度')).toBeInTheDocument();
    expect(screen.getByText('查看进度')).toBeInTheDocument();
    expect(screen.getByText('联系客服')).toBeInTheDocument();

    fireEvent.click(screen.getByText('联系客服'));
    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '客服功能开发中',
        icon: 'none',
      });
    });
  });

  test('renders refunded order with refund info', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue({
      ...baseOrder,
      status: 'refunded' as const,
      paidAmount: '1800.00',
      paidAt: '2026-08-16T10:05:00Z',
      channel: 'wechat' as const,
      refundAmount: '1800.00',
      refundReason: '个人原因',
      refundPaidAt: '2026-08-17T10:00:00Z',
      packageStatus: 'refunded' as const,
    });
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('已退款')).toBeInTheDocument();
    });

    expect(screen.getByText('删除记录')).toBeInTheDocument();
    expect(screen.getByText('退款金额')).toBeInTheDocument();
    expect(
      screen.getAllByText((content) => content.includes('1,800')).length
    ).toBeGreaterThanOrEqual(2);
    expect(screen.getByText('个人原因')).toBeInTheDocument();
  });

  test('renders rejected order with contact service and view package', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue({
      ...baseOrder,
      status: 'rejected' as const,
      paidAmount: '1800.00',
      paidAt: '2026-08-16T10:05:00Z',
      channel: 'alipay' as const,
      refundAmount: '0',
      refundReason: '个人原因',
      rejectedReason: '已使用课时数超过限制',
      packageStatus: 'active' as const,
    });
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('退款未通过')).toBeInTheDocument();
    });

    expect(screen.getByText('联系客服')).toBeInTheDocument();
    expect(screen.getByText('查看套餐')).toBeInTheDocument();
    expect(screen.getByText(/已使用课时数超过限制/)).toBeInTheDocument();

    fireEvent.click(screen.getByText('联系客服'));
    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '客服功能开发中',
        icon: 'none',
      });
    });
  });

  test('renders expired package notice and view package', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue({
      ...baseOrder,
      status: 'paid' as const,
      paidAmount: '1800.00',
      paidAt: '2026-08-16T10:05:00Z',
      channel: 'wechat' as const,
      packageStatus: 'expired' as const,
      availableHours: 0,
      usedHours: 10,
      packageExpireAt: '2026-08-15T10:05:00Z',
    });
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('已过期')).toBeInTheDocument();
    });

    expect(
      screen.getByText('套餐已过期，退款申请请前往套餐详情页')
    ).toBeInTheDocument();
    expect(screen.getByText('查看套餐')).toBeInTheDocument();
  });

  test('shows refund amount calculation for paid order', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue({
      ...baseOrder,
      status: 'paid' as const,
      paidAmount: '1800.00',
      paidAt: '2026-08-16T10:05:00Z',
      channel: 'wechat' as const,
      packageStatus: 'active' as const,
      availableHours: 8,
      reservedHours: 2,
      usedHours: 0,
      refundRatio: 1,
      refundEnabled: true,
      refundValidDays: 30,
    });
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('退款金额计算')).toBeInTheDocument();
    });

    expect(
      screen.getByText(
        /退款金额 = 套餐价 ×（剩余课时 \+ 已预约课时）\/ 总课时 × 退款比例/
      )
    ).toBeInTheDocument();
  });

  test('shows error state and allows retry', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockRejectedValueOnce(
      new Error('订单不存在或无权查看')
    );
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValueOnce(baseOrder);
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('订单不存在或无权查看')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('重新加载'));

    await waitFor(() => {
      expect(screen.getByText('待支付')).toBeInTheDocument();
    });
  });

  test('navigates back when back button is clicked', async () => {
    (orderApi.fetchOrderDetail as jest.Mock).mockResolvedValue(baseOrder);
    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('订单详情')).toBeInTheDocument();
    });

    fireEvent.click(document.querySelector('.order-detail-page__back')!);
    expect(Taro.navigateBack).toHaveBeenCalledWith({ delta: 1 });
  });
});
