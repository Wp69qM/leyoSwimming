<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { Calendar, Search, ArrowUp, ArrowDown } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { AdminUserListItem, AdminUserDetail } from '@/types/api';
import { getUserList, banUser, unbanUser } from '@/api/userManagement';
import { formatDateTime } from '@/utils/format';
import UserCreateModal from './UserCreateModal.vue';
import UserEditModal from './UserEditModal.vue';
import UserViewModal from './UserViewModal.vue';

const identityOptions = [
  { label: '全部', value: null },
  { label: '游客', value: 0 },
  { label: '注册用户', value: 1 },
  { label: '学员', value: 2 },
];

const statusOptions = [
  { label: '全部', value: null },
  { label: '正常', value: 0 },
  { label: '注销', value: 1 },
  { label: '封禁', value: 2 },
];

const profileOptions = [
  { label: '全部', value: null },
  { label: '已完善', value: true },
  { label: '未完善', value: false },
];

const genderOptions = [
  { label: '全部', value: null },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
];

const queryForm = reactive({
  identity: null as number | null,
  status: null as number | null,
  profileCompleted: null as boolean | null,
  gender: null as number | null,
  minAge: null as string | number | null,
  maxAge: null as string | number | null,
  source: '',
  startDate: '',
  endDate: '',
  keyword: '',
});

const dateRange = computed<[string, string] | ''>({
  get() {
    return queryForm.startDate && queryForm.endDate
      ? [queryForm.startDate, queryForm.endDate]
      : '';
  },
  set(val) {
    if (Array.isArray(val) && val.length === 2) {
      queryForm.startDate = val[0];
      queryForm.endDate = val[1];
    } else {
      queryForm.startDate = '';
      queryForm.endDate = '';
    }
  },
});

const tableData = ref<AdminUserListItem[]>([]);
const loading = ref(false);
const error = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const advancedExpanded = ref(false);

const createVisible = ref(false);
const editVisible = ref(false);
const viewVisible = ref(false);
const selectedUser = ref<AdminUserDetail | null>(null);
const selectedUserId = ref<number | null>(null);

const identityTagMap: Record<
  number,
  { label: string; color: string; bgColor: string }
> = {
  0: { label: '游客', color: '#8C8C8C', bgColor: '#F5F5F5' },
  1: { label: '注册用户', color: '#1890FF', bgColor: '#E6F7FF' },
  2: { label: '学员', color: '#52C41A', bgColor: '#F6FFED' },
};

const statusTagMap: Record<
  number,
  { label: string; color: string; bgColor: string }
> = {
  0: { label: '正常', color: '#52C41A', bgColor: '#F6FFED' },
  1: { label: '注销', color: '#8C8C8C', bgColor: '#F5F5F5' },
  2: { label: '封禁', color: '#FF4D4F', bgColor: '#FFF1F0' },
};

const profileTagMap: Record<
  boolean,
  { label: string; color: string; bgColor: string }
> = {
  true: { label: '已完善', color: '#52C41A', bgColor: '#F6FFED' },
  false: { label: '未完善', color: '#FAAD14', bgColor: '#FFFBE6' },
};

function formatGender(gender: number): string {
  return gender === 1 ? '男' : gender === 2 ? '女' : '-';
}

function formatAge(age: number | undefined): string {
  return age == null ? '未知' : String(age);
}

function formatName(name: string | undefined): string {
  return name || '未设置';
}

function normalizeAge(value: string | number | null): number | undefined {
  if (value === null || value === undefined || value === '') return undefined;
  const num = Number(value);
  return Number.isNaN(num) ? undefined : num;
}

