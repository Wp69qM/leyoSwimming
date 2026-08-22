<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { extendPackage } from '@/api/packageManagement';
import { formatDateTime } from '@/utils/format';

interface Props {
  visible: boolean;
  packageId: number;
  packageNo: string;
  availableCount: number;
  currentExpireAt: string;
  version: number;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success'): void;
}>();

const loading = ref(false);
const formRef = ref();

const form = reactive({
  newExpireAt: '',
  reason: '',
});

const rules = computed(() => ({
  newExpireAt: [
    { required: true, message: '请选择新到期时间', trigger: 'change' },
    {
      validator: (
        _rule: unknown,
        value: string,
        callback: (error?: Error) => void
      ) => {
        if (!value) {
          callback(new Error('请选择新到期时间'));
          return;
        }
        if (new Date(value) <= new Date()) {
          callback(new Error('新到期时间必须晚于当前时间'));
          return;
        }
        if (new Date(value) <= new Date(props.currentExpireAt)) {
          callback(new Error('新到期时间必须晚于原到期时间'));
          return;
        }
        callback();
      },
      trigger: 'change',
    },
  ],
  reason: [
    { required: true, message: '请输入延期原因', trigger: 'blur' },
    { max: 200, message: '延期原因不超过 200 字', trigger: 'blur' },
  ],
}));

watch(
  () => props.visible,
  (val) => {
    if (val) {
      form.newExpireAt = '';
      form.reason = '';
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
    const res = await extendPackage({
      packageId: props.packageId,
      newExpireAt: form.newExpireAt,
      reason: form.reason,
      version: props.version,
    });
    ElMessage.success(res.data?.message || '套餐已延期');
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
    title="延期套餐"
    width="480px"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="summary">
      <div class="summary-item">
        <span class="label">套餐编号：</span>
        <span class="value">{{ packageNo }}</span>
      </div>
      <div class="summary-item">
        <span class="label">剩余课时：</span>
        <span class="value">{{ availableCount }} 课时</span>
      </div>
      <div class="summary-item">
        <span class="label">当前到期时间：</span>
        <span class="value">{{ formatDateTime(currentExpireAt) }}</span>
      </div>
    </div>

    <el-alert
      title="延期后套餐有效期将更新，已消耗的课时不受影响。"
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
      <el-form-item label="新到期时间" prop="newExpireAt" required>
        <el-date-picker
          v-model="form.newExpireAt"
          type="datetime"
          placeholder="选择新到期时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="延期原因" prop="reason" required>
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="请输入延期原因"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">
          确认延期
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

.summary-item {
  display: flex;
  font-size: 14px;
}

.summary-item .label {
  color: #8c8c8c;
}

.summary-item .value {
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
