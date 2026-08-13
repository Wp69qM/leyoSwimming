<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, Plus } from '@element-plus/icons-vue';
import { useAdminAuthStore } from '@/stores/adminAuth';
import type { AdminAccountListItem, AdminAccountDetail } from '@/types/api';
import {
  getAdminAccountList,
  toggleAdminAccountStatus,
  deleteAdminAccount,
  resetAdminAccountPassword,
} from '@/api/adminAccountManagement';
import { formatDateTime } from '@/utils/format';
import AdminAccountCreateModal from './AdminAccountCreateModal.vue';
import AdminAccountEditModal from './AdminAccountEditModal.vue';
import AdminAccountViewModal from './AdminAccountViewModal.vue';
import AdminAccountResetPasswordModal from './AdminAccountResetPasswordModal.vue';

const authStore = useAdminAuthStore();
const currentAdmin = computed(() => authStore.admin);
const isSuperAdmin = computed(() => currentAdmin.value?.role === 'super_admin');

const roleOptions = [
  { label: '全部', value: '' },
  { label: '超级管理员', value: 'super_admin' },
  { label: '普通管理员', value: 'admin' },
];

const statusOptions = [
  { label: '全部', value: null },
  { label: '启用', value: 0 },
  { label: '禁用', value: 1 },
];

const roleTagMap: Record<
  string,
  { label: string; color: string; bgColor: string }
> = {
  super_admin: { label: '超级管理员', color: '#1890FF', bgColor: '#E6F7FF' },
  admin: { label: '普通管理员', color: '#52C41A', bgColor: '#F6FFED' },
};

const statusTagMap: Record<
  number,
  { label: string; color: string; bgColor: string }
> = {
  0: { label: '启用', color: '#52C41A', bgColor: '#F6FFED' },
  1: { label: '禁用', color: '#FF4D4F', bgColor: '#FFF1F0' },
};

const queryForm = reactive({
  role: '',
  status: null as number | null,
  keyword: '',
});

const tableData = ref<AdminAccountListItem[]>([]);
const loading = ref(false);
const error = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

const createVisible = ref(false);
const editVisible = ref(false);
const viewVisible = ref(false);
const resetPasswordVisible = ref(false);
const selectedAdmin = ref<AdminAccountDetail | null>(null);
const selectedAdminId = ref<number | null>(null);
const tempPassword = ref('');

watch(resetPasswordVisible, (visible) => {
  if (!visible) {
    tempPassword.value = '';
  }
});

function formatLastLogin(value: string | undefined): string {
  return value ? formatDateTime(value) : '从未登录';
}

function hasAction(row: AdminAccountListItem, action: string): boolean {
  return row.allowedActions?.includes(action) ?? false;
}

