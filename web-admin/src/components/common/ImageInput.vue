<script setup lang="ts">
const props = defineProps<{
  modelValue: string;
  placeholder?: string;
  width?: number;
  height?: number;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

function update(value: string) {
  emit('update:modelValue', value);
}

function clear() {
  emit('update:modelValue', '');
}
</script>

<template>
  <div class="image-input">
    <div
      class="image-preview"
      :style="{
        width: props.width ? `${props.width}px` : '100px',
        height: props.height ? `${props.height}px` : '100px',
      }"
    >
      <img v-if="props.modelValue" :src="props.modelValue" alt="图片" />
      <span v-else class="image-placeholder">{{
        props.placeholder || '未上传'
      }}</span>
    </div>
    <div class="image-actions">
      <el-input
        :model-value="props.modelValue"
        placeholder="请输入图片 URL"
        style="width: 200px"
        @update:model-value="update"
      />
      <el-button v-if="props.modelValue" link type="danger" @click="clear">
        删除
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.image-input {
  display: flex;
  align-items: center;
  gap: 12px;
}

.image-preview {
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
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 8px;
  font-size: 12px;
  color: #8c8c8c;
  text-align: center;
}

.image-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
