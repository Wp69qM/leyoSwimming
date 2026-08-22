import {
  render,
  screen,
  waitFor,
  fireEvent,
  act,
} from '@testing-library/react';
import Taro from '@tarojs/taro';
import * as orderApi from '@/api/order';
import type { OrderListItem } from '@/types/order';
import OrderListPage from './index';

const reachBottomCallbacks: Array<() => void> = [];

jest.mock('@tarojs/taro', () => ({
  getSystemInfoSync: jest.fn().mockReturnValue({ statusBarHeight: 20 }),
  navigateBack: jest.fn(),
  navigateTo: jest.fn().mockResolvedValue(undefined),
  showToast: jest.fn(),
  showModal: jest.fn(),
  useReachBottom: jest.fn((callback: () => void) => {
    reachBottomCallbacks.push(callback);
  }),
}));

jest.mock('@/api/order', () => ({
  fetchOrderList: jest.fn(),
}));

jest.mock('@/components/common/Icon', () => ({
  Icon: ({ name }: { name: string }) => (
    <span data-testid={`icon-${name}`}>{name}</span>
  ),
}));

const baseItem: OrderListItem = {
  orderId: 100,
  orderNo: 'O-20260816-001',
  status: 'pending_payment',
  amount: '1800.00',
  paidAmount: '0',
  expireAt: new Date(Date.now() + 3600 * 1000).toISOString(),
  packageName: '10 节正价课',
  packageMode: 'standard',
  coachName: '张明远',
  teachingType: 'one_on_one',
  totalHours: 10,
  createdAt: '2026-08-16T10:00:00Z',
};

function mockPage(records: OrderListItem[], current = 1, pages = 1) {
  return {
    records,
    total: records.length,
    size: 10,
    current,
    pages,
  };
}

function mockModalConfirm() {
  (Taro.showModal as jest.Mock).mockResolvedValue({ confirm: true });
}

