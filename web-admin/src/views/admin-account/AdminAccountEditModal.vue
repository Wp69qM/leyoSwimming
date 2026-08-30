<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue';
import type {
  AdminAccountUpdateRequest,
  AdminAccountDetail,
} from '@/types/api';
import {
  getAdminAccountDetail,
  updateAdminAccount,
} from '@/api/adminAccountManagement';
import { EditPen } from '@element-plus/icons-vue';

const props = defineProps<{
  visible: boolean;
  adminId: number | null;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success', admin: AdminAccountDetail): void;
}>();

const formRef = ref();
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const admin = ref<AdminAccountDetail | null>(null);

const form = reactive({
  name: '',
  role: 'admin',
  version: 0,
});

const rules = computed(() => ({
  name: [
    { required: true, message: '姓名不能为空', trigger: 'blur' },
    {
      min: 2,
      max: 32,
      message: '姓名长度需在 2-32 个字符之间',
      trigger: 'blur',
    },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}));

const roleOptions = [
  { label: '超级管理员', value: 'super_admin' },
  { label: '普通管理员', value: 'admin' },
];

const isChanged = computed(() => {
  if (!admin.value) return false;
  return form.name !== admin.value.name || form.role !== admin.value.role;
});

const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
});

function resetForm() {
  admin.value = null;
  error.value = '';
  form.name = '';
  form.role = 'admin';
  form.version = 0;
  formRef.value?.resetFields();
}

function fillFromAdmin(data: AdminAccountDetail) {
  admin.value = data;
  form.name = data.name || '';
  form.role = data.role || 'admin';
  form.version = data.version ?? 0;
}

async function fetchDetail() {
  if (!props.adminId) return;
  loading.value = true;
  error.value = '';
  try {
    const res = await getAdminAccountDetail({ adminId: props.adminId });
    if (res.data) {
      fillFromAdmin(res.data);
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载管理员资料失败';
  } finally {
    loading.value = false;
  }
}

function handleClose() {
  emit('update:visible', false);
}

async function handleSubmit() {
  error.value = '';
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  saving.value = true;
  try {
    const payload: AdminAccountUpdateRequest = {
      adminId: props.adminId!,
      version: form.version,
    };
    if (form.name !== admin.value?.name) {
      payload.name = form.name.trim();
    }
    if (form.role !== admin.value?.role) {
      payload.role = form.role;
    }
    const res = await updateAdminAccount(payload);
    if (res.data) {
      emit('success', res.data);
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '更新失败';
  } finally {
    saving.value = false;
  }
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      resetForm();
      fetchDetail();
    }
  }
);
</script>

<template>
  <el-dialog
    v-model="localVisible"
    width="480px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <template #header>
      <div class="dialog-header">
        <el-icon class="dialog-header-icon" :size="18"><EditPen /></el-icon>
        <div class="dialog-header-text">
          <div class="dialog-title">编辑管理员</div>
          <div v-if="admin" class="dialog-subtitle">
            账号：{{ admin.username }} · {{ admin.name || '未设置' }}
          </div>
        </div>
      </div>
    </template>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="form-error"
    />

    <el-skeleton v-if="loading" :rows="3" animated />

    <el-form
      v-else
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      class="admin-form"
    >
      <el-form-item label="登录账号">
        <el-input :value="admin?.username" disabled style="width: 280px" />
      </el-form-item>

      <el-form-item label="姓名" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入姓名"
          maxlength="32"
          style="width: 280px"
        />
      </el-form-item>

      <el-form-item label="角色" prop="role">
        <el-radio-group v-model="form.role">
          <el-radio
            v-for="option in roleOptions"
            :key="option.value"
            :label="option.value"
          >
            {{ option.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="!isChanged"
          @click="handleSubmit"
        >
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dialog-header-icon {
  color: #1890ff;
}

.dialog-header-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dialog-title {
  font-size: 18px;
  font-weight: 500;
  color: #262626;
  line-height: 1.2;
}

.dialog-subtitle {
  font-size: 13px;
  color: #86909c;
  line-height: 1.2;
}

.admin-form {
  max-height: 60vh;
  padding: 24px 24px 8px;
  overflow-y: auto;
}

.form-error {
  margin: 16px 24px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
