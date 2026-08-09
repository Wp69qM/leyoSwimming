<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useAdminAuthStore } from '@/stores/adminAuth';

const router = useRouter();
const authStore = useAdminAuthStore();

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await authStore.logout();
    ElMessage.success('已退出登录');
    await router.push('/login');
  } catch {
    // 用户取消退出
  }
}
</script>

<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="brand">
          <div class="brand-logo">
            <svg class="brand-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path
                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"
                fill="currentColor"
              />
            </svg>
          </div>
          <span class="brand-name">leyoSwimming</span>
        </div>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item" active-class="active"> 首页 </router-link>
      </nav>
    </aside>

    <main class="main">
      <header class="header">
        <div class="header-left">
          <h2 class="page-title">管理后台</h2>
        </div>
        <div class="header-right">
          <el-dropdown v-if="authStore.admin" trigger="click">
            <span class="user-info">
              <span class="user-name">{{ authStore.admin.name }}</span>
              <span class="dropdown-icon">▼</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  角色：{{ authStore.admin.role === 'super_admin' ? '超级管理员' : '管理员' }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout"> 退出登录 </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <div class="content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #f5f7fa;
}

.sidebar {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: 200px;
  height: 100%;
  background: #ffffff;
  border-right: 1px solid #e4e7ed;
}

.sidebar-header {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 16px;
  border-bottom: 1px solid #e4e7ed;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: #ffffff;
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 100%);
  border-radius: 8px;
}

.brand-icon {
  width: 22px;
  height: 22px;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  overflow-y: auto;
}

.nav-item {
  display: block;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.43;
  color: #595959;
  text-decoration: none;
  border-radius: 8px;

  &:hover {
    color: #1890ff;
    background: #f0f7ff;
  }

  &.active {
    font-weight: 500;
    color: #1890ff;
    background: #e6f7ff;
  }
}

.main {
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  overflow: hidden;
}

.header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #262626;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;

  &:hover {
    background: #f5f7fa;
  }
}

.user-name {
  font-size: 14px;
  color: #606266;
}

.dropdown-icon {
  font-size: 10px;
  color: #909399;
}

.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
