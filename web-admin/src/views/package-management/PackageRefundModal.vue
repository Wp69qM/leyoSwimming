<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { refundPackage } from '@/api/packageManagement';
import { formatDateTime } from '@/utils/format';

interface Props {
  visible: boolean;
  packageId: number;
  packageNo: string;
  totalHours: number;
  consumedCount: number;
  availableCount: number;
  expireAt: string;
  refundAmount: string;
  refundRatio: string;
  version: number;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success'): void;
}>();

const loading = ref(false);
const formRef = ref();

const maxRefundAmount = computed(() => Number(props.refundAmount));

const form = reactive({
  refundAmount: '',
  reason: '',
  adjustReason: '',
});

function toCents(amount: string | number): number {
  return Math.round(Number(amount) * 100);
}

const isAmountChanged = computed(() => {
  return toCents(form.refundAmount) !== toCents(maxRefundAmount.value);
});

const rules = computed(() => ({
  refundAmount: [
    { required: true, message: '请输入退款金额', trigger: 'blur' },
    {
      validator: (
        _rule: unknown,
        value: string,
        callback: (error?: Error) => void
      ) => {
        const num = Number(value);
        if (Number.isNaN(num) || num <= 0 || num > maxRefundAmount.value) {
          callback(
            new Error(
              `退款金额必须大于 0 且不超过 ${maxRefundAmount.value.toFixed(2)} 元`
            )
          );
          return;
        }
        callback();
      },
      trigger: 'blur',
    },
  ],
  reason: [
    { required: true, message: '退款原因不能为空', trigger: 'blur' },
    { max: 500, message: '退款原因不超过 500 字', trigger: 'blur' },
  ],
  adjustReason: [
    {
      validator: (
        _rule: unknown,
        value: string,
        callback: (error?: Error) => void
      ) => {
        if (isAmountChanged.value && (!value || !value.trim())) {
          callback(new Error('请填写金额调整原因'));
          return;
        }
        callback();
      },
      trigger: 'blur',
    },
    { max: 500, message: '调整原因不超过 500 字', trigger: 'blur' },
  ],
}));

watch(
  () => props.visible,
  (val) => {
    if (val) {
      form.refundAmount = props.refundAmount;
      form.reason = '';
      form.adjustReason = '';
    }
  }
);

function handleClose() {
  emit('update:visible', false);
}

async function handleConfirm() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }

  loading.value = true;
  try {
    const payload: {
      packageId: number;
      reason: string;
      version: number;
      refundAmount?: string;
      adjustReason?: string;
    } = {
      packageId: props.packageId,
      reason: form.reason.trim(),
      version: props.version,
    };
    if (isAmountChanged.value) {
      payload.refundAmount = Number(form.refundAmount).toFixed(2);
      payload.adjustReason = form.adjustReason.trim();
    }

    const res = await refundPackage(payload);
    ElMessage.success(
      res.data?.message || '退款订单已生成，请前往订单管理审批'
    );
    emit('success');
    emit('update:visible', false);
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="发起退款"
    width="520px"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="summary">
      <div class="summary-row">
        <span class="label">套餐编号：</span>
        <span class="value">{{ packageNo }}</span>
      </div>
      <div class="summary-row">
        <span class="label">课时信息：</span>
        <span class="value">
          总 {{ totalHours }} / 已消耗 {{ consumedCount }} / 剩余
          {{ availableCount }}
        </span>
      </div>
      <div class="summary-row">
        <span class="label">到期时间：</span>
        <span class="value">{{ formatDateTime(expireAt) }}</span>
      </div>
      <div class="summary-row">
        <span class="label">退款比例：</span>
        <span class="value">{{ Number(refundRatio).toFixed(0) }}%</span>
      </div>
    </div>

    <el-alert
      title="发起退款后将生成一笔退款订单，请前往订单管理页进行审批。原购买订单状态保持不变。"
      type="info"
      :closable="false"
      show-icon
      class="tip"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="form"
    >
      <el-form-item label="可退金额" prop="refundAmount" required>
        <el-input
          v-model="form.refundAmount"
          placeholder="请输入退款金额"
          style="width: 100%"
        >
          <template #prepend>¥</template>
        </el-input>
        <div class="hint">
          系统计算可退金额：¥{{ maxRefundAmount.toFixed(2) }}
        </div>
      </el-form-item>
      <el-form-item
        v-if="isAmountChanged"
        label="金额调整原因"
        prop="adjustReason"
      >
        <el-input
          v-model="form.adjustReason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="金额与系统计算不一致时必填"
        />
      </el-form-item>
      <el-form-item label="退款原因" prop="reason" required>
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入退款原因"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">
          确认发起退款
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.summary-row {
  display: flex;
  font-size: 14px;
}

.summary-row .label {
  color: #8c8c8c;
}

.summary-row .value {
  color: #262626;
}

.tip {
  margin-bottom: 16px;
}

.form {
  :deep(.el-form-item__label) {
    padding-bottom: 4px;
    font-weight: 500;
  }
}

.hint {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
