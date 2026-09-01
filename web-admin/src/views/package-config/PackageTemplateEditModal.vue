<script setup lang="ts">
import { ref, reactive, watch, computed, nextTick } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import type {
  AdminPackageTemplateDetail,
  AdminPackageTemplateAddRequest,
  AdminPackageTemplateUpdateRequest,
  AdminCoachListItem,
} from '@/types/api';
import {
  getPackageTemplateDetail,
  addPackageTemplate,
  updatePackageTemplate,
} from '@/api/packageTemplate';
import { getCoachList } from '@/api/coachManagement';
import { uploadFile } from '@/api/upload';
import { generateUUID } from '@leyo/shared';
import { Plus } from '@element-plus/icons-vue';

interface Props {
  visible: boolean;
  mode: 'add' | 'edit' | 'view';
  templateId?: number;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success'): void;
}>();

const MODE_OPTIONS = [
  { label: '正价套餐', value: 'standard' },
  { label: '体验课', value: 'experience' },
];

const TEACHING_TYPE_OPTIONS = [
  { label: '一对一', value: 'one_on_one' },
  { label: '一对二', value: 'one_on_two' },
  { label: '一对三', value: 'one_on_three' },
];

const STROKE_OPTIONS = [
  { label: '蛙泳', value: 1 },
  { label: '自由泳', value: 2 },
  { label: '仰泳', value: 3 },
  { label: '蝶泳', value: 4 },
];

const formRef = ref<FormInstance>();
const loading = ref(false);
const saving = ref(false);
const coachOptions = ref<AdminCoachListItem[]>([]);
const detailVersion = ref(0);

const form = reactive({
  name: '',
  packageMode: 'standard' as 'standard' | 'experience',
  coachIds: [] as number[],
  teachingType: 'one_on_one',
  strokeIds: [] as number[],
  totalHours: undefined as number | undefined,
  durationMinutes: undefined as number | undefined,
  validDays: undefined as number | undefined,
  originalPrice: undefined as number | undefined,
  price: undefined as number | undefined,
  refundEnabled: false,
  refundRatio: undefined as number | undefined,
  refundValidDays: undefined as number | undefined,
  tags: [] as string[],
  description: '',
  images: [] as string[],
  status: 'inactive' as 'active' | 'inactive',
});

const dialogTitle = computed(() => {
  if (props.mode === 'add') return '新增标准套餐';
  if (props.mode === 'view') return '查看标准套餐';
  return '编辑标准套餐';
});

const isReadOnly = computed(() => {
  return props.mode === 'view' || form.status === 'active';
});

