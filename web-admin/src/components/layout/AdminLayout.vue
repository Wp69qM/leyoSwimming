<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useAdminAuthStore } from '@/stores/adminAuth';

const route = useRoute();
const router = useRouter();
const authStore = useAdminAuthStore();

interface MenuItem {
  title: string;
  path?: string;
  disabled?: boolean;
}

interface MenuGroup {
  title: string;
  children: MenuItem[];
}

const menuGroups: MenuGroup[] = [
  {
    title: '用户管理',
    children: [
      { title: '用户列表', path: '/user-management' },
      { title: '教练入驻审核', path: '/coach-audit/queue' },
      { title: '教练管理', path: '/coach-management' },
      { title: '教练离职审批', path: '/resignation/approval-queue' },
    ],
  },
  {
    title: '课程预约',
    children: [
      { title: '排班管理', path: '/schedule-management', disabled: true },
      { title: '请假审批', path: '/leave-approval', disabled: true },
      { title: '预约释放配置', path: '/release-config', disabled: true },
    ],
  },
  {
    title: '套餐订单',
    children: [
      { title: '套餐管理', path: '/package-management' },
      { title: '订单管理', path: '/order-management' },
      { title: '套餐配置', path: '/package-config' },
    ],
  },
  {
    title: '场馆运营',
    children: [
      { title: '场馆配置', path: '/venue-config', disabled: true },
      {
        title: '公告/Banner/卡片',
        path: '/announcement-config',
        disabled: true,
      },
      { title: '用户须知', path: '/user-agreement-config', disabled: true },
      { title: '闭馆/换水设置', path: '/closure-config', disabled: true },
    ],
  },
  {
    title: '客服工单',
    children: [
      { title: '工单列表', path: '/ticket-management', disabled: true },
    ],
  },
  {
    title: '系统设置',
    children: [{ title: '管理员账号', path: '/admin-account-management' }],
  },
];

function isMenuActive(path: string): boolean {
  return route.path === path || route.path.startsWith(`${path}/`);
}

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
      <nav class="sidebar-nav">
        <router-link
          to="/"
          class="nav-item nav-item--primary"
          exact-active-class="active"
        >
          首页
        </router-link>

        <div v-for="group in menuGroups" :key="group.title" class="nav-group">
          <div class="nav-group-title">{{ group.title }}</div>
          <template v-for="item in group.children" :key="item.title">
            <router-link
              v-if="!item.disabled && item.path"
              :to="item.path"
              class="nav-item nav-item--secondary"
              :class="{ active: item.path && isMenuActive(item.path) }"
            >
              {{ item.title }}
            </router-link>
            <div
              v-else
              class="nav-item nav-item--secondary nav-item--disabled"
              :title="`${item.title}（暂未开放）`"
            >
              {{ item.title }}
            </div>
          </template>
        </div>
      </nav>
    </aside>

    <main class="main">
      <header class="header">
        <div class="header-left">
          <div class="brand">
            <div class="brand-logo">
              <svg
                class="brand-icon"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"
                  fill="currentColor"
                />
              </svg>
            </div>
            <span class="brand-name">leyoSwimming 管理后台</span>
          </div>
        </div>
        <div class="header-right">
          <el-dropdown v-if="authStore.admin" trigger="click">
            <span class="user-info">
              <span class="user-avatar">{{
                authStore.admin.name?.charAt(0)
              }}</span>
              <span class="user-name">{{ authStore.admin.name }}</span>
              <span class="dropdown-icon">▼</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  角色：{{
                    authStore.admin.role === 'super_admin'
                      ? '超级管理员'
                      : '管理员'
                  }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  退出登录
                </el-dropdown-item>
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
  background: #f0f2f5;
}

.sidebar {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: 220px;
  height: 100%;
  background: #001529;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 0;
  overflow-y: auto;
}

.nav-group {
  margin-top: 8px;
}

.nav-group-title {
  display: flex;
  align-items: center;
  height: 48px;
  padding: 0 24px;
  font-size: 16px;
  font-weight: 500;
  color: #ffffff;
}

.nav-item {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 24px;
  text-decoration: none;
  cursor: pointer;
  transition:
    background 0.2s,
    color 0.2s;

  &--primary {
    height: 48px;
    padding: 0 24px;
    font-size: 16px;
    font-weight: 500;
    color: #ffffff;
  }

  &--secondary {
    padding-left: 56px;
    font-size: 14px;
    color: #bfbfbf;
  }

  &--disabled {
    cursor: not-allowed;
    opacity: 0.5;
  }

  &:hover:not(.active):not(.nav-item--disabled) {
    color: #ffffff;
    background: rgba(255, 255, 255, 0.08);
  }

  &.active {
    color: #ffffff;
    background: #1890ff;
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

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: #ffffff;
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 100%);
  border-radius: 8px;
}

.brand-icon {
  width: 20px;
  height: 20px;
}

.brand-name {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;

  &:hover {
    background: #f5f7fa;
  }
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  font-size: 14px;
  color: #ffffff;
  background: #1890ff;
  border-radius: 50%;
}

.user-name {
  font-size: 14px;
  color: #262626;
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
