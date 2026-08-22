import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import Taro from '@tarojs/taro';
import * as packageApi from '@/api/package';
import MyPackagePage from './index';

jest.mock('@tarojs/taro', () => ({
  getSystemInfoSync: jest.fn().mockReturnValue({ statusBarHeight: 20 }),
  navigateBack: jest.fn(),
  navigateTo: jest.fn().mockResolvedValue(undefined),
  showToast: jest.fn(),
}));

jest.mock('@/api/package', () => ({
  fetchMyPackageList: jest.fn(),
}));

jest.mock('@/components/common/Icon', () => ({
  Icon: ({ name }: { name: string }) => (
    <span data-testid={`icon-${name}`}>{name}</span>
  ),
}));

const mockActivePackage = {
  packageId: 1,
  packageName: '10 节正价课',
  packageMode: 'standard' as const,
  coachName: '张明远',
  teachingType: '一对一',
  durationMinutes: 60,
  validDays: 90,
  totalHours: 10,
  consumedCount: 4,
  availableCount: 6,
  paidAmount: '1800',
  originalPrice: '2000',
  status: 'active' as const,
  frozenReason: null,
  refundEnabled: true,
  refundValidDays: 7,
  expireAt: '2026-10-30T23:59:59',
  createdAt: '2026-07-30T10:00:00',
  canRefund: true,
};

const mockExperiencePackage = {
  packageId: 2,
  packageName: '新人体验课',
  packageMode: 'experience' as const,
  coachName: '张明远',
  teachingType: '一对一',
  durationMinutes: 60,
  validDays: 30,
  totalHours: 1,
  consumedCount: 0,
  availableCount: 1,
  paidAmount: '99',
  originalPrice: '129',
  status: 'active' as const,
  frozenReason: null,
  refundEnabled: false,
  refundValidDays: 0,
  expireAt: '2026-09-01T23:59:59',
  createdAt: '2026-08-01T10:00:00',
  canRefund: false,
};

const mockExhaustedPackage = {
  packageId: 3,
  packageName: '10 节正价课',
  packageMode: 'standard' as const,
  coachName: '张明远',
  teachingType: '一对一',
  durationMinutes: 60,
  validDays: 90,
  totalHours: 10,
  consumedCount: 10,
  availableCount: 0,
  paidAmount: '1800',
  originalPrice: '2000',
  status: 'exhausted' as const,
  frozenReason: null,
  refundEnabled: true,
  refundValidDays: 7,
  expireAt: '2026-10-30T23:59:59',
  createdAt: '2026-07-30T10:00:00',
  canRefund: false,
};

const mockFrozenPackage = {
  packageId: 4,
  packageName: '10 节正价课',
  packageMode: 'standard' as const,
  coachName: '张明远',
  teachingType: '一对一',
  durationMinutes: 60,
  validDays: 90,
  totalHours: 10,
  consumedCount: 3,
  availableCount: 7,
  paidAmount: '1800',
  originalPrice: '2000',
  status: 'frozen' as const,
  frozenReason: 'coach_resigned',
  refundEnabled: true,
  refundValidDays: 7,
  expireAt: '2026-11-01T23:59:59',
  createdAt: '2026-07-30T10:00:00',
  canRefund: true,
};

