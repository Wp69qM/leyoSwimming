<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Document } from '@element-plus/icons-vue';
import type { KnowledgeCategory, KnowledgeContentType } from '@/types/api';
import {
  addKnowledge,
  uploadKnowledgeFile,
} from '@/api/knowledgeManagement';

const props = defineProps({
  visible: {
    type: Boolean,
    required: true,
  },
});

const emit = defineEmits(['update:visible', 'success']);

const activeTab = ref<'manual' | 'file'>('manual');
const formRef = ref();
const loading = ref(false);

const categoryOptions = [
  { label: '安全', value: 'safety' },
  { label: '技巧', value: 'technique' },
  { label: '急救', value: 'emergency' },
  { label: '其他', value: 'other' },
];

const contentTypeOptions = [
  { label: '纯文本', value: 'text' },
  { label: 'Markdown', value: 'markdown' },
];

const form = reactive({
  title: '',
  category: '' as KnowledgeCategory | '',
  contentType: 'text' as KnowledgeContentType,
  content: '',
  file: null as File | null,
});

const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
});

const formRules = {
  title: [
    {
      validator: (_: unknown, value: string, callback: (error?: Error) => void) => {
        if (!value || !value.trim()) {
          callback(new Error('请输入标题'));
        } else if (value.trim().length > 200) {
          callback(new Error('标题最多 200 个字符'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
  category: [
    {
      validator: (_: unknown, value: string, callback: (error?: Error) => void) => {
        if (!value) {
          callback(new Error('请选择分类'));
        } else {
          callback();
        }
      },
      trigger: 'change',
    },
  ],
  contentType: [
    {
      validator: (_: unknown, value: string, callback: (error?: Error) => void) => {
        if (activeTab.value === 'manual' && !value) {
          callback(new Error('请选择内容类型'));
        } else {
          callback();
        }
      },
      trigger: 'change',
    },
  ],
  content: [
    {
      validator: (_: unknown, value: string, callback: (error?: Error) => void) => {
        if (activeTab.value === 'manual' && !value.trim()) {
          callback(new Error('请输入内容'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
  file: [
    {
      validator: (_: unknown, value: File | null, callback: (error?: Error) => void) => {
        if (activeTab.value === 'file' && !value) {
          callback(new Error('请上传文件'));
        } else {
          callback();
        }
      },
      trigger: 'change',
    },
  ],
};

function resetForm() {
  form.title = '';
  form.category = '';
  form.contentType = 'text';
  form.content = '';
  form.file = null;
  activeTab.value = 'manual';
  formRef.value?.resetFields();
}

function handleClose() {
  emit('update:visible', false);
}

function handleFileChange(uploadFile: { raw: File }) {
  form.file = uploadFile.raw;
}

function handleFileRemove() {
  form.file = null;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    if (activeTab.value === 'manual') {
      await addKnowledge({
        title: form.title.trim(),
        category: form.category as KnowledgeCategory,
        contentType: form.contentType,
        content: form.content,
      });
      ElMessage.success('保存成功');
    } else {
      if (!form.file) {
        ElMessage.error('请上传文件');
        return;
      }
      await uploadKnowledgeFile({
        title: form.title.trim(),
        category: form.category as KnowledgeCategory,
        file: form.file,
      });
      ElMessage.success('上传成功');
    }
    emit('success');
    resetForm();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败');
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
    title="新增知识库文档"
    width="560px"
    :close-on-click-modal="false"
    :append-to-body="false"
    destroy-on-close
    @close="handleClose"
  >
    <el-radio-group
      v-model="activeTab"
      class="upload-tabs"
      data-testid="upload-tabs"
    >
      <el-radio-button value="manual">手动输入</el-radio-button>
      <el-radio-button value="file">文件上传</el-radio-button>
    </el-radio-group>

    <el-form
      ref="formRef"
      :model="form"
      :rules="formRules"
      label-width="100px"
      class="upload-form"
      data-testid="knowledge-form"
    >
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="form.title"
          data-testid="title-input"
          placeholder="请输入文档标题"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="分类" prop="category">
        <el-select
          v-model="form.category"
          data-testid="category-select"
          placeholder="请选择分类"
          style="width: 100%"
        >
          <el-option
            v-for="option in categoryOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item
        v-if="activeTab === 'manual'"
        label="内容类型"
        prop="contentType"
        data-testid="manual-form"
      >
        <el-radio-group
          v-model="form.contentType"
          data-testid="content-type-radio"
        >
          <el-radio
            v-for="option in contentTypeOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item
        v-if="activeTab === 'manual'"
        label="内容"
        prop="content"
      >
        <el-input
          v-model="form.content"
          data-testid="content-textarea"
          type="textarea"
          :rows="8"
          placeholder="请输入文档内容"
        />
      </el-form-item>

      <el-form-item
        v-if="activeTab === 'file'"
        label="文件上传"
        prop="file"
        data-testid="file-form"
      >
        <el-upload
          drag
          :auto-upload="false"
          :show-file-list="true"
          :limit="1"
          accept=".txt,.md"
          data-testid="file-upload"
          @change="handleFileChange"
          @remove="handleFileRemove"
        >
          <el-icon class="el-icon--upload"><Document /></el-icon>
          <div class="el-upload__text">
            点击或拖拽文件到此处上传
          </div>
          <template #tip>
            <div class="el-upload__tip upload-tip">
              仅支持 .txt / .md 格式文件
            </div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button data-testid="cancel-button" @click="handleClose">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="loading"
          data-testid="submit-button"
          @click="handleSubmit"
        >
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.upload-tabs {
  display: flex;
  margin: 0 24px;

  :deep(.el-radio-button__inner) {
    padding: 9px 24px;
  }
}

.upload-form {
  padding: 24px 24px 0;
}

.upload-tip {
  margin-top: 8px;
  font-size: 13px;
  color: #8c8c8c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