const rules: FormRules = {
  name: [
    { required: true, message: '套餐名称不能为空', trigger: 'blur' },
    { max: 64, message: '套餐名称不能超过 64 个字符', trigger: 'blur' },
  ],
  packageMode: [
    { required: true, message: '请选择套餐模式', trigger: 'change' },
  ],
  coachIds: [
    { required: true, message: '请至少选择 1 名适用教练', trigger: 'change' },
  ],
  teachingType: [
    { required: true, message: '请选择教学类型', trigger: 'change' },
  ],
  strokeIds: [
    { required: true, message: '请至少选择 1 项泳姿', trigger: 'change' },
  ],
  totalHours: [
    { required: true, message: '请输入课时数', trigger: 'blur' },
    {
      type: 'integer',
      min: 1,
      max: 100,
      message: '课时数需在 1-100 之间',
      trigger: 'blur',
    },
  ],
  durationMinutes: [
    { required: true, message: '请输入每节课时长', trigger: 'blur' },
    {
      type: 'integer',
      min: 1,
      message: '每节课时长需大于 0',
      trigger: 'blur',
    },
  ],
  validDays: [
    { required: true, message: '请输入有效期', trigger: 'blur' },
    {
      type: 'integer',
      min: 1,
      message: '有效期需大于 0 天',
      trigger: 'blur',
    },
  ],
  originalPrice: [
    { required: true, message: '请输入原价', trigger: 'blur' },
    {
      type: 'number',
      min: 0,
      message: '原价不能为负数',
      trigger: 'blur',
    },
  ],
  price: [
    { required: true, message: '请输入售价', trigger: 'blur' },
    {
      type: 'number',
      min: 0,
      message: '售价不能为负数',
      trigger: 'blur',
    },
    {
      validator: (_rule, value: number, callback) => {
        if (
          value !== undefined &&
          form.originalPrice !== undefined &&
          value > form.originalPrice
        ) {
          callback(new Error('售价不能高于原价'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
  refundRatio: [
    {
      validator: (_rule, value: number, callback) => {
        if (form.refundEnabled) {
          if (value === undefined || value < 0 || value > 100) {
            callback(new Error('退款比例需在 0-100 之间'));
            return;
          }
        }
        callback();
      },
      trigger: 'blur',
    },
  ],
  refundValidDays: [
    {
      validator: (_rule, value: number, callback) => {
        if (form.refundEnabled) {
          if (value === undefined || value < 0) {
            callback(new Error('退款有效期不能为负数'));
            return;
          }
        }
        callback();
      },
      trigger: 'blur',
    },
  ],
};

function resetForm() {
  form.name = '';
  form.packageMode = 'standard';
  form.coachIds = [];
  form.teachingType = 'one_on_one';
  form.strokeIds = [];
  form.totalHours = undefined;
  form.durationMinutes = undefined;
  form.validDays = undefined;
  form.originalPrice = undefined;
  form.price = undefined;
  form.refundEnabled = false;
  form.refundRatio = undefined;
  form.refundValidDays = undefined;
  form.tags = [];
  form.description = '';
  form.images = [];
  form.status = 'inactive';
  detailVersion.value = 0;
  formRef.value?.clearValidate();
}

function fillForm(detail: AdminPackageTemplateDetail) {
  form.name = detail.name;
  form.packageMode = detail.packageMode;
  form.coachIds = detail.coachIds ?? [];
  form.teachingType = detail.teachingType;
  form.strokeIds = detail.strokeIds ?? [];
  form.totalHours = detail.totalHours;
  form.durationMinutes = detail.durationMinutes;
  form.validDays = detail.validDays;
  form.originalPrice = detail.originalPrice;
  form.price = detail.price;
  form.refundEnabled = detail.refundEnabled;
  form.refundRatio = detail.refundRatio !== undefined ? detail.refundRatio * 100 : undefined;
  form.refundValidDays = detail.refundValidDays;
  form.tags = detail.tags ?? [];
  form.description = detail.description ?? '';
  form.images = detail.images ?? [];
  form.status = detail.status;
  detailVersion.value = detail.version;
}

async function loadCoaches() {
  try {
    const res = await getCoachList({ page: 1, pageSize: 100 });
    coachOptions.value = res.data?.list ?? [];
  } catch (err) {
    coachOptions.value = [];
    ElMessage.error(err instanceof Error ? err.message : '加载教练列表失败');
  }
}

async function loadDetail() {
  if (!props.templateId) return;
  loading.value = true;
  try {
    const res = await getPackageTemplateDetail({
      packageTemplateId: props.templateId,
    });
    if (res.data) {
      fillForm(res.data);
    }
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载套餐详情失败');
    emit('update:visible', false);
  } finally {
    loading.value = false;
  }
}

function handleClose() {
  emit('update:visible', false);
}

function handleOpened() {
  resetForm();
  loadCoaches();
  if (props.mode !== 'add' && props.templateId) {
    loadDetail();
  }
}

function buildAddRequest(): AdminPackageTemplateAddRequest {
  return {
    name: form.name.trim(),
    packageMode: form.packageMode,
    coachIds: form.coachIds,
    teachingType: form.teachingType,
    strokeIds: form.strokeIds,
    totalHours: form.totalHours as number,
    durationMinutes: form.durationMinutes as number,
    validDays: form.validDays as number,
    originalPrice: form.originalPrice as number,
    price: form.price as number,
    refundEnabled: form.refundEnabled,
    refundRatio: form.refundEnabled && form.refundRatio !== undefined ? form.refundRatio / 100 : undefined,
    refundValidDays: form.refundEnabled ? form.refundValidDays : undefined,
    tags: form.tags.length > 0 ? form.tags : undefined,
    description: form.description || undefined,
    images: form.images.length > 0 ? form.images : undefined,
    idempotencyKey: generateUUID(),
  };
}

function buildUpdateRequest(): AdminPackageTemplateUpdateRequest {
  return {
    packageTemplateId: props.templateId as number,
    name: form.name.trim(),
    packageMode: form.packageMode,
    coachIds: form.coachIds,
    teachingType: form.teachingType,
    strokeIds: form.strokeIds,
    totalHours: form.totalHours as number,
    durationMinutes: form.durationMinutes as number,
    validDays: form.validDays as number,
    originalPrice: form.originalPrice as number,
    price: form.price as number,
    refundEnabled: form.refundEnabled,
    refundRatio: form.refundEnabled && form.refundRatio !== undefined ? form.refundRatio / 100 : undefined,
    refundValidDays: form.refundEnabled ? form.refundValidDays : undefined,
    tags: form.tags,
    description: form.description,
    images: form.images.length > 0 ? form.images : undefined,
    version: detailVersion.value,
  };
}

async function handleSave() {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) {
    ElMessage.warning('请检查表单填写是否正确');
    return;
  }

  saving.value = true;
  try {
    if (props.mode === 'add') {
      await addPackageTemplate(buildAddRequest());
      ElMessage.success('套餐已创建');
    } else {
      await updatePackageTemplate(buildUpdateRequest());
      ElMessage.success('套餐已更新');
    }
    emit('success');
    emit('update:visible', false);
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

const tagInputVisible = ref(false);
const tagInputValue = ref('');
const tagInputRef = ref<HTMLInputElement>();

function handleTagClose(tag: string) {
  form.tags = form.tags.filter((t) => t !== tag);
}

function showTagInput() {
  tagInputVisible.value = true;
  nextTick(() => tagInputRef.value?.focus());
}

function handleTagInputConfirm() {
  const value = tagInputValue.value.trim();
  if (value && !form.tags.includes(value)) {
    form.tags = [...form.tags, value];
  }
  tagInputVisible.value = false;
  tagInputValue.value = '';
}

function removeImage(index: number) {
  form.images = form.images.filter((_, i) => i !== index);
}

async function handleImageUpload(options: { file: File }) {
  const file = options.file;
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件');
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB');
    return;
  }

  try {
    const url = await uploadFile(file);
    form.images = [...form.images, url];
    ElMessage.success('图片上传成功');
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '图片上传失败');
  }
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      handleOpened();
    }
  }
);
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="720px"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
    @opened="handleOpened"
    @closed="handleClose"
  >
    <el-alert
      v-if="form.status === 'active' && mode !== 'view'"
      title="已上架套餐需先下架才能编辑"
      type="warning"
      :closable="false"
      class="active-warning"
    />

    <el-skeleton v-if="loading" :rows="6" animated />

    <el-form
      v-else
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      class="template-form"
    >
      <el-form-item label="套餐名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入套餐名称"
          maxlength="64"
          show-word-limit
          :disabled="isReadOnly"
        />
      </el-form-item>

      <el-form-item label="套餐模式" prop="packageMode">
        <el-radio-group v-model="form.packageMode" :disabled="isReadOnly">
          <el-radio
            v-for="option in MODE_OPTIONS"
            :key="option.value"
            :label="option.value"
          >
            {{ option.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="适用教练" prop="coachIds">
        <el-select
          v-model="form.coachIds"
          multiple
          placeholder="请选择适用教练"
          style="width: 100%"
          :disabled="isReadOnly"
        >
          <el-option
            v-for="coach in coachOptions"
            :key="coach.coachId"
            :label="`${coach.name}（参考单价 ¥${coach.referencePrice}）`"
            :value="coach.coachId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="教学类型" prop="teachingType">
        <el-radio-group v-model="form.teachingType" :disabled="isReadOnly">
          <el-radio
            v-for="option in TEACHING_TYPE_OPTIONS"
            :key="option.value"
            :label="option.value"
          >
            {{ option.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="泳姿" prop="strokeIds">
        <el-checkbox-group v-model="form.strokeIds" :disabled="isReadOnly">
          <el-checkbox
            v-for="stroke in STROKE_OPTIONS"
            :key="stroke.value"
            :label="stroke.value"
          >
            {{ stroke.label }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <div class="form-row">
        <el-form-item label="课时数" prop="totalHours">
          <el-input-number
            v-model="form.totalHours"
            :min="1"
            :max="100"
            :disabled="isReadOnly"
            placeholder="课时数"
          />
        </el-form-item>
        <el-form-item label="每节课时长" prop="durationMinutes">
          <el-input-number
            v-model="form.durationMinutes"
            :min="1"
            :disabled="isReadOnly"
            placeholder="分钟"
          />
        </el-form-item>
        <el-form-item label="有效期" prop="validDays">
          <el-input-number
            v-model="form.validDays"
            :min="1"
            :disabled="isReadOnly"
            placeholder="天"
          />
        </el-form-item>
      </div>

      <div class="form-row">
        <el-form-item label="原价" prop="originalPrice">
          <el-input-number
            v-model="form.originalPrice"
            :min="0"
            :precision="2"
            :disabled="isReadOnly"
            placeholder="元"
          />
        </el-form-item>
        <el-form-item label="售价" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            :disabled="isReadOnly"
            placeholder="元"
          />
        </el-form-item>
      </div>

      <el-form-item label="退款配置">
        <div class="refund-block">
          <el-switch
            v-model="form.refundEnabled"
            active-text="可退"
            inactive-text="不可退"
            :disabled="isReadOnly"
          />
          <div v-if="form.refundEnabled" class="refund-fields">
            <el-form-item prop="refundRatio" class="inline-field">
              <el-input-number
                v-model="form.refundRatio"
                :min="0"
                :max="100"
                :disabled="isReadOnly"
                placeholder="退款比例 %"
              />
            </el-form-item>
            <el-form-item prop="refundValidDays" class="inline-field">
              <el-input-number
                v-model="form.refundValidDays"
                :min="0"
                :disabled="isReadOnly"
                placeholder="退款有效天数"
              />
            </el-form-item>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="标签">
        <div class="tags-wrap">
          <el-tag
            v-for="tag in form.tags"
            :key="tag"
            closable
            :disable-transitions="false"
            @close="handleTagClose(tag)"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="tagInputVisible"
            ref="tagInputRef"
            v-model="tagInputValue"
            class="tag-input"
            size="small"
            @keyup.enter="handleTagInputConfirm"
            @blur="handleTagInputConfirm"
          />
          <el-button
            v-else
            class="button-new-tag"
            size="small"
            :disabled="isReadOnly"
            @click="showTagInput"
          >
            + 添加标签
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="套餐描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          :disabled="isReadOnly"
          placeholder="请输入套餐描述"
        />
      </el-form-item>

      <el-form-item label="展示图片">
        <div class="image-list">
          <div
            v-for="(url, index) in form.images"
            :key="index"
            class="image-item"
          >
            <el-image
              class="image-preview"
              :src="url"
              :preview-src-list="form.images"
              fit="cover"
            />
            <el-button
              v-if="!isReadOnly"
              type="danger"
              link
              size="small"
              @click="removeImage(index)"
            >
              删除
            </el-button>
          </div>
          <el-upload
            v-if="!isReadOnly && form.images.length < 5"
            class="image-upload-trigger"
            :show-file-list="false"
            :http-request="handleImageUpload"
            accept="image/jpeg,image/png,image/webp"
          >
            <div class="image-upload-btn">
              <el-icon><Plus /></el-icon>
              <span>上传图片</span>
            </div>
          </el-upload>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        v-if="!isReadOnly"
        type="primary"
        :loading="saving"
        @click="handleSave"
      >
        {{ mode === 'add' ? '创建' : '保存' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.template-form {
  max-height: 60vh;
  overflow-y: auto;
}

.active-warning {
  margin-bottom: 16px;
}

.form-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.refund-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.refund-fields {
  display: flex;
  gap: 16px;
}

.inline-field {
  margin-bottom: 0;
}

.tags-wrap {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.tag-input {
  width: 100px;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
}

.image-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.image-preview {
  width: 120px;
  height: 120px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.image-upload-trigger {
  cursor: pointer;
}

.image-upload-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  gap: 8px;
  color: #8c8c8c;
  background: #f5f7fa;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  transition: border-color 0.2s;

  &:hover {
    border-color: #409eff;
    color: #409eff;
  }
}
</style>