async function fetchList() {
  loading.value = true;
  error.value = false;
  try {
    const res = await getAdminAccountList({
      page: page.value,
      pageSize: pageSize.value,
      role: queryForm.role || undefined,
      status: queryForm.status,
      keyword: queryForm.keyword || undefined,
    });
    if (res.data) {
      tableData.value = res.data.list;
      total.value = res.data.total;
      page.value = res.data.page;
      pageSize.value = res.data.pageSize;
    }
  } catch (err) {
    error.value = true;
    tableData.value = [];
    total.value = 0;
    ElMessage.error(err instanceof Error ? err.message : '加载失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 1;
  fetchList();
}

function handleReset() {
  queryForm.role = '';
  queryForm.status = null;
  queryForm.keyword = '';
  page.value = 1;
  fetchList();
}

function handlePageChange(current: number) {
  page.value = current;
  fetchList();
}

function openCreate() {
  createVisible.value = true;
}

function openEdit(row: AdminAccountListItem) {
  selectedAdminId.value = row.adminId;
  editVisible.value = true;
}

function openView(row: AdminAccountListItem) {
  selectedAdminId.value = row.adminId;
  viewVisible.value = true;
}

function handleCreateSuccess() {
  createVisible.value = false;
  ElMessage.success('管理员账号创建成功');
  fetchList();
}

function handleEditSuccess(admin: AdminAccountDetail) {
  selectedAdmin.value = admin;
  editVisible.value = false;
  selectedAdminId.value = null;
  ElMessage.success('管理员资料已更新');
  fetchList();
}

function handleViewClosed() {
  viewVisible.value = false;
  selectedAdminId.value = null;
}

function handleViewEdit(adminId: number) {
  viewVisible.value = false;
  selectedAdminId.value = adminId;
  editVisible.value = true;
}

async function handleToggleStatus(
  row: AdminAccountListItem,
  targetStatus: number
) {
  const isDisable = targetStatus === 1;
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入${isDisable ? '禁用' : '启用'}原因`,
      `${isDisable ? '禁用' : '启用'}账号`,
      {
        confirmButtonText: `确认${isDisable ? '禁用' : '启用'}`,
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入原因',
        type: 'warning',
      }
    );
    await toggleAdminAccountStatus({
      adminId: row.adminId,
      status: targetStatus,
      reason: value.trim(),
    });
    ElMessage.success(`账号已${isDisable ? '禁用' : '启用'}`);
    fetchList();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

async function handleDelete(row: AdminAccountListItem) {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入删除原因，删除后该账号将无法登录',
      '删除账号',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入删除原因',
        type: 'warning',
      }
    );
    await deleteAdminAccount({ adminId: row.adminId, reason: value.trim() });
    ElMessage.success('账号已删除');
    fetchList();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

async function handleResetPassword(row: AdminAccountListItem) {
  try {
    await ElMessageBox.confirm(
      '确认重置该账号的密码？重置后将生成一次性临时密码。',
      '重置密码',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    const res = await resetAdminAccountPassword({ adminId: row.adminId });
    if (res.data) {
      tempPassword.value = res.data.tempPassword;
      resetPasswordVisible.value = true;
      ElMessage.success('密码已重置');
      fetchList();
    }
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

function handleCommand(row: AdminAccountListItem, cmd: string) {
  if (cmd === 'disable') {
    handleToggleStatus(row, 1);
  } else if (cmd === 'enable') {
    handleToggleStatus(row, 0);
  } else if (cmd === 'delete') {
    handleDelete(row);
  } else if (cmd === 'reset-password') {
    handleResetPassword(row);
  }
}

onMounted(() => {
  fetchList();
});
</script>

<template>
  <div class="admin-account-management">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>系统设置</el-breadcrumb-item>
        <el-breadcrumb-item>管理员账号</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="filter-card">
      <div class="filter-header">
        <el-button
          v-if="isSuperAdmin"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新建管理员
        </el-button>
        <el-form :model="queryForm" inline>
          <el-form-item>
            <el-select
              v-model="queryForm.role"
              placeholder="全部角色"
              style="width: 160px"
              clearable
            >
              <el-option
                v-for="option in roleOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select
              v-model="queryForm.status"
              placeholder="全部状态"
              style="width: 160px"
              clearable
            >
              <el-option
                v-for="option in statusOptions"
                :key="String(option.value)"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="queryForm.keyword"
              placeholder="姓名 / 登录账号 / ID"
              clearable
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="table-card">
      <el-skeleton v-if="loading" :rows="3" animated />
      <template v-else-if="error">
        <el-empty description="加载失败">
          <el-button type="primary" @click="fetchList">重试</el-button>
        </el-empty>
      </template>
      <template v-else-if="tableData.length === 0">
        <el-empty description="暂无管理员账号">
          <el-button type="primary" @click="handleReset">重置筛选</el-button>
        </el-empty>
      </template>
      <el-table
        v-else
        :data="tableData"
        stripe
        header-row-class-name="table-header"
        style="width: 100%"
      >
        <el-table-column label="账号" prop="username" min-width="140" />
        <el-table-column label="姓名" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openView(row)">
              {{ row.name || '未设置' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="角色" align="center" width="120">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: roleTagMap[row.role]?.color,
                backgroundColor: roleTagMap[row.role]?.bgColor,
              }"
            >
              {{ roleTagMap[row.role]?.label || row.role }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="140">
          <template #default="{ row }">
            {{ row.phone || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: statusTagMap[row.status]?.color,
                backgroundColor: statusTagMap[row.status]?.bgColor,
              }"
            >
              {{ statusTagMap[row.status]?.label || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="160">
          <template #default="{ row }">
            {{ formatLastLogin(row.lastLoginAt) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="hasAction(row, 'VIEW')"
              link
              type="primary"
              @click="openView(row)"
            >
              查看
            </el-button>
            <el-button
              v-if="hasAction(row, 'EDIT')"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-dropdown
              v-if="
                hasAction(row, 'DISABLE') ||
                hasAction(row, 'DELETE') ||
                hasAction(row, 'RESET_PASSWORD')
              "
              trigger="click"
              @command="(cmd: string) => handleCommand(row, cmd)"
            >
              <el-button link type="primary">
                更多
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-if="row.status === 0 && hasAction(row, 'DISABLE')"
                    command="disable"
                  >
                    <span style="color: #ff4d4f">禁用</span>
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="row.status === 1 && hasAction(row, 'DISABLE')"
                    command="enable"
                  >
                    <span style="color: #1890ff">启用</span>
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="hasAction(row, 'RESET_PASSWORD')"
                    command="reset-password"
                  >
                    重置密码
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="hasAction(row, 'DELETE')"
                    command="delete"
                  >
                    <span style="color: #ff4d4f">删除</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="tableData.length > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchList"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <AdminAccountCreateModal
      v-model:visible="createVisible"
      @success="handleCreateSuccess"
    />
    <AdminAccountEditModal
      v-model:visible="editVisible"
      :admin-id="selectedAdminId"
      @success="handleEditSuccess"
    />
    <AdminAccountViewModal
      v-model:visible="viewVisible"
      :admin-id="selectedAdminId"
      @closed="handleViewClosed"
      @edit="handleViewEdit"
    />
    <AdminAccountResetPasswordModal
      v-model:visible="resetPasswordVisible"
      :temp-password="tempPassword"
    />
  </div>
</template>

<style scoped lang="scss">
.admin-account-management {
  padding-bottom: 24px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  height: 48px;
  margin-bottom: 16px;
}

.filter-card {
  padding: 16px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.table-card {
  padding: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  padding: 0 10px;
  font-size: 12px;
  border-radius: 12px;
}

.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 8px 0;
}

:deep(.table-header) {
  th {
    height: 48px;
    font-size: 14px;
    font-weight: 500;
    color: #262626;
    background: #f5f7fa;
  }
}
</style>
