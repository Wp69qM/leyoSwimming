import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import Taro from '@tarojs/taro';
import * as packageApi from '@/api/package';
import RefundApplyPage from './index';

const mockPackageId = 123;

const mockRefundablePackage = {
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

const mockFullRefundPackage = {
  ...mockRefundablePackage,
  status: 'frozen' as const,
  frozenReason: 'coach_resigned',
  refundRuleText: '教练离职，可申请 100% 全额退款',
};

const mockNotRefundablePackage = {
  ...mockRefundablePackage,
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
  showToast: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('@/api/package', () => ({
  fetchMyPackageDetail: jest.fn(),
  submitRefund: jest.fn(),
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
    Textarea: ({
      onInput,
      value,
      maxlength,
      placeholderClass: _placeholderClass,
      ...props
    }: {
      onInput?: (e: { detail: { value: string } }) => void;
      value?: string;
      maxlength?: number;
      placeholderClass?: string;
      [key: string]: unknown;
    }) =>
      React.createElement('textarea', {
        ...props,
        value,
        maxLength: maxlength,
        onInput: (e: React.FormEvent<HTMLTextAreaElement>) => {
          onInput?.({ detail: { value: e.currentTarget.value } });
        },
      }),
  };
});

jest.mock('@/components/common/Icon', () => ({
  Icon: ({ name }: { name: string }) => (
    <span data-testid={`icon-${name}`}>{name}</span>
  ),
}));

describe('RefundApplyPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: { packageId: '123' } },
    });
  });

  test('renders loading skeleton initially', () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockReturnValue(
      new Promise(() => {})
    );
    render(<RefundApplyPage />);
    expect(
      document.querySelector('.refund-apply__content--skeleton')
    ).toBeInTheDocument();
  });

  test('shows error when packageId is missing', async () => {
    (Taro.getCurrentInstance as jest.Mock).mockReturnValue({
      router: { params: {} },
    });
    render(<RefundApplyPage />);

    await waitFor(() => {
      expect(screen.getByText('套餐参数缺失')).toBeInTheDocument();
    });
  });

  test('shows error when package is not refundable', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockNotRefundablePackage
    );
    render(<RefundApplyPage />);

    await waitFor(() => {
      expect(screen.getByText('当前套餐不符合退款条件')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('返回我的套餐'));

    await waitFor(() => {
      expect(Taro.redirectTo).toHaveBeenCalledWith({
        url: '/pages/package/mine/index',
      });
    });
  });

  test('renders package snapshot and refund amount', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockRefundablePackage
    );
    render(<RefundApplyPage />);

    await waitFor(() => {
      expect(screen.getByText('10 节正价课')).toBeInTheDocument();
    });

    expect(screen.getByText('正价套餐')).toBeInTheDocument();
    expect(screen.getByText('教练：张明远 · 一对一')).toBeInTheDocument();
    expect(
      screen.getByText('泳姿：自由泳/蛙泳 · 60 分钟/节')
    ).toBeInTheDocument();
    expect(screen.getByText('购买时间')).toBeInTheDocument();
    expect(screen.getByText('2026-08-02 14:30:00')).toBeInTheDocument();
    expect(
      screen.getByText(
        '退款金额 = 实付金额 × (总课时 - 已用课时) / 总课时 × 退款比例'
      )
    ).toBeInTheDocument();
    expect(
      document.querySelector('.amount-card__amount-value')
    ).toHaveTextContent('¥864.00');
  });

  test('shows full refund rule for coach resigned package', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockFullRefundPackage
    );
    render(<RefundApplyPage />);

    await waitFor(() => {
      expect(
        screen.getByText('教练离职，按实付金额全额退款')
      ).toBeInTheDocument();
    });

    expect(
      document.querySelector('.amount-card__amount-value')
    ).toHaveTextContent('¥1,800.00');
  });

  test('selects refund reason and enables submit button', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockRefundablePackage
    );
    render(<RefundApplyPage />);

    await waitFor(() => {
      expect(screen.getByText('请选择退款原因')).toBeInTheDocument();
    });

    const submitButton = screen.getByText('提交申请');
    expect(submitButton.parentElement).toHaveClass(
      'submit-bar__button--disabled'
    );

    fireEvent.click(screen.getByText('请选择退款原因'));

    await waitFor(() => {
      expect(screen.getByText('选择退款原因')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('个人原因'));

    await waitFor(() => {
      expect(screen.getByText('个人原因')).toBeInTheDocument();
    });

    expect(submitButton.parentElement).not.toHaveClass(
      'submit-bar__button--disabled'
    );
  });

  test('shows toast when submit without selecting reason', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockRefundablePackage
    );
    render(<RefundApplyPage />);

    await waitFor(() => {
      expect(screen.getByText('提交申请')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('提交申请'));

    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '请选择退款原因',
        icon: 'none',
      });
    });

    expect(packageApi.submitRefund).not.toHaveBeenCalled();
  });

  test('submits refund and navigates to order detail on success', async () => {
    jest.useFakeTimers();
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockRefundablePackage
    );
    (packageApi.submitRefund as jest.Mock).mockResolvedValue({
      orderId: 999,
      orderNo: 'O-20260817-001',
      refundAmount: '480.00',
    });

    render(<RefundApplyPage />);

    await waitFor(() => {
      expect(screen.getByText('请选择退款原因')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('请选择退款原因'));

    await waitFor(() => {
      expect(screen.getByText('选择退款原因')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('个人原因'));

    const textarea = document.querySelector('.reason-textarea__input');
    fireEvent.input(textarea as Element, {
      target: { value: '临时有事' },
    });

    fireEvent.click(screen.getByText('提交申请'));

    await waitFor(() => {
      expect(packageApi.submitRefund).toHaveBeenCalledWith({
        packageId: mockPackageId,
        reason: '个人原因：临时有事',
      });
    });

    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '申请已提交',
        icon: 'success',
      });
    });

    jest.advanceTimersByTime(1500);

    await waitFor(() => {
      expect(Taro.redirectTo).toHaveBeenCalledWith({
        url: '/pages/order/detail/index?orderId=999',
      });
    });

    jest.useRealTimers();
  });

  test('does not submit twice while submitting', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockRefundablePackage
    );
    (packageApi.submitRefund as jest.Mock).mockReturnValue(
      new Promise(() => {})
    );

    render(<RefundApplyPage />);

    await waitFor(() => {
      expect(screen.getByText('请选择退款原因')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('请选择退款原因'));

    await waitFor(() => {
      expect(screen.getByText('选择退款原因')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('个人原因'));

    await waitFor(() => {
      expect(screen.getByText('个人原因')).toBeInTheDocument();
    });

    const submitButton = screen.getByText('提交申请');
    fireEvent.click(submitButton);
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(packageApi.submitRefund).toHaveBeenCalledTimes(1);
    });
  });

  test('shows error toast when submit fails', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockRefundablePackage
    );
    (packageApi.submitRefund as jest.Mock).mockRejectedValue({
      message: '退款申请失败',
    });

    render(<RefundApplyPage />);

    await waitFor(() => {
      expect(screen.getByText('请选择退款原因')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('请选择退款原因'));

    await waitFor(() => {
      expect(screen.getByText('选择退款原因')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('平台原因'));

    fireEvent.click(screen.getByText('提交申请'));

    await waitFor(() => {
      expect(Taro.showToast).toHaveBeenCalledWith({
        title: '退款申请失败',
        icon: 'none',
      });
    });
  });

  test('navigates back when back button clicked', async () => {
    (packageApi.fetchMyPackageDetail as jest.Mock).mockResolvedValue(
      mockRefundablePackage
    );
    render(<RefundApplyPage />);

    await waitFor(() => {
      expect(screen.getByText('申请退款')).toBeInTheDocument();
    });

    const backButton = document.querySelector('.refund-apply__back');
    expect(backButton).toBeInTheDocument();
    fireEvent.click(backButton as Element);

    await waitFor(() => {
      expect(Taro.navigateBack).toHaveBeenCalled();
    });
  });
});
