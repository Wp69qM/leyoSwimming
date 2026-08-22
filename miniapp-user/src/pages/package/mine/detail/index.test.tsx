import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import Taro from '@tarojs/taro';
import * as packageApi from '@/api/package';
import MyPackageDetailPage from './index';

const mockPackageId = 123;

const mockActivePackage = {
  packageId: mockPackageId,
  packageName: '10 节正价课',
  packageMode: 'standard' as const,
  coachName: '张明远',
  teachingType: '一对一',
  strokeIds: [1, 2],
  durationMinutes: 60,
  validDays: 90,
  totalHours: 10,
  consumedCount: 4,
  availableCount: 6,
  paidAmount: '1800',
  originalPrice: '2000',
  refundEnabled: true,
  refundValidDays: 7,
  refundRatio: '0.8',
  refundRuleText: '开课后 7 天内可申请退款，退款比例 80%',
  status: 'active' as const,
  frozenReason: null,
  expireAt: '2026-10-30T23:59:59',
  createdAt: '2026-08-02T14:30:00',
  canRefund: true,
};

const mockExpiredPackage = {
  ...mockActivePackage,
  status: 'expired' as const,
  canRefund: true,
};

const mockFrozenPackage = {
  ...mockActivePackage,
  status: 'frozen' as const,
  frozenReason: 'coach_resigned',
  canRefund: true,
  refundRuleText: '教练离职，可申请 100% 全额退款',
};

const mockRefundPendingPackage = {
  ...mockActivePackage,
  status: 'frozen' as const,
  frozenReason: 'refund_pending',
  canRefund: false,
};

jest.mock('@tarojs/taro', () => ({
  getSystemInfoSync: jest.fn().mockReturnValue({ statusBarHeight: 20 }),
  getCurrentInstance: jest.fn().mockReturnValue({
    router: { params: { packageId: '123' } },
  }),
  navigateBack: jest.fn(),
  navigateTo: jest.fn().mockResolvedValue(undefined),
  redirectTo: jest.fn().mockResolvedValue(undefined),
  showToast: jest.fn(),
}));

jest.mock('@/api/package', () => ({
  fetchMyPackageDetail: jest.fn(),
}));

jest.mock('@/components/common/Icon', () => ({
  Icon: ({ name }: { name: string }) => (
    <span data-testid={`icon-${name}`}>{name}</span>
  ),
}));

describe('MyPackageDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: { packageId: '123' } },
    });
  });

  test('renders loading skeleton initially', () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockReturnValue(
      new Promise(() => {})
    );
    render(<MyPackageDetailPage />);
    expect(
      document.querySelector('.my-package-detail--skeleton')
    ).toBeInTheDocument();
  });

  test('shows error when packageId is missing', async () => {
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: {} },
    });
    render(<MyPackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('套餐参数缺失')).toBeInTheDocument();
    });
  });

  test('shows error when packageId is invalid', async () => {
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: { packageId: 'abc' } },
    });
    render(<MyPackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('套餐参数缺失')).toBeInTheDocument();
    });
  });

  test('renders package info and usage card', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockActivePackage
    );
    render(<MyPackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    expect(screen.getByText('正价套餐')).toBeInTheDocument();
    expect(screen.getByText('可用')).toBeInTheDocument();
    expect(screen.getByText('张明远')).toBeInTheDocument();
    expect(screen.getByText('自由泳 / 蛙泳')).toBeInTheDocument();
    expect(screen.getByText('使用进度')).toBeInTheDocument();
    expect(screen.getByText('10')).toBeInTheDocument();
    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('6')).toBeInTheDocument();
    expect(screen.getByText('¥1,800.00')).toBeInTheDocument();
  });

  test('shows refund entry when refundable', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockActivePackage
    );
    render(<MyPackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('refund-entry')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('refund-entry-button'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: `/pages/refund/apply/index?packageId=${mockPackageId}`,
      });
    });
  });

  test('hides refund entry when not refundable', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockRefundPendingPackage
    );
    render(<MyPackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    expect(screen.queryByTestId('refund-entry')).not.toBeInTheDocument();
  });

  test('shows expired notice for expired package', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockExpiredPackage
    );
    render(<MyPackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('已过期')).toBeInTheDocument();
    });

    expect(screen.getByText('套餐已过期，无法再预约课程')).toBeInTheDocument();
  });

  test('shows coach resigned tip for frozen coach_resigned package', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockFrozenPackage
    );
    render(<MyPackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('已冻结')).toBeInTheDocument();
    });

    expect(
      screen.getByText('教练已离职，本套餐可申请 100% 全额退款')
    ).toBeInTheDocument();
  });

  test('shows error state and navigates back to my packages', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockRejectedValue({
      message: '网络错误',
    });
    render(<MyPackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('网络错误')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('返回我的套餐'));

    await waitFor(() => {
      expect(Taro.redirectTo).toHaveBeenCalledWith({
        url: '/pages/package/mine/index',
      });
    });
  });

  test('navigates back when back button clicked', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockActivePackage
    );
    render(<MyPackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('套餐详情')).toBeInTheDocument();
    });

    const backButton = document.querySelector('.my-package-detail__back');
    expect(backButton).toBeInTheDocument();
    fireEvent.click(backButton as Element);

    await waitFor(() => {
      expect(Taro.navigateBack).toHaveBeenCalled();
    });
  });
});
