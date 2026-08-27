<script setup lang="ts">
import { ref } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { uploadFile } from '@/api/upload';

const props = defineProps<{
  modelValue: string;
  placeholder?: string;
  width?: number;
  height?: number;
  accept?: string;
  maxSize?: number;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const uploading = ref(false);

const previewSize = {
  width: props.width ? `${props.width}px` : '100px',
  height: props.height ? `${props.height}px` : '100px',
};

const acceptTypes = props.accept || 'image/*';
const maxSizeMB = props.maxSize ?? 5;

function validate(file: File): boolean {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件');
    return false;
  }
  if (file.size > maxSizeMB * 1024 * 1024) {
    ElMessage.error(`图片大小不能超过 ${maxSizeMB}MB`);
    return false;
  }
  return true;
}

async function handleUpload(options: { file: File }) {
  const { file } = options;
  if (!validate(file)) return;

  uploading.value = true;
  try {
    const url = await uploadFile(file);
    emit('update:modelValue', url);
    ElMessage.success('上传成功');
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '上传失败');
  } finally {
    uploading.value = false;
  }
}

function clear() {
  emit('update:modelValue', '');
}
</script>

<template>
  <div class="image-input">
    <div v-if="props.modelValue" class="image-preview" :style="previewSize">
      <img :src="props.modelValue" alt="图片" />

      <div class="image-overlay">
        <el-upload
          class="upload-replace"
          action=""
          :show-file-list="false"
          :http-request="handleUpload"
          :accept="acceptTypes"
          :disabled="uploading"
        >
          <el-button type="primary" link :loading="uploading">
            重新上传
          </el-button>
        </el-upload>
        <el-button type="danger" link @click="clear">删除</el-button>
      </div>
    </div>

    <el-upload
      v-else
      class="upload-trigger"
      action=""
      :show-file-list="false"
      :http-request="handleUpload"
      :accept="acceptTypes"
      :disabled="uploading"
    >
      <div class="upload-box" :style="previewSize">
        <el-icon :size="24" class="upload-icon"><Plus /></el-icon>
        <span class="upload-text">{{ uploading ? '上传中...' : '点击上传' }}</span>
      </div>
    </el-upload>
  </div>
</template>

<style scoped lang="scss">
.image-input {
  display: flex;
  align-items: center;
  gap: 12px;
}

.image-preview {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  background: #f5f7fa;
  border-radius: 4px;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &:hover .image-overlay {
    opacity: 1;
  }
}

.image-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.2s;
}

.upload-trigger {
  line-height: 1;
}

.upload-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #8c8c8c;
  cursor: pointer;
  background: #f5f7fa;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  transition: border-color 0.2s;

  &:hover {
    border-color: #409eff;
  }
}

.upload-icon {
  color: #a8abb2;
}

.upload-text {
  font-size: 12px;
}
</style>
