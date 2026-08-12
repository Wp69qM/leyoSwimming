<script setup lang="ts">
import { ref } from 'vue';
import { maskPhone } from '@/utils/format';

const props = defineProps<{
  phone: string;
}>();

const revealed = ref(false);

function toggle() {
  revealed.value = !revealed.value;
}
</script>

<template>
  <span class="phone-reveal">
    <span class="phone-text">{{
      revealed ? props.phone : maskPhone(props.phone)
    }}</span>
    <el-button
      v-if="props.phone"
      link
      type="primary"
      class="reveal-btn"
      @click.stop="toggle"
    >
      {{ revealed ? '隐藏' : '查看' }}
    </el-button>
  </span>
</template>

<style scoped lang="scss">
.phone-reveal {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.phone-text {
  font-variant-numeric: tabular-nums;
}

.reveal-btn {
  padding: 0;
  font-size: 12px;
}
</style>
