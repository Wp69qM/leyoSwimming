import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import Taro from '@tarojs/taro';
import * as orderApi from '@/api/order';
import * as packageApi from '@/api/package';
import * as profileApi from '@/api/profile';
import OrderConfirmPage from './index';

jest.mock('@tarojs/taro', () => ({
  getSystemInfoSync: jest.fn().mockReturnValue({ statusBarHeight: 20 }),
  getCurrentInstance: jest.fn().mockReturnValue({
    router: { params: { packageId: '1', coachId: '10', packageType: '1' } },
  }),
  navigateBack: jest.fn(),
  navigateTo: jest.fn().mockResolvedValue(undefined),
  switchTab: jest.fn(),
  showToast: jest.fn(),
}));

jest.mock('@/stores/authStore', () => ({
  useAuthStore: jest
    .fn()
    .mockImplementation((selector) => selector({ isLoggedIn: true })),
}));

jest.mock('@/api/package', () => ({
  fetchPackageDetail: jest.fn(),
}));

jest.mock('@/api/profile', () => ({
  getProfile: jest.fn(),
}));

jest.mock('@/api/order', () => ({
  createTrialOrder: jest.fn(),
  createFormalOrder: jest.fn(),
  payOrder: jest.fn(),
}));

jest.mock('@/components/common/Icon', () => ({
  Icon: ({ name }: { name: string }) => (
    <span data-testid={`icon-${name}`}>{name}</span>
  ),
}));

jest.mock('@tarojs/components', () => {
  const React = require('react');
  return {
    View: ({
      children,
      ...props
    }: {
      children?: React.ReactNode;
      [key: string]: unknown;
    }) => React.createElement('div', props, children),
    Text: ({
      children,
      ...props
    }: {
      children?: React.ReactNode;
      [key: string]: unknown;
    }) => React.createElement('span', props, children),
    Image: (props: { [key: string]: unknown }) =>
      React.createElement('img', props),
    Input: ({
      onInput,
      value,
      ...props
    }: {
      onInput?: (e: { detail: { value: string } }) => void;
      value?: string;
      [key: string]: unknown;
    }) =>
      React.createElement('input', {
        ...props,
        value,
        onInput: (e: React.FormEvent<HTMLInputElement>) => {
          onInput?.({ detail: { value: e.currentTarget.value } });
        },
      }),
  };
});

const mockCoach = {
  coachId: 10,
  name: '张明远',
  avatarUrl: null,
  rating: '4.9',
  teachingYears: 8,
  totalStudents: 326,
  referencePrice: '200',
  teachingStrokes: ['自由泳', '蛙泳'],
};

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
  images: [],
  applicableCoaches: [mockCoach],
};

const mockExperienceDetail = {
  ...mockStandardDetail,
  name: '新人体验课',
  packageMode: 'experience',
  totalHours: 1,
  originalPrice: '199',
  price: '99',
  applicableCoaches: [{ ...mockCoach, coachId: 10, name: '李海燕' }],
};

const mockAdultProfile = {
  id: 'u1',
  name: '学员 A',
  age: 25,
  guardianPhone: undefined,
  profileCompleted: true,
  consent: {
    termsVersion: '1.0.0',
    privacyVersion: '1.0.0',
    requiredTermsVersion: '1.0.0',
    requiredPrivacyVersion: '1.0.0',
    needsReconsent: false,
  },
};

const mockMinorProfile = {
  ...mockAdultProfile,
  age: 16,
};

function setupMocks({
  packageDetail = mockStandardDetail,
  profile = mockAdultProfile,
  packageType = '1',
}: {
  packageDetail?: typeof mockStandardDetail;
  profile?: typeof mockAdultProfile;
  packageType?: string;
} = {}) {
  (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
    router: {
      params: {
        packageId: String(packageDetail.id),
        coachId: '10',
        packageType,
      },
    },
  });
  (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue(packageDetail);
  (profileApi.getProfile as jest.Mock).mockResolvedValue(profile);
}