async function fetchList() {
  loading.value = true;
  error.value = false;
  try {
    const minAge = normalizeAge(queryForm.minAge);
    const maxAge = normalizeAge(queryForm.maxAge);
    const res = await getUserList({
      page: page.value,
      pageSize: pageSize.value,
      identity: queryForm.identity,
      status: queryForm.status,
      profileCompleted: queryForm.profileCompleted,
      gender: queryForm.gender,
      minAge,
      maxAge,
      source: queryForm.source || undefined,
      startDate: queryForm.startDate || undefined,
      endDate: queryForm.endDate || undefined,
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
  const minAge = normalizeAge(queryForm.minAge);
  const maxAge = normalizeAge(queryForm.maxAge);
  if (minAge != null && maxAge != null && minAge > maxAge) {
    ElMessage.warning('最小年龄不能大于最大年龄');
    return;
  }
  page.value = 1;
  fetchList();
}

function handleReset() {
  queryForm.identity = null;
  queryForm.status = null;
  queryForm.profileCompleted = null;
  queryForm.gender = null;
  queryForm.minAge = null;
  queryForm.maxAge = null;
  queryForm.source = '';
  queryForm.startDate = '';
  queryForm.endDate = '';
  queryForm.keyword = '';
  advancedExpanded.value = false;
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

function openEdit(row: AdminUserListItem) {
  selectedUserId.value = row.userId;
  editVisible.value = true;
}

function openView(row: AdminUserListItem) {
  selectedUserId.value = row.userId;
  viewVisible.value = true;
}

function handleCommand(command: string, row: AdminUserListItem) {
  if (command === 'view') {
    openView(row);
  }
}

function handleCreateSuccess(user: AdminUserDetail) {
  selectedUser.value = user;
  createVisible.value = false;
  ElMessage.success('用户已创建');
  fetchList();
}

function handleEditSuccess() {
  editVisible.value = false;
  selectedUserId.value = null;
  ElMessage.success('用户资料已更新');
  fetchList();
}

function handleViewClosed() {
  viewVisible.value = false;
  selectedUserId.value = null;
}

async function handleBan(row: AdminUserListItem) {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入封禁原因，用户将收到该原因',
      '封禁用户',
      {
        confirmButtonText: '确认封禁',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入封禁原因',
        type: 'warning',
      }
    );
    await banUser({ userId: row.userId, reason: value.trim() });
    ElMessage.success('已封禁');
    fetchList();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

async function handleUnban(row: AdminUserListItem) {
  try {
    await ElMessageBox.confirm('确认解除该用户的封禁状态？', '解禁用户', {
      confirmButtonText: '确认解禁',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await unbanUser({ userId: row.userId, reason: '管理员手动解禁' });
    ElMessage.success('已解禁');
    fetchList();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

onMounted(() => {
  fetchList();
});
</script>

<template>
  <div class="user-management">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>用户管理</el-breadcrumb-item>
        <el-breadcrumb-item>用户列表</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="page-header">
      <h1 class="page-title">用户列表</h1>
      <el-button type="primary" @click="openCreate">新建用户</el-button>
    </div>

    <div class="filter-card">
      <el-form :model="queryForm" inline class="filter-form" label-width="0">
        <div class="filter-row">
          <el-form-item>
            <el-select
              v-model="queryForm.identity"
              placeholder="全部身份"
              style="width: 160px"
              clearable
            >
              <el-option
                v-for="option in identityOptions"
                :key="String(option.value)"
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
            <el-select
              v-model="queryForm.profileCompleted"
              placeholder="资料完善状态"
              style="width: 160px"
              clearable
            >
              <el-option
                v-for="option in profileOptions"
                :key="String(option.value)"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :prefix-icon="Calendar"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="queryForm.keyword"
              placeholder="昵称 / 手机号 / ID"
              clearable
              :prefix-icon="Search"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button
              link
              type="primary"
              class="advanced-toggle"
              @click="advancedExpanded = !advancedExpanded"
            >
              <el-icon class="toggle-icon" :size="14">
                <component :is="advancedExpanded ? ArrowUp : ArrowDown" />
              </el-icon>
              {{ advancedExpanded ? '收起高级筛选' : '展开高级筛选' }}
            </el-button>
          </el-form-item>
        </div>
        <div v-show="advancedExpanded" class="filter-row advanced-row">
          <el-form-item>
            <el-select
              v-model="queryForm.gender"
              placeholder="全部性别"
              style="width: 160px"
              clearable
            >
              <el-option
                v-for="option in genderOptions"
                :key="String(option.value)"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="queryForm.minAge"
              type="number"
              placeholder="最小年龄"
              min="0"
              max="120"
              style="width: 100px"
            />
            <span class="date-separator">至</span>
            <el-input
              v-model="queryForm.maxAge"
              type="number"
              placeholder="最大年龄"
              min="0"
              max="120"
              style="width: 100px"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="queryForm.source"
              placeholder="注册来源"
              clearable
              style="width: 200px"
            />
          </el-form-item>
        </div>
      </el-form>
    </div>

    <div class="table-card">
      <el-skeleton v-if="loading" :rows="3" animated />
      <template v-else-if="error">
        <el-empty description="加载失败">
          <el-button type="primary" @click="fetchList">重试</el-button>
        </el-empty>
      </template>
      <template v-else-if="tableData.length === 0">
        <el-empty description="暂无用户数据">
          <el-button type="primary" @click="handleReset">重置筛选</el-button>
        </el-empty>
      </template>
      <el-table
        v-else
        :data="tableData"
        header-row-class-name="table-header"
        row-class-name="table-row"
        style="width: 100%"
      >
        <el-table-column label="用户 ID" width="80">
          <template #default="{ row }">
            <span class="id-text">{{ row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="昵称/姓名" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openView(row)">
              {{ formatName(row.name) }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="120">
          <template #default="{ row }">
            <PhoneReveal :phone="row.phone" />
          </template>
        </el-table-column>
        <el-table-column label="性别" align="center" width="80">
          <template #default="{ row }">
            {{ formatGender(row.gender) }}
          </template>
        </el-table-column>
        <el-table-column label="年龄" align="center" width="80">
          <template #default="{ row }">
            {{ formatAge(row.age) }}
          </template>
        </el-table-column>
        <el-table-column label="身份" align="center" width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: identityTagMap[row.identity]?.color,
                backgroundColor: identityTagMap[row.identity]?.bgColor,
              }"
            >
              {{ identityTagMap[row.identity]?.label || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="资料完善" align="center" width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: profileTagMap[row.profileCompleted]?.color,
                backgroundColor: profileTagMap[row.profileCompleted]?.bgColor,
              }"
            >
              {{ profileTagMap[row.profileCompleted]?.label || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" align="center" width="100">
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
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openView(row)">
              查看
            </el-button>
            <el-button link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="row.status !== 2"
              link
              type="danger"
              @click="handleBan(row)"
            >
              封禁
            </el-button>
            <el-button v-else link type="primary" @click="handleUnban(row)">
              解禁
            </el-button>
            <el-dropdown
              trigger="click"
              @command="(cmd: string) => handleCommand(cmd, row)"
            >
              <span class="operation-more">
                更多
                <i class="more-arrow"></i>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="view">查看日志</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="tableData.length > 0" class="pagination-wrap">
        <div class="pagination-total">
          共
          <span class="total-number">{{ total.toLocaleString() }}</span> 名用户
        </div>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="sizes, prev, pager, next"
          @size-change="fetchList"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <UserCreateModal
      v-model:visible="createVisible"
      @success="handleCreateSuccess"
    />
    <UserEditModal
      v-model:visible="editVisible"
      :user-id="selectedUserId"
      @success="handleEditSuccess"
    />
    <UserViewModal
      v-model:visible="viewVisible"
      :user-id="selectedUserId"
      @edit="
        () => {
          viewVisible = false;
          editVisible = true;
        }
      "
      @ban="(row) => handleBan(row)"
      @unban="(row) => handleUnban(row)"
      @closed="handleViewClosed"
    />
  </div>
</template>

<style scoped lang="scss">
.user-management {
  padding-bottom: 24px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  height: 48px;
  margin-bottom: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1d2129;
}

.filter-card {
  padding: 16px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.filter-form {
  margin-bottom: 0;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.advanced-row {
  margin-top: 12px;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.advanced-toggle {
  padding-right: 0;
  padding-left: 0;
}

.toggle-icon {
  margin-right: 4px;
}

.date-separator {
  display: inline-block;
  padding: 0 8px;
  font-size: 14px;
  color: #8c8c8c;
}

.table-card {
  padding: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.id-text {
  font-size: 13px;
  color: #86909c;
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

.operation-more {
  display: inline-flex;
  align-items: center;
  margin-left: 12px;
  font-size: 14px;
  line-height: 1;
  color: #1890ff;
  cursor: pointer;
  user-select: none;

  &:hover {
    color: #40a9ff;
  }
}

.more-arrow {
  display: inline-block;
  width: 0;
  height: 0;
  margin-left: 4px;
  border-top: 4px solid currentcolor;
  border-right: 4px solid transparent;
  border-left: 4px solid transparent;
  transition: transform 0.2s ease;
}

.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 8px 0;
}

.pagination-total {
  font-size: 14px;
  color: #86909c;
}

.pagination-total .total-number {
  font-weight: 500;
  color: #1d2129;
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

:deep(.table-row) {
  td {
    height: 56px;
    border-bottom: 1px solid #f0f2f5;
  }
}

:deep(.el-form--inline) {
  .el-form-item {
    margin-right: 0;
    margin-bottom: 0;
  }

  .el-form-item__label {
    display: none;
  }
}

:deep(.el-table__body) {
  .el-table__row:last-child td {
    border-bottom: none;
  }
}
</style>
