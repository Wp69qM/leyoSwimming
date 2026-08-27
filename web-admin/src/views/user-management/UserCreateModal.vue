<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue';

import type { AdminUserAddRequest, AdminUserDetail } from '@/types/api';
import { addUser } from '@/api/userManagement';
import { SWIM_STROKE_OPTIONS } from '@/constants/swimStrokes';

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success', user: AdminUserDetail): void;
}>();

const formRef = ref();
const loading = ref(false);
const error = ref('');

const form = reactive<AdminUserAddRequest>({
  avatarUrl: '',
  phone: '',
  name: '',
  gender: 1,
  age: undefined as unknown as number,
  hasSwimBasis: false,
  swimStrokes: [],
  swimYears: undefined as unknown as number,
  personalDesc: '',
  guardianName: '',
  guardianPhone: '',
});

const isMinor = computed(() => (form.age ?? 0) < 18);

const rules = computed(() => ({
  phone: [
    { required: true, message: '手机号不能为空', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '姓名不能为空', trigger: 'blur' },
    { max: 64, message: '姓名不能超过 64 个字符', trigger: 'blur' },
  ],
  gender: [{ required: true, message: '性别不能为空', trigger: 'change' }],
  age: [
    { required: true, message: '年龄不能为空', trigger: 'blur' },
    {
      type: 'integer' as const,
      min: 0,
      max: 150,
      message: '年龄需在 0-150 岁之间',
      trigger: 'blur',
    },
  ],
  swimStrokes: [
    {
      validator: (_: unknown, value: string[]) => {
        if (form.hasSwimBasis && (!value || value.length === 0)) {
          return new Error('请选择至少一种泳姿');
        }
        return true;
      },
      trigger: 'change',
    },
  ],
  swimYears: [
    {
      validator: (_: unknown, value: number) => {
        if (form.hasSwimBasis && (value == null || value < 0)) {
          return new Error('请输入游泳年限');
        }
        return true;
      },
      trigger: 'blur',
    },
  ],
  guardianName: [
    {
      validator: (_: unknown, value: string) => {
        if (isMinor.value && !value) {
          return new Error('监护人姓名不能为空');
        }
        return true;
      },
      trigger: 'blur',
    },
  ],
  guardianPhone: [
    {
      validator: (_: unknown, value: string) => {
        if (isMinor.value) {
          if (!value) return new Error('监护人手机号不能为空');
          if (!/^1[3-9]\d{9}$/.test(value))
            return new Error('监护人手机号格式不正确');
        }
        return true;
      },
      trigger: 'blur',
    },
  ],
}));

const canSubmit = computed(() => {
  if (!form.phone || !/^1[3-9]\d{9}$/.test(form.phone)) return false;
  if (
    !form.name ||
    form.name.trim().length === 0 ||
    form.name.trim().length > 64
  )
    return false;
  if (!form.gender) return false;
  if (form.age == null || form.age < 0 || form.age > 150) return false;
  if (form.hasSwimBasis) {
    if (!form.swimStrokes || form.swimStrokes.length === 0) return false;
    if (form.swimYears == null || form.swimYears < 0) return false;
  }
  if (isMinor.value) {
    if (!form.guardianName || form.guardianName.trim().length === 0)
      return false;
    if (!form.guardianPhone || !/^1[3-9]\d{9}$/.test(form.guardianPhone))
      return false;
  }
  return true;
});

const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
});

function resetForm() {
  form.avatarUrl = '';
  form.phone = '';
  form.name = '';
  form.gender = 1;
  form.age = undefined as unknown as number;
  form.hasSwimBasis = false;
  form.swimStrokes = [];
  form.swimYears = undefined as unknown as number;
  form.personalDesc = '';
  form.guardianName = '';
  form.guardianPhone = '';
  error.value = '';
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
    const payload: AdminUserAddRequest = {
      avatarUrl: form.avatarUrl || undefined,
      phone: form.phone.trim(),
      name: form.name.trim(),
      gender: form.gender,
      age: form.age,
      hasSwimBasis: form.hasSwimBasis,
      swimStrokes: form.hasSwimBasis ? form.swimStrokes : undefined,
      swimYears: form.hasSwimBasis ? form.swimYears : undefined,
      personalDesc: form.personalDesc || undefined,
      guardianName: isMinor.value ? form.guardianName.trim() : undefined,
      guardianPhone: isMinor.value ? form.guardianPhone.trim() : undefined,
    };
    const res = await addUser(payload);
    if (res.data) {
      emit('success', res.data);
      resetForm();
    }
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
    title="新建用户"
    width="560px"
    :close-on-click-modal="false"
    destroy-on-close
    class="user-create-dialog"
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
      label-position="top"
      class="user-form"
    >
      <div class="section-title">基础信息</div>

      <el-form-item label="头像">
        <AvatarInput v-model="form.avatarUrl" />
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="form.phone"
          placeholder="请输入手机号"
          maxlength="11"
          style="width: 240px"
        />
      </el-form-item>

      <el-form-item label="昵称/姓名" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入昵称/姓名"
          maxlength="64"
        />
      </el-form-item>

      <el-form-item label="性别" prop="gender">
        <el-radio-group v-model="form.gender">
          <el-radio :label="1">男</el-radio>
          <el-radio :label="2">女</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="年龄" prop="age">
        <el-input-number
          v-model="form.age"
          :min="0"
          :max="150"
          controls-position="right"
        />
      </el-form-item>

      <div class="section-title">游泳档案</div>

      <el-form-item label="有无游泳基础">
        <el-switch
          v-model="form.hasSwimBasis"
          active-text="有"
          inactive-text="无"
        />
      </el-form-item>

      <template v-if="form.hasSwimBasis">
        <el-form-item label="会什么泳姿" prop="swimStrokes">
          <el-checkbox-group v-model="form.swimStrokes">
            <el-checkbox
              v-for="option in SWIM_STROKE_OPTIONS"
              :key="option.code"
              :label="option.code"
            >
              {{ option.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="游泳年限" prop="swimYears">
          <el-input-number
            v-model="form.swimYears"
            :min="0"
            controls-position="right"
          />
        </el-form-item>
      </template>

      <el-form-item label="个人描述">
        <el-input
          v-model="form.personalDesc"
          type="textarea"
          :rows="3"
          maxlength="512"
          show-word-limit
          placeholder="请输入个人描述"
        />
      </el-form-item>

      <template v-if="isMinor">
        <div class="section-title">监护人信息</div>

        <el-form-item label="监护人姓名" prop="guardianName">
          <el-input
            v-model="form.guardianName"
            placeholder="请输入监护人姓名"
          />
        </el-form-item>

        <el-form-item label="监护人手机号" prop="guardianPhone">
          <el-input
            v-model="form.guardianPhone"
            placeholder="请输入监护人手机号"
            maxlength="11"
          />
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :loading="loading"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.user-form {
  max-height: 60vh;
  padding-right: 8px;
  overflow-y: auto;
}

.section-title {
  margin: 24px 0 16px;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.section-title:first-child {
  margin-top: 0;
}

.form-error {
  margin-bottom: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.user-create-dialog .el-dialog__header) {
  padding: 0 24px;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.user-create-dialog .el-dialog__headerbtn) {
  width: 32px;
  height: 32px;
}

:deep(.user-create-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.user-create-dialog .el-dialog__footer) {
  padding: 0 24px;
  border-top: 1px solid #e4e7ed;
}
</style>
