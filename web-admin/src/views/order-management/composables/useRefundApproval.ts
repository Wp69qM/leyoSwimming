import { ElMessage, ElMessageBox } from 'element-plus';
import { approveRefund, rejectRefund } from '@/api/orderManagement';

interface ApproveParams {
  orderId: number;
  defaultAmount: string;
}

interface RejectParams {
  orderId: number;
}

export function useRefundApproval(onSuccess: () => void | Promise<void>) {
  async function approve({ orderId, defaultAmount }: ApproveParams) {
    try {
      const { value } = await ElMessageBox.prompt(
        '请输入实际退款金额（可低于系统计算金额）',
        '通过退款申请',
        {
          confirmButtonText: '确认通过',
          cancelButtonText: '取消',
          inputType: 'text',
          inputValue: defaultAmount,
          inputPattern: /^\d+(\.\d{1,2})?$/,
          inputErrorMessage: '请输入有效的金额，最多两位小数',
          inputValidator: (val) => {
            const num = Number(val);
            if (Number.isNaN(num) || num <= 0) {
              return '退款金额必须大于 0';
            }
            const max = Number(defaultAmount);
            if (!Number.isNaN(max) && num > max) {
              return `退款金额不能超过可退金额 ${defaultAmount} 元`;
            }
            return true;
          },
          type: 'warning',
        }
      );
      const { value: adjustReason } = await ElMessageBox.prompt(
        '请输入调整原因（未调整可填“无”）',
        '退款金额调整原因',
        {
          confirmButtonText: '提交',
          cancelButtonText: '取消',
          inputPattern: /\S+/,
          inputErrorMessage: '请输入调整原因',
        }
      );
      await approveRefund({
        orderId,
        refundAmount: value.trim(),
        adjustReason: adjustReason.trim(),
      });
      ElMessage.success('退款申请已通过');
      await onSuccess();
    } catch (err) {
      if (err instanceof Error && err.message !== 'cancel') {
        ElMessage.error(err.message);
      }
    }
  }

  async function reject({ orderId }: RejectParams) {
    try {
      const { value } = await ElMessageBox.prompt(
        '请输入驳回原因，学员将收到该原因',
        '驳回退款申请',
        {
          confirmButtonText: '确认驳回',
          cancelButtonText: '取消',
          inputPattern: /\S+/,
          inputErrorMessage: '请输入驳回原因',
          type: 'warning',
        }
      );
      await rejectRefund({
        orderId,
        rejectedReason: value.trim(),
      });
      ElMessage.success('退款申请已驳回，关联套餐已恢复为生效中');
      await onSuccess();
    } catch (err) {
      if (err instanceof Error && err.message !== 'cancel') {
        ElMessage.error(err.message);
      }
    }
  }

  return { approve, reject };
}
