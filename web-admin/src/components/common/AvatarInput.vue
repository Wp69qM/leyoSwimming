<script setup lang="ts">
const props = defineProps<{
  modelValue: string;
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
  <div class="avatar-input">
    <div class="avatar-preview">
      <img v-if="props.modelValue" :src="props.modelValue" alt="头像" />
      <span v-else class="avatar-placeholder">未上传</span>
    </div>
    <div class="avatar-actions">
      <el-input
        :model-value="props.modelValue"
        placeholder="请输入头像图片 URL"
        style="width: 240px"
        @update:model-value="update"
      />
      <el-button v-if="props.modelValue" link type="danger" @click="clear">
        删除
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.avatar-input {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-preview {
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
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 12px;
  color: #8c8c8c;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
