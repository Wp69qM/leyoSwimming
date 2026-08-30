<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { freezePackage, unfreezePackage } from '@/api/packageManagement';
import type { PackageStatus } from '@/types/api';
import { ADMIN_FREEZE_REASON_OPTIONS } from './constants';
import { formatDateTime } from '@/utils/format';

interface Props {
  visible: boolean;
  packageId: number;
  status: PackageStatus;
  packageNo: string;
  availableCount: number;
  expireAt: string;
  frozenReason?: string | null;
  version: number;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success'): void;
}>();

const loading = ref(false);
const isFreeze = computed(() => props.status === 'active');
const title = computed(() => (isFreeze.value ? '冻结套餐' : '解冻套餐'));

const form = reactive({
  reason: '',
  note: '',
});

watch(
  () => props.visible,
  (val) => {
    if (val) {
      form.reason = '';
      form.note = '';
    }
  }
);

function handleClose() {
  emit('update:visible', false);
}

async function handleConfirm() {
  if (isFreeze.value && !form.reason) {
    ElMessage.warning('请选择冻结原因');
    return;
  }

  try {
    await ElMessageBox.confirm(
      isFreeze.value
        ? '确认冻结后该套餐将无法预约课程，是否继续？'
        : '确认解冻该套餐？',
      '二次确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    loading.value = true;
    if (isFreeze.value) {
      const reasonDetail = form.note
        ? `${form.reason}：${form.note}`
        : form.reason;
      const res = await freezePackage({
        packageId: props.packageId,
        reasonDetail,
        version: props.version,
      });
      ElMessage.success(res.data?.message || '套餐已冻结');
    } else {
      const res = await unfreezePackage({
        packageId: props.packageId,
        version: props.version,
      });
      ElMessage.success(res.data?.message || '套餐已解冻');
    }
    emit('success');
    emit('update:visible', false);
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err instanceof Error ? err.message : '操作失败');
    }
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="title"
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
        <span class="label">到期时间：</span>
        <span class="value">{{ formatDateTime(expireAt) }}</span>
      </div>
    </div>

    <el-alert
      v-if="isFreeze"
      title="冻结后该套餐将无法继续预约课程，已有的未上课预约将被取消。"
      type="warning"
      :closable="false"
      show-icon
      class="tip"
    />
    <el-alert
      v-else
      title="解冻后该套餐恢复为可预约状态，学员可继续预约课程。"
      type="info"
      :closable="false"
      show-icon
      class="tip"
    />

    <template v-if="isFreeze">
      <el-form label-position="top" class="form">
        <el-form-item label="冻结原因" required>
          <el-radio-group v-model="form.reason">
            <el-radio
              v-for="option in ADMIN_FREEZE_REASON_OPTIONS"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注（可选）">
          <el-input
            v-model="form.note"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="管理员补充说明"
          />
        </el-form-item>
      </el-form>
    </template>

    <template v-else>
      <div class="freeze-reason">
        <span class="label">当前冻结原因：</span>
        <span class="value">{{ frozenReason || '-' }}</span>
      </div>
    </template>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">
          {{ isFreeze ? '确认冻结' : '确认解冻' }}
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

.summary-item,
.freeze-reason {
  display: flex;
  font-size: 14px;
}

.summary-item .label,
.freeze-reason .label {
  color: #8c8c8c;
}

.summary-item .value,
.freeze-reason .value {
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
