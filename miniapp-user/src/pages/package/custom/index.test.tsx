import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import Taro from '@tarojs/taro';
import { ApiError } from '@/api/request';
import * as orderApi from '@/api/order';
import * as packageApi from '@/api/package';
import * as profileApi from '@/api/profile';
import CustomPackageConfigPage from './index';

jest.mock('@tarojs/taro', () => ({
  getSystemInfoSync: jest.fn().mockReturnValue({ statusBarHeight: 20 }),
  getCurrentInstance: jest.fn().mockReturnValue({
    router: { params: { packageId: '-1', coachId: '10' } },
  }),
  navigateBack: jest.fn(),
  navigateTo: jest.fn().mockResolvedValue(undefined),
  switchTab: jest.fn(),
  showToast: jest.fn(),
}));

jest.mock('@/api/package', () => ({
  fetchPackageDetail: jest.fn(),
  fetchCustomPackageConfig: jest.fn(),
  fetchActivePackage: jest.fn(),
}));

jest.mock('@/api/profile', () => ({
  getProfile: jest.fn(),
}));

jest.mock('@/api/order', () => ({
  createFormalOrder: jest.fn(),
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
      onBlur,
      value,
      maxlength,
      ...props
    }: {
      onInput?: (e: { detail: { value: string } }) => void;
      onBlur?: () => void;
      value?: string;
      maxlength?: number;
      [key: string]: unknown;
    }) =>
      React.createElement('input', {
        ...props,
        value,
        maxLength: maxlength,
        onInput: (e: React.FormEvent<HTMLInputElement>) => {
          onInput?.({ detail: { value: e.currentTarget.value } });
        },
        onBlur,
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
  status: 1,
};

const mockCustomDetail = {
  id: -1,
  name: '自定义正价课',
  packageMode: 'custom',
  teachingType: '一对一',
  totalHours: 0,
  durationMinutes: 60,
  validDays: 90,
  originalPrice: '0',
  price: '0',
  refundEnabled: true,
  refundRatio: '0.8',
  refundValidDays: 7,
  refundPolicySummary: null,
  tags: [],
  description: null,
  images: [],
  applicableCoaches: [mockCoach],
};

const mockConfig = {
  minHours: 1,
  maxHours: 50,
  defaultValidDays: 30,
  allowedValidDays: [30, 60, 90, 180],
};

const mockAdultProfile = {
  id: 'u1',
  name: '学员 A',
  age: 25,
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
  profile = mockAdultProfile,
  activePackage = null,
}: {
  profile?: typeof mockAdultProfile;
  activePackage?: {
    id: number;
    coachId: number;
    coachName: string;
    status: string;
  } | null;
} = {}) {
  (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue(
    mockCustomDetail
  );
  (packageApi.fetchCustomPackageConfig as jest.Mock).mockResolvedValue(
    mockConfig
  );
  (packageApi.fetchActivePackage as jest.Mock).mockResolvedValue(activePackage);
  (profileApi.getProfile as jest.Mock).mockResolvedValue(profile);
}

function checkAllAgreements() {
  ['userNotice', 'health', 'disclaimer'].forEach((key) => {
    fireEvent.click(screen.getByTestId(`agreement-checkbox-${key}`));
  });
}

describe('CustomPackageConfigPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: { packageId: '-1', coachId: '10' } },
    });
    (packageApi.fetchPackageDetail as jest.Mock).mockReset();
    (packageApi.fetchCustomPackageConfig as jest.Mock).mockReset();
    (packageApi.fetchActivePackage as jest.Mock)
      .mockReset()
      .mockResolvedValue(null);
    (profileApi.getProfile as jest.Mock).mockReset().mockResolvedValue(null);
    (orderApi.createFormalOrder as jest.Mock).mockReset();
  });

  test('renders loading skeleton initially', () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockReturnValue(
      new Promise(() => {})
    );
    (packageApi.fetchCustomPackageConfig as jest.Mock).mockReturnValue(
      new Promise(() => {})
    );
    (profileApi.getProfile as jest.Mock).mockReturnValue(new Promise(() => {}));
    render(<CustomPackageConfigPage />);
    expect(screen.getByTestId('custom-config-skeleton')).toBeInTheDocument();
  });

  test('renders coach info and default price', async () => {
    setupMocks();
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(
        screen.getByText('张明远', { selector: '.selected-coach-card__name' })
      ).toBeInTheDocument();
    });

    expect(screen.getByText('参考单价 ¥200/节')).toBeInTheDocument();
    expect(screen.getByText('8 节')).toBeInTheDocument();
    expect(screen.getByText('30 天')).toBeInTheDocument();
    expect(screen.getByText('¥1,600')).toBeInTheDocument();
  });

  test('updates price when hours changed', async () => {
    setupMocks();
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('¥1,600')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('12节'));

    await waitFor(() => {
      expect(screen.getByText('¥2,400')).toBeInTheDocument();
    });
  });

  test('validates custom hours range and clamps on blur', async () => {
    setupMocks();
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('可选范围 1 ~ 50 节')).toBeInTheDocument();
    });

    const input = screen.getByTestId('hours-input');
    fireEvent.input(input, { target: { value: '0' } });

    await waitFor(() => {
      expect(screen.getByText('最少购买 1 节')).toBeInTheDocument();
    });

    expect(screen.getByTestId('submit-button')).toHaveClass(
      'bottom-submit-bar__button--disabled'
    );

    fireEvent.blur(input);

    await waitFor(() => {
      expect(screen.getByDisplayValue('1')).toBeInTheDocument();
    });
  });

  test('changes validity days', async () => {
    setupMocks();
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('30 天')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('180天'));

    await waitFor(() => {
      expect(screen.getByText('180 天')).toBeInTheDocument();
    });
  });

  test('toggles stroke selection with at least one kept', async () => {
    setupMocks();
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('选择泳姿')).toBeInTheDocument();
    });

    expect(screen.getByText('自由泳').parentElement).toHaveClass(
      'stroke-selector__pill--selected'
    );
    expect(screen.getByText('蛙泳').parentElement).toHaveClass(
      'stroke-selector__pill--selected'
    );

    fireEvent.click(screen.getByText('蛙泳'));

    await waitFor(() => {
      expect(screen.getByText('蛙泳').parentElement).not.toHaveClass(
        'stroke-selector__pill--selected'
      );
    });

    fireEvent.click(screen.getByText('蛙泳'));

    await waitFor(() => {
      expect(screen.getByText('蛙泳').parentElement).toHaveClass(
        'stroke-selector__pill--selected'
      );
    });
  });

  test('shows toast when unchecking the last stroke', async () => {
    setupMocks();
    (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue({
      ...mockCustomDetail,
      applicableCoaches: [{ ...mockCoach, teachingStrokes: ['自由泳'] }],
    });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('选择泳姿')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('自由泳'));

    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '至少选择一种泳姿',
        icon: 'none',
      });
    });

    expect(screen.getByText('自由泳').parentElement).toHaveClass(
      'stroke-selector__pill--selected'
    );
  });

  test('disables submit until agreements checked', async () => {
    setupMocks();
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('提交订单')).toBeInTheDocument();
    });

    expect(screen.getByTestId('submit-button')).toHaveClass(
      'bottom-submit-bar__button--disabled'
    );

    checkAllAgreements();

    await waitFor(() => {
      expect(screen.getByTestId('submit-button')).not.toHaveClass(
        'bottom-submit-bar__button--disabled'
      );
    });
  });

  test('shows guardian section for minor and disables submit without phone', async () => {
    setupMocks({ profile: mockMinorProfile });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('监护人信息')).toBeInTheDocument();
    });

    checkAllAgreements();

    expect(screen.getByTestId('submit-button')).toHaveClass(
      'bottom-submit-bar__button--disabled'
    );
  });

  test('creates formal order and navigates to payment', async () => {
    setupMocks();
    (orderApi.createFormalOrder as jest.Mock).mockResolvedValue({
      orderId: 200,
      orderNo: 'O20240816003',
      amount: '1600',
      expireAt: '2026-08-17T12:00:00Z',
    });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('提交订单')).toBeInTheDocument();
    });

    checkAllAgreements();

    fireEvent.click(screen.getByText('提交订单'));

    await waitFor(() => {
      expect(orderApi.createFormalOrder).toHaveBeenCalledWith({
        packageId: -1,
        coachId: 10,
        hours: 8,
        validDays: 30,
        strokeIds: [1, 2],
        agreementVersions: {
          userNotice: '1.0.0',
          health: '1.0.0',
          disclaimer: '1.0.0',
        },
      });
    });

    expect(Taro.navigateTo).toHaveBeenCalledWith({
      url: '/pages/order/payment/index?orderId=200',
    });
  });

  test('shows error state and allows retry', async () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockRejectedValue({
      message: '网络错误',
    });
    (packageApi.fetchCustomPackageConfig as jest.Mock).mockRejectedValue({
      message: '网络错误',
    });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('网络错误')).toBeInTheDocument();
    });

    setupMocks();
    fireEvent.click(screen.getByText('重新加载'));

    await waitFor(() => {
      expect(
        screen.getByText('张明远', { selector: '.selected-coach-card__name' })
      ).toBeInTheDocument();
    });
  });

  test('shows empty state for invalid params', async () => {
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: { packageId: 'invalid', coachId: 'invalid' } },
    });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('暂无配置信息')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('返回首页'));
    expect(Taro.switchTab).toHaveBeenCalledWith({ url: '/pages/index/index' });
  });

  test('accepts synthetic custom package id -1', async () => {
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: { packageId: '-1', coachId: '10' } },
    });
    setupMocks();
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(
        screen.getByText('张明远', { selector: '.selected-coach-card__name' })
      ).toBeInTheDocument();
    });

    expect(packageApi.fetchPackageDetail).toHaveBeenCalledWith(-1, 10);
  });

  test('shows conflict banner when user has active package with another coach', async () => {
    setupMocks({
      activePackage: {
        id: 99,
        coachId: 20,
        coachName: '李教练',
        status: 'active',
      },
    });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(
        screen.getByText('您已绑定其他教练，需先退订才能购买当前套餐')
      ).toBeInTheDocument();
    });

    expect(screen.getByText('更换教练')).toBeInTheDocument();
  });

  test('disables submit when user has active package with another coach', async () => {
    setupMocks({
      activePackage: {
        id: 99,
        coachId: 20,
        coachName: '李教练',
        status: 'active',
      },
    });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('提交订单')).toBeInTheDocument();
    });

    checkAllAgreements();

    expect(screen.getByTestId('submit-button')).toHaveClass(
      'bottom-submit-bar__button--disabled'
    );
  });

  test('allows submit when active package belongs to current coach', async () => {
    setupMocks({
      activePackage: {
        id: 99,
        coachId: 10,
        coachName: '张明远',
        status: 'active',
      },
    });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('提交订单')).toBeInTheDocument();
    });

    checkAllAgreements();

    await waitFor(() => {
      expect(screen.getByTestId('submit-button')).not.toHaveClass(
        'bottom-submit-bar__button--disabled'
      );
    });
  });

  test('navigates to coach list when change coach clicked', async () => {
    setupMocks({
      activePackage: {
        id: 99,
        coachId: 20,
        coachName: '李教练',
        status: 'active',
      },
    });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('更换教练')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('更换教练'));

    expect(Taro.switchTab).toHaveBeenCalledWith({
      url: '/pages/coach/index',
    });
  });

  test('shows warning banner when coach is unavailable', async () => {
    setupMocks();
    (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue({
      ...mockCustomDetail,
      applicableCoaches: [{ ...mockCoach, status: 3 }],
    });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(
        screen.getByText('该教练当前不可购买，请更换教练或稍后再试')
      ).toBeInTheDocument();
    });

    checkAllAgreements();
    expect(screen.getByTestId('submit-button')).toHaveClass(
      'bottom-submit-bar__button--disabled'
    );
  });

  test('disables submit for minor without guardian phone', async () => {
    setupMocks({ profile: mockMinorProfile });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('提交订单')).toBeInTheDocument();
    });

    checkAllAgreements();

    expect(screen.getByTestId('submit-button')).toHaveClass(
      'bottom-submit-bar__button--disabled'
    );
  });

  test('filters stroke options by coach teaching strokes', async () => {
    setupMocks();
    (packageApi.fetchPackageDetail as jest.Mock).mockResolvedValue({
      ...mockCustomDetail,
      applicableCoaches: [{ ...mockCoach, teachingStrokes: ['自由泳'] }],
    });
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(screen.getByText('选择泳姿')).toBeInTheDocument();
    });

    expect(screen.queryByText('蛙泳')).not.toBeInTheDocument();
    expect(screen.getByText('自由泳')).toBeInTheDocument();
  });

  test('shows warning banner for off-shelf package', async () => {
    (packageApi.fetchPackageDetail as jest.Mock).mockRejectedValue(
      new ApiError(420001, '套餐不存在')
    );
    render(<CustomPackageConfigPage />);

    await waitFor(() => {
      expect(
        screen.getByText('该套餐已下架或不可用，请重新选择')
      ).toBeInTheDocument();
    });
  });
});