describe('OrderListPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    reachBottomCallbacks.length = 0;
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('renders loading skeleton initially', () => {
    (orderApi.fetchOrderList as jest.Mock).mockReturnValue(
      new Promise(() => {})
    );
    render(<OrderListPage />);
    expect(document.querySelector('.order-card-skeleton')).toBeInTheDocument();
  });

  test('renders order list with tabs', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([baseItem])
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText(/O-20260816-001/)).toBeInTheDocument();
    });

    expect(screen.getByText('全部')).toBeInTheDocument();
    expect(screen.getAllByText('待支付').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('已完成')).toBeInTheDocument();
    expect(screen.getByText('退款中')).toBeInTheDocument();
    expect(screen.getByText('已取消')).toBeInTheDocument();
    expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    expect(screen.getByText('去支付')).toBeInTheDocument();
    expect(screen.getByText('取消订单')).toBeInTheDocument();
  });

  test('switches tab and reloads list', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(mockPage([]));
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('暂无订单')).toBeInTheDocument();
    });

    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([
        { ...baseItem, status: 'paid' as const, paidAmount: '1800.00' },
      ])
    );

    fireEvent.click(screen.getByText('已完成'));

    await waitFor(() => {
      expect(orderApi.fetchOrderList).toHaveBeenLastCalledWith(
        'completed',
        1,
        10
      );
    });

    await waitFor(() => {
      expect(screen.getByText('查看详情')).toBeInTheDocument();
    });
  });

  test('navigates to payment when 去支付 is clicked', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([baseItem])
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('去支付')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('去支付'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/order/payment/index?orderId=100',
      });
    });
  });

  test('navigates to detail when card is clicked', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([
        { ...baseItem, status: 'paid' as const, paidAmount: '1800.00' },
      ])
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('查看详情')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('10 节正价课'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/order/detail/index?orderId=100',
      });
    });
  });

  test('cancels pending payment order shows placeholder', async () => {
    mockModalConfirm();
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([baseItem])
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('取消订单')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('取消订单'));

    await waitFor(() => {
      expect(Taro.showModal).toHaveBeenCalledWith(
        expect.objectContaining({ title: '确认取消' })
      );
    });

    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '取消功能开发中',
        icon: 'none',
      });
    });
  });

  test('deletes cancelled order shows placeholder', async () => {
    mockModalConfirm();
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([{ ...baseItem, status: 'cancelled' as const }])
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('删除记录')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('删除记录'));

    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '删除功能开发中',
        icon: 'none',
      });
    });
  });

  test('empty state CTA navigates to package list', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(mockPage([]));
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('去购买套餐')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('去购买套餐'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/package/list/index',
      });
    });
  });

  test('shows tab-specific empty title', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(mockPage([]));
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('暂无订单')).toBeInTheDocument();
    });

    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(mockPage([]));
    fireEvent.click(screen.getByText('待支付'));

    await waitFor(() => {
      expect(screen.getByText('暂无待支付订单')).toBeInTheDocument();
    });
  });

  test('shows error state and allows retry', async () => {
    (orderApi.fetchOrderList as jest.Mock)
      .mockRejectedValueOnce(new Error('加载失败'))
      .mockResolvedValueOnce(mockPage([baseItem]));
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('加载失败')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('重新加载'));

    await waitFor(() => {
      expect(screen.getByText(/O-20260816-001/)).toBeInTheDocument();
    });
  });

  test('shows footer when there are more pages', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([baseItem], 1, 2)
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('上拉加载更多')).toBeInTheDocument();
    });
  });

  test('navigates back when back button is clicked', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(mockPage([]));
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('我的订单')).toBeInTheDocument();
    });

    fireEvent.click(document.querySelector('.order-list-page__back')!);
    expect(Taro.navigateBack).toHaveBeenCalled();
  });

  test('triggers load more on reach bottom', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([baseItem], 1, 2)
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText(/O-20260816-001/)).toBeInTheDocument();
    });

    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([{ ...baseItem, orderId: 101, orderNo: 'O-20260816-002' }], 2, 2)
    );

    act(() => {
      reachBottomCallbacks.forEach((callback) => callback());
    });

    await waitFor(() => {
      expect(screen.getByText(/O-20260816-002/)).toBeInTheDocument();
    });

    expect(orderApi.fetchOrderList).toHaveBeenLastCalledWith('all', 2, 10);
  });

  test('shows load more error and allows retry', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([baseItem], 1, 2)
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText(/O-20260816-001/)).toBeInTheDocument();
    });

    (orderApi.fetchOrderList as jest.Mock).mockRejectedValue(
      new Error('加载失败')
    );

    act(() => {
      reachBottomCallbacks.forEach((callback) => callback());
    });

    await waitFor(() => {
      expect(screen.getByText('加载失败，点击重试')).toBeInTheDocument();
    });

    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([{ ...baseItem, orderId: 101, orderNo: 'O-20260816-002' }], 2, 2)
    );

    fireEvent.click(screen.getByText('加载失败，点击重试'));

    await waitFor(() => {
      expect(screen.getByText(/O-20260816-002/)).toBeInTheDocument();
    });
  });

  test('shows countdown for pending payment order', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([baseItem])
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText(/剩 \d{2}:\d{2}:\d{2}/)).toBeInTheDocument();
    });

    act(() => {
      jest.advanceTimersByTime(1000);
    });

    await waitFor(() => {
      expect(screen.getByText(/剩 \d{2}:\d{2}:\d{2}/)).toBeInTheDocument();
    });
  });

  test('refreshes order list when pending payment countdown expires', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([
        {
          ...baseItem,
          expireAt: new Date(Date.now() + 500).toISOString(),
        },
      ])
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText(/剩 \d{2}:\d{2}:\d{2}/)).toBeInTheDocument();
    });

    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([{ ...baseItem, orderId: 101, orderNo: 'O-20260816-002' }])
    );

    act(() => {
      jest.advanceTimersByTime(1000);
    });

    await waitFor(() => {
      expect(screen.getByText(/O-20260816-002/)).toBeInTheDocument();
    });
  });

  test('shows refunded order action buttons', async () => {
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([{ ...baseItem, status: 'refunded', paidAmount: '1800.00' }])
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('查看详情')).toBeInTheDocument();
    });

    expect(screen.getByText('删除记录')).toBeInTheDocument();
  });

  test('shows refund pending order action buttons', async () => {
    mockModalConfirm();
    (orderApi.fetchOrderList as jest.Mock).mockResolvedValue(
      mockPage([{ ...baseItem, status: 'refund_pending' }])
    );
    render(<OrderListPage />);

    await waitFor(() => {
      expect(screen.getByText('查看进度')).toBeInTheDocument();
    });

    expect(screen.getByText('取消退款')).toBeInTheDocument();

    fireEvent.click(screen.getByText('取消退款'));

    await waitFor(() => {
      expect(Taro.showModal).toHaveBeenCalledWith(
        expect.objectContaining({ title: '确认取消退款' })
      );
    });
  });
});
