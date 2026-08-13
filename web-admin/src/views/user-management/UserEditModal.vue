<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue';
import type { AdminUserUpdateRequest, AdminUserDetail } from '@/types/api';
import { getUserDetail, updateUser } from '@/api/userManagement';

const props = defineProps<{
  visible: boolean;
  userId: number | null;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success'): void;
}>();

const formRef = ref();
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const user = ref<AdminUserDetail | null>(null);

const form = reactive<AdminUserUpdateRequest & { phoneDisplay: string }>({
  userId: 0,
  avatarUrl: '',
  phoneDisplay: '',
  name: '',
  gender: 1,
  age: 0,
  hasSwimBasis: false,
  swimStrokes: [],
  swimYears: 0,
  personalDesc: '',
  guardianName: '',
  guardianPhone: '',
  version: 0,
});

const isMinor = computed(() => (form.age ?? 0) < 18);

const rules = computed(() => ({
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

const swimStrokeOptions = ['蛙泳', '自由泳', '仰泳', '蝶泳'];

const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
});

function resetForm() {
  user.value = null;
  error.value = '';
  form.userId = 0;
  form.avatarUrl = '';
  form.phoneDisplay = '';
  form.name = '';
  form.gender = 1;
  form.age = 0;
  form.hasSwimBasis = false;
  form.swimStrokes = [];
  form.swimYears = 0;
  form.personalDesc = '';
  form.guardianName = '';
  form.guardianPhone = '';
  form.version = 0;
  formRef.value?.resetFields();
}

function fillFromUser(data: AdminUserDetail) {
  user.value = data;
  form.userId = data.userId;
  form.avatarUrl = data.avatarUrl || '';
  form.phoneDisplay = data.phone || '';
  form.name = data.name || '';
  form.gender = data.gender ?? 1;
  form.age = data.age ?? 0;
  form.hasSwimBasis = !!data.hasSwimBasis;
  form.swimStrokes = data.swimStrokes || [];
  form.swimYears = data.swimYears ?? 0;
  form.personalDesc = data.personalDesc || '';
  form.guardianName = data.guardianName || '';
  form.guardianPhone = data.guardianPhone || '';
  form.version = data.version;
}

async function fetchDetail() {
  if (!props.userId) return;
  loading.value = true;
  error.value = '';
  try {
    const res = await getUserDetail({ userId: props.userId });
    if (res.data) {
      fillFromUser(res.data);
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载用户资料失败';
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
    const payload: AdminUserUpdateRequest = {
      userId: form.userId,
      avatarUrl: form.avatarUrl || undefined,
      name: form.name.trim(),
      gender: form.gender,
      age: form.age,
      hasSwimBasis: form.hasSwimBasis,
      swimStrokes: form.hasSwimBasis ? form.swimStrokes : undefined,
      swimYears: form.hasSwimBasis ? form.swimYears : undefined,
      personalDesc: form.personalDesc || undefined,
      guardianName: isMinor.value ? form.guardianName.trim() : undefined,
      guardianPhone: isMinor.value ? form.guardianPhone.trim() : undefined,
      version: form.version,
    };
    await updateUser(payload);
    emit('success');
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
    title="编辑用户资料"
    width="560px"
    :close-on-click-modal="false"
    destroy-on-close
    class="user-edit-dialog"
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

    <el-skeleton v-if="loading" :rows="4" animated />

    <el-form
      v-else
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

      <el-form-item label="手机号">
        <el-input v-model="form.phoneDisplay" disabled style="width: 240px" />
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
              v-for="stroke in swimStrokeOptions"
              :key="stroke"
              :label="stroke"
            >
              {{ stroke }}
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
        <el-button type="primary" :loading="saving" @click="handleSubmit">
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

:deep(.user-edit-dialog .el-dialog__header) {
  padding: 0 24px;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.user-edit-dialog .el-dialog__headerbtn) {
  width: 32px;
  height: 32px;
}

:deep(.user-edit-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.user-edit-dialog .el-dialog__footer) {
  padding: 0 24px;
  border-top: 1px solid #e4e7ed;
}
</style>