describe('MyPackagePage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders loading skeleton initially', () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockReturnValue(
      new Promise(() => {})
    );
    render(<MyPackagePage />);
    expect(document.querySelector('.my-package--skeleton')).toBeInTheDocument();
  });

  test('renders empty state when no packages', async () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockResolvedValue([]);
    render(<MyPackagePage />);

    await waitFor(() => {
      expect(screen.getByText('您还没有套餐，去选购吧')).toBeInTheDocument();
    });

    expect(
      screen.getByText('选购心仪教练的课程套餐，开启游泳之旅')
    ).toBeInTheDocument();
    expect(screen.getByText('去购买套餐')).toBeInTheDocument();
  });

  test('navigates to package list from empty state', async () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockResolvedValue([]);
    render(<MyPackagePage />);

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

  test('renders active packages and summary', async () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockResolvedValue([
      mockActivePackage,
      mockExperiencePackage,
    ]);
    render(<MyPackagePage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    expect(screen.getByTestId('summary-total')).toHaveTextContent('11');
    expect(screen.getByTestId('summary-used')).toHaveTextContent('4');
    expect(screen.getByTestId('summary-remaining')).toHaveTextContent('7');
    expect(screen.getAllByText('可用')).toHaveLength(3);
    expect(screen.getByText('正价套餐')).toBeInTheDocument();
    expect(screen.getByText('体验课')).toBeInTheDocument();
  });

  test('groups packages by status and only active expanded by default', async () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockResolvedValue([
      mockActivePackage,
      mockExhaustedPackage,
      mockFrozenPackage,
    ]);
    render(<MyPackagePage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    expect(screen.getByText('已耗尽')).toBeInTheDocument();
    expect(screen.getByText('已冻结')).toBeInTheDocument();
    expect(screen.getAllByText('可用')).toHaveLength(2);
    expect(screen.getAllByTestId('refund-button')).toHaveLength(1);

    const frozenHeader = screen
      .getByText('已冻结')
      .closest('.package-group__header');
    fireEvent.click(frozenHeader as Element);

    await waitFor(() => {
      expect(screen.getAllByTestId('refund-button')).toHaveLength(2);
    });
  });

  test('shows coach resigned notice for frozen group', async () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockResolvedValue([
      mockFrozenPackage,
    ]);
    render(<MyPackagePage />);

    await waitFor(() => {
      expect(screen.getByText('已冻结')).toBeInTheDocument();
    });

    const frozenHeader = screen
      .getByText('已冻结')
      .closest('.package-group__header');
    fireEvent.click(frozenHeader as Element);

    await waitFor(() => {
      expect(
        screen.getByText('教练已离职，请更换教练或申请退款')
      ).toBeInTheDocument();
    });
  });

  test('toggles group expansion', async () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockResolvedValue([
      mockActivePackage,
      mockExhaustedPackage,
    ]);
    render(<MyPackagePage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    const exhaustedHeader = screen
      .getByText('已耗尽')
      .closest('.package-group__header');
    expect(exhaustedHeader).toBeInTheDocument();
    fireEvent.click(exhaustedHeader as Element);

    await waitFor(() => {
      expect(screen.getAllByText('10 节正价课')).toHaveLength(2);
    });
  });

  test('shows refund button only for refundable packages', async () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockResolvedValue([
      mockActivePackage,
      mockExperiencePackage,
    ]);
    render(<MyPackagePage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    expect(screen.getAllByText('申请退款')).toHaveLength(1);
  });

  test('navigates to refund apply when refund button is clicked', async () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockResolvedValue([
      mockActivePackage,
    ]);
    render(<MyPackagePage />);

    await waitFor(() => {
      expect(screen.getByText('申请退款')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('申请退款'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/refund/apply/index?packageId=1',
      });
    });
  });

  test('navigates to package detail when card is clicked', async () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockResolvedValue([
      mockActivePackage,
    ]);
    render(<MyPackagePage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('10 节正价课'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/package/mine/detail/index?packageId=1',
      });
    });
  });

  test('navigates back when navbar back is clicked', async () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockResolvedValue([
      mockActivePackage,
    ]);
    render(<MyPackagePage />);

    await waitFor(() => {
      expect(screen.getByText('我的套餐')).toBeInTheDocument();
    });

    const backButton = document.querySelector('.my-package__back');
    expect(backButton).toBeInTheDocument();
    fireEvent.click(backButton as Element);

    await waitFor(() => {
      expect(Taro.navigateBack).toHaveBeenCalled();
    });
  });

  test('shows error state and retry', async () => {
    (packageApi.fetchMyPackageList as jest.Mock).mockRejectedValue({
      message: '网络错误',
    });
    render(<MyPackagePage />);

    await waitFor(() => {
      expect(screen.getByText('网络错误')).toBeInTheDocument();
    });

    (packageApi.fetchMyPackageList as jest.Mock).mockResolvedValue([
      mockActivePackage,
    ]);
    fireEvent.click(screen.getByText('重新加载'));

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });
  });
});
