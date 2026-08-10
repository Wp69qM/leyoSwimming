<script setup lang="ts">
import { useAdminAuthStore } from '@/stores/adminAuth';

const authStore = useAdminAuthStore();
</script>

<template>
  <div class="home-page">
    <h1 class="welcome-title">
      欢迎回来，{{ authStore.admin?.name || '管理员' }}！
    </h1>
    <p class="welcome-desc">这是 leyoSwimming 管理后台首页。</p>
  </div>
</template>

<style scoped lang="scss">
.home-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.welcome-title {
  margin: 0;
  font-size: 24px;
  font-weight: 500;
  color: #262626;
}

.welcome-desc {
  margin: 0;
  font-size: 14px;
  color: #8c8c8c;
}
</style>
