import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import Taro from '@tarojs/taro';
import * as packageApi from '@/api/package';
import PackageListPage from './index';

jest.mock('@tarojs/taro', () => ({
  getSystemInfoSync: jest.fn().mockReturnValue({ statusBarHeight: 20 }),
  navigateTo: jest.fn().mockResolvedValue(undefined),
  navigateBack: jest.fn(),
}));

jest.mock('@/api/package', () => ({
  fetchPackageList: jest.fn(),
}));

jest.mock('@/components/common/Icon', () => ({
  Icon: ({ name }: { name: string }) => (
    <span data-testid={`icon-${name}`}>{name}</span>
  ),
}));

const mockItems = [
  {
    id: 1,
    name: '新人体验课',
    packageMode: 'experience',
    teachingType: '一对一',
    totalHours: 1,
    durationMinutes: 60,
    validDays: 30,
    originalPrice: '199',
    price: '99',
    tags: [],
    imageUrl: null,
    refundPolicySummary: null,
  },
  {
    id: 2,
    name: '10 节正价课',
    packageMode: 'standard',
    teachingType: '一对一',
    totalHours: 10,
    durationMinutes: 60,
    validDays: 90,
    originalPrice: '2000',
    price: '1800',
    tags: ['热销'],
    imageUrl: null,
    refundPolicySummary: null,
  },
  {
    id: -1,
    name: '自定义课时',
    packageMode: 'custom',
    teachingType: '一对一',
    totalHours: 0,
    durationMinutes: 60,
    validDays: 90,
    originalPrice: '0',
    price: '0',
    tags: [],
    imageUrl: null,
    refundPolicySummary: null,
  },
];

describe('PackageListPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders loading skeleton initially', () => {
    (packageApi.fetchPackageList as jest.Mock).mockReturnValue(
      new Promise(() => {})
    );
    render(<PackageListPage />);
    expect(
      document.querySelector('.package-list--skeleton')
    ).toBeInTheDocument();
  });

  test('renders experience, standard and custom entries', async () => {
    (packageApi.fetchPackageList as jest.Mock).mockResolvedValue({
      items: mockItems,
    });
    render(<PackageListPage />);

    await waitFor(() => {
      expect(screen.getByText('新人体验课')).toBeInTheDocument();
    });

    expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    expect(screen.getByText('自定义课时')).toBeInTheDocument();
  });

  test('navigates to package detail when custom entry is clicked', async () => {
    (packageApi.fetchPackageList as jest.Mock).mockResolvedValue({
      items: mockItems,
    });
    render(<PackageListPage />);

    await waitFor(() => {
      expect(screen.getByText('自定义课时')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('自定义课时'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/package/detail/index?id=-1',
      });
    });
  });

  test('hides custom entry when no custom template exists', async () => {
    (packageApi.fetchPackageList as jest.Mock).mockResolvedValue({
      items: mockItems.filter((item) => item.packageMode !== 'custom'),
    });
    render(<PackageListPage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    expect(screen.queryByText('自定义课时')).not.toBeInTheDocument();
  });

  test('shows fallback price label when custom template price is zero', async () => {
    (packageApi.fetchPackageList as jest.Mock).mockResolvedValue({
      items: mockItems,
    });
    render(<PackageListPage />);

    await waitFor(() => {
      expect(screen.getByText('自定义课时')).toBeInTheDocument();
    });

    expect(screen.getByText('按课时灵活计价')).toBeInTheDocument();
  });

  test('shows custom entry reference price when template price is set', async () => {
    const customWithPrice = mockItems.map((item) =>
      item.packageMode === 'custom' ? { ...item, price: '250' } : item
    );
    (packageApi.fetchPackageList as jest.Mock).mockResolvedValue({
      items: customWithPrice,
    });
    render(<PackageListPage />);

    await waitFor(() => {
      expect(screen.getByText('自定义课时')).toBeInTheDocument();
    });

    expect(screen.getByText('参考 ¥250/节')).toBeInTheDocument();
  });

  test('shows error state and retry', async () => {
    (packageApi.fetchPackageList as jest.Mock).mockRejectedValue({
      message: '网络错误',
    });
    render(<PackageListPage />);

    await waitFor(() => {
      expect(screen.getByText('网络错误')).toBeInTheDocument();
    });

    (packageApi.fetchPackageList as jest.Mock).mockResolvedValue({
      items: mockItems,
    });
    fireEvent.click(screen.getByText('点击重试'));

    await waitFor(() => {
      expect(screen.getByText('新人体验课')).toBeInTheDocument();
    });
  });
});
