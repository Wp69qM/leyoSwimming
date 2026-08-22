import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import Taro from '@tarojs/taro';
import * as packageApi from '@/api/package';
import PackageDetailPage from './index';

jest.mock('@tarojs/taro', () => ({
  getSystemInfoSync: jest.fn().mockReturnValue({ statusBarHeight: 20 }),
  getCurrentInstance: jest
    .fn()
    .mockReturnValue({ router: { params: { id: '1' } } }),
  navigateTo: jest.fn().mockResolvedValue(undefined),
  navigateBack: jest.fn(),
  switchTab: jest.fn(),
  previewImage: jest.fn(),
}));

jest.mock('@/stores/authStore', () => ({
  useAuthStore: jest
    .fn()
    .mockImplementation((selector) => selector({ isLoggedIn: true })),
}));

jest.mock('@/api/package', () => ({
  fetchPackageDetail: jest.fn(),
}));

jest.mock('@/components/common/Icon', () => ({
  Icon: ({ name }: { name: string }) => (
    <span data-testid={`icon-${name}`}>{name}</span>
  ),
}));

const mockStandardDetail = {
  id: 1,
  name: '10 节正价课',
  packageMode: 'standard',
  teachingType: '一对一',
  totalHours: 10,
  durationMinutes: 60,
  validDays: 90,
  originalPrice: '2000',
  price: '1800',
  refundEnabled: true,
  refundRatio: '0.8',
  refundValidDays: 7,
  refundPolicySummary: null,
  tags: ['热销', '推荐'],
  description: '本套餐适合有一定游泳基础的学员。',
  images: ['https://example.com/a.jpg'],
  applicableCoaches: [
    {
      coachId: 10,
      name: '张明远',
      avatarUrl: null,
      rating: '4.9',
      teachingYears: 8,
      totalStudents: 326,
      referencePrice: '200',
      teachingStrokes: ['自由泳', '蛙泳'],
    },
  ],
};

const mockExperienceDetail = {
  ...mockStandardDetail,
  name: '新人体验课',
  packageMode: 'experience',
  totalHours: 1,
  price: '99',
  tags: [],
  applicableCoaches: [
    {
      coachId: 11,
      name: '李海燕',
      avatarUrl: null,
      rating: '4.7',
      teachingYears: 5,
      totalStudents: 128,
      referencePrice: '99',
      teachingStrokes: [],
    },
  ],
};

describe('PackageDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders loading skeleton initially', () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockReturnValue(
      new Promise(() => {})
    );
    render(<PackageDetailPage />);
    expect(
      document.querySelector('.package-detail--skeleton')
    ).toBeInTheDocument();
  });

  test('renders standard package detail', async () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue(
      mockStandardDetail
    );
    render(<PackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    expect(document.querySelector('.main-info-card__price')).toHaveTextContent(
      '1,800'
    );
    expect(
      document.querySelector('.main-info-card__original-price')
    ).toHaveTextContent('2,000');
    expect(screen.getByText('套餐包含')).toBeInTheDocument();
    expect(
      screen.getByText('10 节一对一游泳私教课（60 分钟/节）')
    ).toBeInTheDocument();
  });

  test('renders experience package detail with 30 days validity', async () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue(
      mockExperienceDetail
    );
    render(<PackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('新人体验课')).toBeInTheDocument();
    });

    expect(screen.getByText('有效期 30 天')).toBeInTheDocument();
    expect(screen.getByText('体验课有效期 30 天')).toBeInTheDocument();
    expect(screen.getByText('1 节 · 30 天有效')).toBeInTheDocument();
  });

  test('shows error state and retry', async () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockRejectedValue({
      message: '网络错误',
    });
    render(<PackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('网络错误')).toBeInTheDocument();
    });

    (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue(
      mockStandardDetail
    );
    fireEvent.click(screen.getByText('重新加载'));

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });
  });

  test('navigates to order confirm for standard package', async () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue(
      mockStandardDetail
    );
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: { id: '1', source: 'coach', coachId: '10' } },
    });
    render(<PackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('立即购买')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('立即购买'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/order/confirm/index?packageId=1&coachId=10&packageType=1',
      });
    });
  });

  test('navigates to custom config for custom package', async () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue({
      ...mockStandardDetail,
      packageMode: 'custom',
      totalHours: 0,
    });
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: { id: '1', source: 'coach', coachId: '10' } },
    });
    render(<PackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('立即购买')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('立即购买'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/package/custom/index?packageId=1&coachId=10',
      });
    });
  });

  test('requires coach selection when source is list', async () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue(
      mockStandardDetail
    );
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: { id: '1', source: 'list' } },
    });
    render(<PackageDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('请选择教练')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('请选择教练'));
    expect(Taro.navigateTo).not.toHaveBeenCalled();
  });

  test('previews image on click', async () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue(
      mockStandardDetail
    );
    render(<PackageDetailPage />);

    await waitFor(() => {
      expect(
        document.querySelector('.section-card__image')
      ).toBeInTheDocument();
    });

    fireEvent.click(document.querySelector('.section-card__image') as Element);

    expect(Taro.previewImage).toHaveBeenCalledWith({
      urls: mockStandardDetail.images,
      current: mockStandardDetail.images[0],
    });
  });
});
