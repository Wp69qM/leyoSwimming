<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage, ElUpload } from 'element-plus';
import { uploadFile } from '@/api/upload';
import { Delete } from '@element-plus/icons-vue';

const props = defineProps<{
  modelValue: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const uploading = ref(false);

const DEFAULT_AVATAR =
  'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgZmlsbD0iI2Y1ZjdmYSIvPjxjaXJjbGUgY3g9IjUwIiBjeT0iMzgiIHI9IjE4IiBmaWxsPSIjYzBjNGNjIi8+PHBhdGggZD0iTTIwIDg1YzAtMTYgMTktMjggMzAtMjhzMzAgMTIgMzAgMjgiIGZpbGw9IiNjMGM0Y2MiLz48L3N2Zz4=';

const previewUrl = computed(() => props.modelValue || DEFAULT_AVATAR);

async function handleUpload(options: { file: File }) {
  const file = options.file;
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件');
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB');
    return;
  }

  uploading.value = true;
  try {
    const url = await uploadFile(file);
    emit('update:modelValue', url);
    ElMessage.success('头像上传成功');
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '头像上传失败');
  } finally {
    uploading.value = false;
  }
}

function handleClear() {
  emit('update:modelValue', '');
}
</script>

<template>
  <div class="avatar-input">
    <el-upload
      class="avatar-upload"
      :show-file-list="false"
      :http-request="handleUpload"
      accept="image/jpeg,image/png,image/webp"
      :disabled="uploading"
    >
      <div class="avatar-preview" :class="{ uploading }">
        <img :src="previewUrl" alt="头像" />
        <div class="avatar-overlay">
          <span v-if="uploading">上传中...</span>
          <span v-else>点击上传</span>
        </div>
      </div>
    </el-upload>

    <el-button
      v-if="props.modelValue"
      type="danger"
      link
      :icon="Delete"
      @click="handleClear"
    >
      删除
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.avatar-input {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-upload {
  cursor: pointer;
}

.avatar-preview {
  position: relative;
  width: 80px;
  height: 80px;
  overflow: hidden;
  background: #f5f7fa;
  border-radius: 50%;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .avatar-overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #fff;
    background: rgba(0, 0, 0, 0.4);
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .avatar-overlay,
  &.uploading .avatar-overlay {
    opacity: 1;
  }
}
</style>