describe('OrderConfirmPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: { packageId: '1', coachId: '10', packageType: '1' } },
    });
    (packageApi.fetchPackageDetail as jest.Mock).mockReset();
    (profileApi.getProfile as jest.Mock).mockReset();
    (orderApi.createTrialOrder as jest.Mock).mockReset();
    (orderApi.createFormalOrder as jest.Mock).mockReset();
  });

  test('renders loading skeleton initially', () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockReturnValue(
      new Promise(() => {})
    );
    (profileApi.getProfile as jest.Mock).mockReturnValue(new Promise(() => {}));
    render(<OrderConfirmPage />);
    expect(
      document.querySelector('.order-confirm--skeleton')
    ).toBeInTheDocument();
  });

  test('renders standard package detail', async () => {
    setupMocks();
    render(<OrderConfirmPage />);

    await waitFor(() => {
      expect(screen.getByText('正价套餐')).toBeInTheDocument();
    });

    expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    expect(screen.getByText('10 节')).toBeInTheDocument();
    expect(screen.getByText('商品总价')).toBeInTheDocument();
    expect(document.querySelector('.price-breakdown__total')).toHaveTextContent(
      '1,800'
    );
    expect(screen.getByText('《健康承诺书》')).toBeInTheDocument();
    expect(screen.getByText('《免责协议》')).toBeInTheDocument();
  });

  test('renders experience package detail with fixed quantity 1', async () => {
    setupMocks({ packageDetail: mockExperienceDetail, packageType: '0' });
    render(<OrderConfirmPage />);

    await waitFor(() => {
      expect(screen.getByText('体验套餐')).toBeInTheDocument();
    });

    expect(screen.getByText('新人体验课')).toBeInTheDocument();
    expect(screen.getByText('1 节')).toBeInTheDocument();
    expect(screen.queryByText('《健康承诺书》')).not.toBeInTheDocument();
    expect(screen.queryByText('《免责协议》')).not.toBeInTheDocument();
    expect(screen.getByText('优惠减免')).toBeInTheDocument();
    expect(screen.getByText('-¥100')).toBeInTheDocument();
  });

  test('disables submit button until agreements are checked', async () => {
    setupMocks();
    render(<OrderConfirmPage />);

    await waitFor(() => {
      expect(screen.getByText('提交订单')).toBeInTheDocument();
    });

    const button = document.querySelector('.bottom-submit-bar__button');
    expect(button).toHaveClass('bottom-submit-bar__button--disabled');

    const checkboxes = document.querySelectorAll('.agreement-item__checkbox');
    checkboxes.forEach((cb) => fireEvent.click(cb));

    await waitFor(() => {
      expect(button).not.toHaveClass('bottom-submit-bar__button--disabled');
    });
  });

  test('creates formal order and navigates to payment on submit', async () => {
    setupMocks();
    (orderApi.createFormalOrder as jest.Mock).mockResolvedValue({
      orderId: 100,
      orderNo: 'O20240816001',
      amount: '1800',
      expireAt: '2026-08-17T12:00:00Z',
    });
    render(<OrderConfirmPage />);

    await waitFor(() => {
      expect(screen.getByText('提交订单')).toBeInTheDocument();
    });

    const checkboxes = document.querySelectorAll('.agreement-item__checkbox');
    checkboxes.forEach((cb) => fireEvent.click(cb));

    fireEvent.click(screen.getByText('提交订单'));

    await waitFor(() => {
      expect(orderApi.createFormalOrder).toHaveBeenCalledWith({
        coachId: 10,
        packageId: 1,
        agreementVersions: {
          userNotice: '1.0.0',
          health: '1.0.0',
          disclaimer: '1.0.0',
        },
      });
    });

    expect(Taro.navigateTo).toHaveBeenCalledWith({
      url: '/pages/order/payment/index?orderId=100',
    });
  });

  test('creates trial order for experience package', async () => {
    setupMocks({ packageDetail: mockExperienceDetail, packageType: '0' });
    (orderApi.createTrialOrder as jest.Mock).mockResolvedValue({
      orderId: 101,
      orderNo: 'O20240816002',
      amount: '99',
      expireAt: '2026-08-17T12:00:00Z',
    });
    render(<OrderConfirmPage />);

    await waitFor(() => {
      expect(screen.getByText('提交订单')).toBeInTheDocument();
    });

    fireEvent.click(
      document.querySelector('.agreement-item__checkbox') as Element
    );
    fireEvent.click(screen.getByText('提交订单'));

    await waitFor(() => {
      expect(orderApi.createTrialOrder).toHaveBeenCalledWith({
        coachId: 10,
        packageId: 1,
      });
    });

    expect(Taro.navigateTo).toHaveBeenCalledWith({
      url: '/pages/order/payment/index?orderId=101',
    });
  });

  test('shows guardian section for minor buying formal package', async () => {
    setupMocks({ profile: mockMinorProfile });
    render(<OrderConfirmPage />);

    await waitFor(() => {
      expect(screen.getByText('监护人信息')).toBeInTheDocument();
    });

    expect(
      screen.getByText('您尚未成年，购买正价课需填写监护人手机号')
    ).toBeInTheDocument();
  });

  test('shows error state and allows retry', async () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockRejectedValue({
      message: '网络错误',
    });
    (profileApi.getProfile as jest.Mock).mockRejectedValue({
      message: '网络错误',
    });
    render(<OrderConfirmPage />);

    await waitFor(() => {
      expect(screen.getByText('加载失败，点击重试')).toBeInTheDocument();
    });

    setupMocks();
    fireEvent.click(screen.getByText('重新加载'));

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });
  });

  test('calculates price correctly when originalPrice is zero', async () => {
    setupMocks({
      packageDetail: { ...mockStandardDetail, originalPrice: '0' },
    });
    render(<OrderConfirmPage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    expect(screen.getByText('商品总价')).toBeInTheDocument();
    expect(document.querySelector('.price-breakdown__value')).toHaveTextContent(
      '1,800'
    );
    expect(screen.getByText('-¥0')).toBeInTheDocument();
  });

  test('validates guardian phone format before submit', async () => {
    setupMocks({ profile: mockMinorProfile });
    render(<OrderConfirmPage />);

    await waitFor(() => {
      expect(screen.getByText('提交订单')).toBeInTheDocument();
    });

    const checkboxes = document.querySelectorAll('.agreement-item__checkbox');
    checkboxes.forEach((cb) => fireEvent.click(cb));

    const input = document.querySelector(
      '.guardian-section__input'
    ) as HTMLInputElement;
    fireEvent.input(input, { target: { value: '1234567890' } });

    fireEvent.click(screen.getByText('提交订单'));

    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '请填写正确的监护人手机号',
        icon: 'none',
      });
    });

    expect(orderApi.createFormalOrder).not.toHaveBeenCalled();
  });

  test('shows empty state for invalid package id', async () => {
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: {
        params: { packageId: 'invalid', coachId: '10', packageType: '1' },
      },
    });
    render(<OrderConfirmPage />);

    await waitFor(() => {
      expect(screen.getByText('暂无订单信息')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('返回首页'));
    expect(Taro.switchTab).toHaveBeenCalledWith({ url: '/pages/index/index' });
  });

  test('navigates to coach detail on coach card click', async () => {
    setupMocks();
    render(<OrderConfirmPage />);

    await waitFor(() => {
      expect(screen.getByText('张明远')).toBeInTheDocument();
    });

    fireEvent.click(document.querySelector('.order-coach-card') as Element);

    expect(Taro.navigateTo).toHaveBeenCalledWith({
      url: '/pages/coach/detail/index?id=10',
    });
  });
});
