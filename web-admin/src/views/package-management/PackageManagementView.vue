<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type {
  AdminPackageListItem,
  PackageStatus,
  PackageMode,
} from '@/types/api';
import { getPackageList } from '@/api/packageManagement';
import { formatDateTime } from '@/utils/format';

const router = useRouter();

const statusOptions = [
  { label: '全部', value: '' },
  { label: '活跃', value: 'active' },
  { label: '已耗尽', value: 'exhausted' },
  { label: '已过期', value: 'expired' },
  { label: '已冻结', value: 'frozen' },
  { label: '已退款', value: 'refunded' },
];

const courseTypeOptions = [
  { label: '全部', value: '' },
  { label: '体验课', value: 'experience' },
  { label: '正价课', value: 'standard' },
];

const statusMap: Record<
  PackageStatus,
  { label: string; color: string; bgColor: string }
> = {
  active: { label: '活跃', color: '#52C41A', bgColor: '#F6FFED' },
  exhausted: { label: '已耗尽', color: '#8C8C8C', bgColor: '#F5F5F5' },
  expired: { label: '已过期', color: '#FAAD14', bgColor: '#FFFBE6' },
  frozen: { label: '已冻结', color: '#722ED1', bgColor: '#F9F0FF' },
  refunded: { label: '已退款', color: '#FF4D4F', bgColor: '#FFF1F0' },
};

const modeMap: Record<
  PackageMode,
  { label: string; color: string; bgColor: string }
> = {
  standard: { label: '正价套餐', color: '#1890FF', bgColor: '#E6F7FF' },
  experience: { label: '体验课', color: '#FAAD14', bgColor: '#FFFBE6' },
};

const queryForm = reactive({
  status: '',
  courseType: '',
  startExpireAt: '',
  endExpireAt: '',
  keyword: '',
});

const tableData = ref<AdminPackageListItem[]>([]);
const loading = ref(false);
const error = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

function formatHours(row: AdminPackageListItem): string {
  return `${row.availableCount}/${row.reservedCount}/${row.consumedCount}`;
}

async function fetchList() {
  loading.value = true;
  error.value = false;
  try {
    const res = await getPackageList({
      page: page.value,
      pageSize: pageSize.value,
      status: (queryForm.status as PackageStatus) || undefined,
      courseType: queryForm.courseType || undefined,
      startExpireAt: queryForm.startExpireAt || undefined,
      endExpireAt: queryForm.endExpireAt || undefined,
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
  queryForm.status = '';
  queryForm.courseType = '';
  queryForm.startExpireAt = '';
  queryForm.endExpireAt = '';
  queryForm.keyword = '';
  page.value = 1;
  fetchList();
}

function handlePageChange(current: number) {
  page.value = current;
  fetchList();
}

function openDetail(row: AdminPackageListItem) {
  router.push(`/package-management/detail/${row.packageId}`);
}

function handleFreeze(row: AdminPackageListItem) {
  ElMessage.info('冻结功能即将上线');
}

function handleUnfreeze(row: AdminPackageListItem) {
  ElMessage.info('解冻功能即将上线');
}

function handleExtend(row: AdminPackageListItem) {
  ElMessage.info('延期功能即将上线');
}

function handleRefund(row: AdminPackageListItem) {
  ElMessage.info('请前往订单管理处理退款订单');
}

onMounted(() => {
  fetchList();
});
</script>

<template>
  <div class="package-management">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>套餐订单</el-breadcrumb-item>
        <el-breadcrumb-item>套餐管理</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="filter-card">
      <el-form :model="queryForm" inline class="filter-form">
        <div class="filter-row">
          <el-form-item label="套餐状态">
            <el-select
              v-model="queryForm.status"
              placeholder="全部"
              style="width: 160px"
              clearable
            >
              <el-option
                v-for="option in statusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="课程类型">
            <el-select
              v-model="queryForm.courseType"
              placeholder="全部"
              style="width: 160px"
              clearable
            >
              <el-option
                v-for="option in courseTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="到期时间">
            <el-date-picker
              v-model="queryForm.startExpireAt"
              type="date"
              placeholder="开始日期"
              value-format="YYYY-MM-DD"
              style="width: 160px"
            />
            <span class="date-separator">至</span>
            <el-date-picker
              v-model="queryForm.endExpireAt"
              type="date"
              placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input
              v-model="queryForm.keyword"
              placeholder="套餐编号 / 用户 / 教练"
              clearable
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
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
        <el-empty description="暂无套餐数据">
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
        <el-table-column label="套餐编号" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">
              {{ row.packageNo }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="120">
          <template #default="{ row }">
            {{ row.userName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="教练" width="120">
          <template #default="{ row }">
            {{ row.coachName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="套餐模式" align="center" width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: modeMap[row.packageMode]?.color,
                backgroundColor: modeMap[row.packageMode]?.bgColor,
              }"
            >
              {{ modeMap[row.packageMode]?.label || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" align="center" width="100">
          <template #default="{ row }">
            {{ row.courseType || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: statusMap[row.status]?.color,
                backgroundColor: statusMap[row.status]?.bgColor,
              }"
            >
              {{ statusMap[row.status]?.label || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="剩余课时" align="center" width="100">
          <template #default="{ row }">
            {{ formatHours(row) }}
          </template>
        </el-table-column>
        <el-table-column label="到期时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.expireAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">
              查看
            </el-button>
            <el-button
              v-if="row.status === 'active'"
              link
              type="danger"
              @click="handleFreeze(row)"
            >
              冻结
            </el-button>
            <el-button
              v-if="row.status === 'frozen'"
              link
              type="primary"
              @click="handleUnfreeze(row)"
            >
              解冻
            </el-button>
            <el-button
              v-if="row.status === 'active' || row.status === 'expired'"
              link
              type="primary"
              @click="handleExtend(row)"
            >
              延期
            </el-button>
            <el-button
              v-if="row.status === 'active'"
              link
              type="warning"
              @click="handleRefund(row)"
            >
              退款
            </el-button>
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
  </div>
</template>

<style scoped lang="scss">
.package-management {
  padding-bottom: 24px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  height: 48px;
  margin-bottom: 16px;
}

.filter-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.filter-form {
  flex: 1;
  margin-bottom: 0;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}

.filter-actions {
  margin-left: auto;
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
  justify-content: center;
  padding-top: 16px;
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
    padding-right: 8px;
    font-size: 14px;
    color: #262626;
  }
}

:deep(.el-table__body) {
  .el-table__row:last-child td {
    border-bottom: none;
  }
}
</style>
