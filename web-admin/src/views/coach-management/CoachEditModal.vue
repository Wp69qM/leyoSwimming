<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue';
import type {
  AdminCoachAddRequest,
  AdminCoachUpdateRequest,
  AdminCoachDetail,
  CoachCertificateItem,
  AdminCoachCertificate,
} from '@/types/api';
import { getCoachDetail, addCoach, updateCoach } from '@/api/coachManagement';
import AvatarInput from '@/components/common/AvatarInput.vue';
import ImageInput from '@/components/common/ImageInput.vue';

const props = defineProps<{
  visible: boolean;
  coachId: number | null;
  isEdit: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success'): void;
}>();

const formRef = ref();
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const coach = ref<AdminCoachDetail | null>(null);

const swimStrokeOptions = ['蛙泳', '自由泳', '仰泳', '蝶泳'];

const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
});

const form = reactive({
  avatarUrl: '',
  phone: '',
  name: '',
  gender: 'MALE',
  age: 25,
  email: '',
  wechatQrUrl: '',
  idCardNo: '',
  teachingYears: 0,
  totalStudents: 0,
  totalHours: 0,
  teachingStrokes: [] as string[],
  bio: '',
  referencePrice: 300,
  portraitUrl: '',
  idCardFrontUrl: '',
  idCardBackUrl: '',
  healthCertUrl: '',
  coachCerts: [] as { imageUrl: string }[],
});

