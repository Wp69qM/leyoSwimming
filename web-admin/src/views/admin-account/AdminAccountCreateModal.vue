<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue';
import { View, Hide } from '@element-plus/icons-vue';
import type { AdminAccountAddRequest } from '@/types/api';
import { addAdminAccount } from '@/api/adminAccountManagement';

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success'): void;
}>();

const formRef = ref();
const loading = ref(false);
const error = ref('');
const passwordVisible = ref(false);
const confirmPasswordVisible = ref(false);

const form = reactive({
  name: '',
  username: '',
  password: '',
  confirmPassword: '',
  role: 'admin',
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
  username: [
    { required: true, message: '登录账号不能为空', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]{3,32}$/,
      message: '登录账号需为 3-32 位字母、数字或下划线',
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '初始密码不能为空', trigger: 'blur' },
    {
      pattern:
        /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&_#^])[A-Za-z\d@$!%*?&_#^]{8,32}$/,
      message: '密码需 8-32 位，且包含字母、数字和特殊字符',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_: unknown, value: string) => {
        if (value !== form.password) {
          return new Error('两次输入的密码不一致');
        }
        return true;
      },
      trigger: 'blur',
    },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}));

const roleOptions = [
  { label: '超级管理员', value: 'super_admin' },
  { label: '普通管理员', value: 'admin' },
];

const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
});

function resetForm() {
  form.name = '';
  form.username = '';
  form.password = '';
  form.confirmPassword = '';
  form.role = 'admin';
  error.value = '';
  passwordVisible.value = false;
  confirmPasswordVisible.value = false;
  formRef.value?.resetFields();
}

function handleClose() {
  emit('update:visible', false);
}

async function handleSubmit() {
  error.value = '';
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    const payload: AdminAccountAddRequest = {
      name: form.name.trim(),
      username: form.username.trim(),
      password: form.password,
      role: form.role,
    };
    await addAdminAccount(payload);
    emit('success');
    resetForm();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '创建失败';
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      resetForm();
    }
  }
);
</script>

<template>
  <el-dialog
    v-model="localVisible"
    title="新建管理员"
    width="480px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="form-error"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      class="admin-form"
    >
      <el-form-item label="姓名" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入姓名"
          maxlength="32"
          style="width: 280px"
        />
      </el-form-item>

      <el-form-item label="登录账号" prop="username">
        <el-input
          v-model="form.username"
          placeholder="请输入登录账号"
          maxlength="32"
          style="width: 280px"
        />
      </el-form-item>

      <el-form-item label="初始密码" prop="password">
        <el-input
          v-model="form.password"
          :type="passwordVisible ? 'text' : 'password'"
          placeholder="请输入初始密码"
          maxlength="32"
          style="width: 280px"
        >
          <template #suffix>
            <el-icon
              class="password-eye"
              @click="passwordVisible = !passwordVisible"
            >
              <View v-if="passwordVisible" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          :type="confirmPasswordVisible ? 'text' : 'password'"
          placeholder="请再次输入密码"
          maxlength="32"
          style="width: 280px"
        >
          <template #suffix>
            <el-icon
              class="password-eye"
              @click="confirmPasswordVisible = !confirmPasswordVisible"
            >
              <View v-if="confirmPasswordVisible" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
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
        <el-button type="primary" :loading="loading" @click="handleSubmit"
          >保存</el-button
        >
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
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

.password-eye {
  cursor: pointer;
  color: #8c8c8c;
}
</style>
