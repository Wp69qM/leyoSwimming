<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { DocumentCopy, CircleCheck } from '@element-plus/icons-vue';

const props = defineProps<{
  visible: boolean;
  tempPassword: string;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
}>();

const copied = ref(false);
let copyTimeout: ReturnType<typeof setTimeout> | null = null;

const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
});

onUnmounted(() => {
  if (copyTimeout) {
    clearTimeout(copyTimeout);
    copyTimeout = null;
  }
});

function handleClose() {
  emit('update:visible', false);
}

async function handleCopy() {
  if (!props.tempPassword) return;
  try {
    await navigator.clipboard.writeText(props.tempPassword);
    copied.value = true;
    ElMessage.success('已复制到剪贴板');
    if (copyTimeout) clearTimeout(copyTimeout);
    copyTimeout = setTimeout(() => {
      copied.value = false;
    }, 2000);
  } catch {
    ElMessage.error('复制失败，请手动复制');
  }
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      copied.value = false;
    }
  }
);
</script>

<template>
  <el-dialog
    v-model="localVisible"
    title="重置密码成功"
    width="400px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <div class="reset-content">
      <el-icon class="success-icon" :size="48" color="#52C41A"
        ><CircleCheck
      /></el-icon>
      <div class="main-text">新密码已生成，请妥善保存</div>
      <div class="warning-text">密码仅显示一次，关闭后将无法再次查看</div>

      <div class="password-wrap">
        <el-input :model-value="tempPassword" readonly class="password-input" />
        <el-button type="primary" :icon="DocumentCopy" @click="handleCopy">
          {{ copied ? '已复制' : '复制' }}
        </el-button>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.reset-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  text-align: center;
}

.success-icon {
  margin-bottom: 16px;
}

.main-text {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.warning-text {
  margin-bottom: 24px;
  font-size: 14px;
  color: #ff4d4f;
}

.password-wrap {
  display: flex;
  gap: 8px;
  width: 100%;
}

.password-input {
  flex: 1;

  :deep(.el-input__inner) {
    font-family: monospace;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