const rules = computed(() => ({
  name: [
    { required: true, message: '姓名不能为空', trigger: 'blur' },
    { max: 64, message: '姓名不能超过 64 个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '手机号不能为空', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  gender: [{ required: true, message: '性别不能为空', trigger: 'change' }],
  age: [
    { required: true, message: '年龄不能为空', trigger: 'blur' },
    {
      type: 'integer' as const,
      min: 18,
      max: 80,
      message: '年龄需在 18-80 岁之间',
      trigger: 'blur',
    },
  ],
  email: [
    { required: true, message: '邮箱不能为空', trigger: 'blur' },
    { type: 'email' as const, message: '邮箱格式不正确', trigger: 'blur' },
    { max: 128, message: '邮箱不能超过 128 个字符', trigger: 'blur' },
  ],
  idCardNo: [
    { required: true, message: '身份证号不能为空', trigger: 'blur' },
    {
      pattern: /^\d{17}[\dXx]$/,
      message: '请输入 18 位有效身份证号',
      trigger: 'blur',
    },
  ],
  teachingYears: [
    { required: true, message: '任教年限不能为空', trigger: 'blur' },
    {
      type: 'integer' as const,
      min: 0,
      max: 60,
      message: '任教年限需在 0-60 之间',
      trigger: 'blur',
    },
  ],
  totalStudents: [
    { required: true, message: '总学员数不能为空', trigger: 'blur' },
    {
      type: 'integer' as const,
      min: 0,
      max: 99999,
      message: '总学员数需在 0-99999 之间',
      trigger: 'blur',
    },
  ],
  totalHours: [
    { required: true, message: '总课时数不能为空', trigger: 'blur' },
    {
      type: 'integer' as const,
      min: 0,
      max: 99999,
      message: '总课时数需在 0-99999 之间',
      trigger: 'blur',
    },
  ],
  teachingStrokes: [
    {
      required: true,
      type: 'array' as const,
      min: 1,
      message: '请至少选择一种擅长泳姿',
      trigger: 'change',
    },
  ],
  bio: [
    { required: true, message: '个人简介不能为空', trigger: 'blur' },
    {
      min: 1,
      max: 500,
      message: '个人简介需在 1-500 个字符之间',
      trigger: 'blur',
    },
  ],
  referencePrice: [
    { required: true, message: '参考单价不能为空', trigger: 'blur' },
    {
      type: 'number' as const,
      min: 50,
      max: 2000,
      message: '参考单价需在 50-2000 元之间',
      trigger: 'blur',
    },
  ],
  portraitUrl: [
    { required: true, message: '请上传个人形象照', trigger: 'change' },
  ],
  idCardFrontUrl: [
    { required: true, message: '请上传身份证正面照', trigger: 'change' },
  ],
  idCardBackUrl: [
    { required: true, message: '请上传身份证反面照', trigger: 'change' },
  ],
  healthCertUrl: [
    { required: true, message: '请上传健康证', trigger: 'change' },
  ],
  coachCerts: [
    {
      validator: (_: unknown, value: { imageUrl: string }[]) => {
        if (
          !value ||
          value.length === 0 ||
          value.every((item) => !item.imageUrl)
        ) {
          return new Error('请上传至少一张教练资格证');
        }
        return true;
      },
      trigger: 'change',
    },
  ],
}));

function resetForm() {
  coach.value = null;
  error.value = '';
  form.avatarUrl = '';
  form.phone = '';
  form.name = '';
  form.gender = 'MALE';
  form.age = 25;
  form.email = '';
  form.wechatQrUrl = '';
  form.idCardNo = '';
  form.teachingYears = 0;
  form.totalStudents = 0;
  form.totalHours = 0;
  form.teachingStrokes = [];
  form.bio = '';
  form.referencePrice = 300;
  form.portraitUrl = '';
  form.idCardFrontUrl = '';
  form.idCardBackUrl = '';
  form.healthCertUrl = '';
  form.coachCerts = [{ imageUrl: '' }];
  formRef.value?.resetFields();
}

function findCertUrl(
  certificates: AdminCoachCertificate[] | undefined,
  certType: string
): string {
  if (!certificates) return '';
  const cert = certificates.find((item) => item.certType === certType);
  return cert?.imageUrl || '';
}

function findCoachCerts(
  certificates: AdminCoachCertificate[] | undefined
): { imageUrl: string }[] {
  if (!certificates) return [{ imageUrl: '' }];
  const items = certificates
    .filter((item) => item.certType === 'COACH_CERT')
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map((item) => ({ imageUrl: item.imageUrl }));
  return items.length > 0 ? items : [{ imageUrl: '' }];
}

function fillFromCoach(data: AdminCoachDetail) {
  coach.value = data;
  form.avatarUrl = data.avatarUrl || '';
  form.phone = data.phone || '';
  form.name = data.name || '';
  form.gender = data.gender || 'MALE';
  form.age = data.age ?? 25;
  form.email = data.email || '';
  form.wechatQrUrl = data.wechatQrUrl || '';
  form.idCardNo = data.idCardNo || '';
  form.teachingYears = data.teachingYears ?? 0;
  form.totalStudents = data.totalStudents ?? 0;
  form.totalHours = data.totalHours ?? 0;
  form.teachingStrokes = data.teachingStrokes
    ? data.teachingStrokes.split(/[,，]/).filter(Boolean)
    : [];
  form.bio = data.bio || '';
  form.referencePrice = data.referencePrice ?? 300;
  form.portraitUrl = findCertUrl(data.certificates, 'PORTRAIT');
  form.idCardFrontUrl = findCertUrl(data.certificates, 'ID_CARD_FRONT');
  form.idCardBackUrl = findCertUrl(data.certificates, 'ID_CARD_BACK');
  form.healthCertUrl = findCertUrl(data.certificates, 'HEALTH_CERT');
  form.coachCerts = findCoachCerts(data.certificates);
}

async function fetchDetail() {
  if (!props.coachId) return;
  loading.value = true;
  error.value = '';
  try {
    const res = await getCoachDetail({ coachId: props.coachId });
    if (res.data) {
      fillFromCoach(res.data);
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载教练资料失败';
  } finally {
    loading.value = false;
  }
}

function buildCertificates(): CoachCertificateItem[] {
  const certs: CoachCertificateItem[] = [];
  if (form.portraitUrl)
    certs.push({ certType: 'PORTRAIT', imageUrl: form.portraitUrl });
  if (form.idCardFrontUrl)
    certs.push({ certType: 'ID_CARD_FRONT', imageUrl: form.idCardFrontUrl });
  if (form.idCardBackUrl)
    certs.push({ certType: 'ID_CARD_BACK', imageUrl: form.idCardBackUrl });
  if (form.healthCertUrl)
    certs.push({ certType: 'HEALTH_CERT', imageUrl: form.healthCertUrl });
  form.coachCerts
    .filter((item) => item.imageUrl)
    .forEach((item) =>
      certs.push({ certType: 'COACH_CERT', imageUrl: item.imageUrl })
    );
  return certs;
}

function addCoachCert() {
  form.coachCerts.push({ imageUrl: '' });
}

function removeCoachCert(index: number) {
  form.coachCerts.splice(index, 1);
  if (form.coachCerts.length === 0) {
    form.coachCerts.push({ imageUrl: '' });
  }
}

function handleClose() {
  emit('update:visible', false);
}

async function handleSubmit() {
  error.value = '';
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  const certificates = buildCertificates();
  if (certificates.length === 0) {
    error.value = '请上传身份证正反面、教练资格证、健康证和个人形象照';
    return;
  }

  saving.value = true;
  try {
    const basePayload = {
      avatarUrl: form.avatarUrl || undefined,
      phone: form.phone.trim(),
      name: form.name.trim(),
      gender: form.gender,
      age: form.age,
      email: form.email.trim(),
      wechatQrUrl: form.wechatQrUrl || undefined,
      idCardNo: form.idCardNo.trim(),
      teachingYears: form.teachingYears,
      totalStudents: form.totalStudents,
      totalHours: form.totalHours,
      teachingStrokes: form.teachingStrokes,
      bio: form.bio.trim(),
      referencePrice: form.referencePrice,
      certificates,
    };

    if (props.isEdit && props.coachId) {
      const payload: AdminCoachUpdateRequest = {
        ...basePayload,
        coachId: props.coachId,
        version: coach.value?.version ?? 0,
      };
      await updateCoach(payload);
    } else {
      const payload: AdminCoachAddRequest = basePayload;
      await addCoach(payload);
    }
    emit('success');
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败';
  } finally {
    saving.value = false;
  }
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      resetForm();
      if (props.isEdit && props.coachId) {
        fetchDetail();
      }
    }
  }
);
</script>

<template>
  <el-dialog
    v-model="localVisible"
    :title="props.isEdit ? '编辑教练资料' : '新建教练'"
    width="720px"
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

    <el-skeleton v-if="loading" :rows="5" animated />

    <el-form
      v-else
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="coach-form"
    >
      <div class="section-title">基础信息</div>

      <el-form-item label="个人形象照" prop="portraitUrl">
        <ImageInput
          v-model="form.portraitUrl"
          placeholder="形象照"
          :width="80"
          :height="80"
        />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="姓名" prop="name">
            <el-input
              v-model="form.name"
              placeholder="请输入姓名"
              maxlength="64"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号" prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              maxlength="11"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio label="MALE">男</el-radio>
              <el-radio label="FEMALE">女</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="年龄" prop="age">
            <el-input-number
              v-model="form.age"
              :min="18"
              :max="80"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
              maxlength="128"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="参考单价（元/节）" prop="referencePrice">
            <el-input-number
              v-model="form.referencePrice"
              :min="50"
              :max="2000"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="头像">
        <AvatarInput v-model="form.avatarUrl" />
      </el-form-item>

      <el-form-item label="微信二维码">
        <ImageInput v-model="form.wechatQrUrl" placeholder="二维码" />
      </el-form-item>

      <div class="section-title">实名与资质</div>

      <el-form-item label="身份证号" prop="idCardNo">
        <el-input
          v-model="form.idCardNo"
          placeholder="请输入身份证号"
          maxlength="18"
          style="width: 240px"
        />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="身份证正面照" prop="idCardFrontUrl">
            <ImageInput v-model="form.idCardFrontUrl" placeholder="正面照" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="身份证反面照" prop="idCardBackUrl">
            <ImageInput v-model="form.idCardBackUrl" placeholder="反面照" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="健康证" prop="healthCertUrl">
        <ImageInput v-model="form.healthCertUrl" placeholder="健康证" />
      </el-form-item>

      <el-form-item label="教练资格证" prop="coachCerts">
        <div class="cert-list">
          <div
            v-for="(item, index) in form.coachCerts"
            :key="index"
            class="cert-item"
          >
            <ImageInput
              v-model="item.imageUrl"
              :placeholder="`资格证 ${index + 1}`"
            />
            <el-button
              v-if="form.coachCerts.length > 1"
              link
              type="danger"
              @click="removeCoachCert(index)"
            >
              删除
            </el-button>
          </div>
          <el-button
            v-if="form.coachCerts.length < 5"
            link
            type="primary"
            @click="addCoachCert"
          >
            + 添加资格证
          </el-button>
        </div>
      </el-form-item>

      <div class="section-title">教学履历</div>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="任教年限" prop="teachingYears">
            <el-input-number
              v-model="form.teachingYears"
              :min="0"
              :max="60"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="总学员数" prop="totalStudents">
            <el-input-number
              v-model="form.totalStudents"
              :min="0"
              :max="99999"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="总课时数" prop="totalHours">
            <el-input-number
              v-model="form.totalHours"
              :min="0"
              :max="99999"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="擅长泳姿" prop="teachingStrokes">
        <el-checkbox-group v-model="form.teachingStrokes">
          <el-checkbox
            v-for="stroke in swimStrokeOptions"
            :key="stroke"
            :label="stroke"
          >
            {{ stroke }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="个人简介" prop="bio">
        <el-input
          v-model="form.bio"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入个人简介"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          {{ props.isEdit ? '保存' : '创建' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.coach-form {
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

.cert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cert-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
