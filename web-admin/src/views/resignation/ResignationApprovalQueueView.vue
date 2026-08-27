<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Warning,
  CircleCheck,
  CircleClose,
  User,
} from '@element-plus/icons-vue';
import {
  listResignationTickets,
  approveResignationTicket,
  rejectResignationTicket,
} from '@/api/resignation';
import type { ResignationTicket, ResignationStatus } from '@/types/resignation';
import { maskPhone, formatDateTime, calculateTenure } from '@/utils/format';

const router = useRouter();

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审批', value: 'pending_audit' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
];

const queryForm = reactive({
  status: '' as ResignationStatus | '',
  keyword: '',
  submitStartDate: '',
  submitEndDate: '',
});

const tableData = ref<ResignationTicket[]>([]);
const loading = ref(false);
const error = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

function isToday(dateStr: string | undefined): boolean {
  if (!dateStr) return false;
  const d = new Date(dateStr);
  const today = new Date();
  return (
    d.getFullYear() === today.getFullYear() &&
    d.getMonth() === today.getMonth() &&
    d.getDate() === today.getDate()
  );
}

const pendingCount = computed(
  () => tableData.value.filter((item) => item.status === 'pending_audit').length
);

const todayApprovedCount = computed(
  () =>
    tableData.value.filter(
      (item) => item.status === 'approved' && isToday(item.submittedAt)
    ).length
);

const todayRejectedCount = computed(
  () =>
    tableData.value.filter(
      (item) => item.status === 'rejected' && isToday(item.submittedAt)
    ).length
);

const statCards = computed(() => [
  {
    label: '待审批',
    value: pendingCount.value,
    color: '#FAAD14',
    bgColor: '#FFFBE6',
    icon: Warning,
  },
  {
    label: '今日通过',
    value: todayApprovedCount.value,
    color: '#52C41A',
    bgColor: '#F6FFED',
    icon: CircleCheck,
  },
  {
    label: '今日拒绝',
    value: todayRejectedCount.value,
    color: '#FF4D4F',
    bgColor: '#FFF1F0',
    icon: CircleClose,
  },
  {
    label: '累计离职',
    value: total.value,
    color: '#8C8C8C',
    bgColor: '#F5F5F5',
    icon: User,
  },
]);

function calculateProgress(total: number, handled: number): number {
  if (total <= 0) return 100;
  return Math.round((handled / total) * 100);
}

const statusMap: Record<
  ResignationStatus,
  { label: string; color: string; bgColor: string }
> = {
  pending_audit: { label: '待审批', color: '#1890FF', bgColor: '#E6F7FF' },
  approved: { label: '已通过', color: '#52C41A', bgColor: '#F6FFED' },
  rejected: { label: '已驳回', color: '#FF4D4F', bgColor: '#FFF1F0' },
  processing: { label: '处理中', color: '#FAAD14', bgColor: '#FFFBE6' },
};

async function fetchList() {
  loading.value = true;
  error.value = false;
  try {
    const res = await listResignationTickets({
      page: page.value,
      pageSize: pageSize.value,
      status: queryForm.status || undefined,
      keyword: queryForm.keyword || undefined,
      submitStartDate: queryForm.submitStartDate || undefined,
      submitEndDate: queryForm.submitEndDate || undefined,
    });
    if (res.data) {
      tableData.value = res.data.items;
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
  queryForm.keyword = '';
  queryForm.submitStartDate = '';
  queryForm.submitEndDate = '';
  page.value = 1;
  fetchList();
}

function handlePageChange(current: number) {
  page.value = current;
  fetchList();
}

function goDetail(ticketId: string) {
  router.push(`/resignation/ticket-detail/${ticketId}`);
}

async function handleApprove(row: ResignationTicket) {
  try {
    await ElMessageBox.confirm(
      `确定通过教练「${row.coachName}」的离职申请吗？`,
      '通过审批确认',
      {
        confirmButtonText: '确认通过',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    await approveResignationTicket(row.ticketId);
    ElMessage.success('审批通过');
    fetchList();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

async function handleReject(row: ResignationTicket) {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入驳回原因',
      '驳回审批确认',
      {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入驳回原因',
        type: 'warning',
      }
    );
    await rejectResignationTicket(row.ticketId, value);
    ElMessage.success('已驳回');
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
  <div class="resignation-approval-queue">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>用户管理</el-breadcrumb-item>
        <el-breadcrumb-item>教练离职审批</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="stat-cards">
      <div
        v-for="card in statCards"
        :key="card.label"
        class="stat-card"
        :style="{ backgroundColor: card.bgColor }"
      >
        <div class="stat-icon" :style="{ color: card.color }">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value" :style="{ color: card.color }">
            {{ card.value }}
          </div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <div class="filter-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="审批状态">
          <el-select
            v-model="queryForm.status"
            placeholder="全部状态"
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
        <el-form-item label="提交时间">
          <el-date-picker
            v-model="queryForm.submitStartDate"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 160px"
          />
          <span class="date-separator">至</span>
          <el-date-picker
            v-model="queryForm.submitEndDate"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="教练姓名"
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

    <div class="table-card">
      <el-skeleton v-if="loading" :rows="3" animated />
      <template v-else-if="error">
        <el-empty description="加载失败">
          <el-button type="primary" @click="fetchList">重试</el-button>
        </el-empty>
      </template>
      <template v-else-if="tableData.length === 0">
        <el-empty description="暂无离职申请">
          <el-button type="primary" @click="fetchList">刷新</el-button>
        </el-empty>
      </template>
      <el-table
        v-else
        :data="tableData"
        stripe
        header-row-class-name="table-header"
        style="width: 100%"
      >
        <el-table-column label="工单号" min-width="100">
          <template #default="{ row }">
            <span class="ticket-no">{{ row.ticketNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="教练姓名" min-width="100">
          <template #default="{ row }">
            <span class="coach-name" @click="goDetail(row.ticketId)">
              {{ row.coachName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="手机号" min-width="120">
          <template #default="{ row }">
            {{ maskPhone(row.coachPhone) }}
          </template>
        </el-table-column>
        <el-table-column label="在职时长" align="center" min-width="100">
          <template #default="{ row }">
            <span class="tenure-text">
              {{ calculateTenure(row.coachJoinedAt) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="当前学员数" align="center" min-width="100">
          <template #default="{ row }">
            <span :class="row.activeStudentCount > 0 ? 'text-danger' : ''">
              {{ row.activeStudentCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="离职原因" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="reason-text">{{ row.reason || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.submittedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="处理进度" align="center" min-width="180">
          <template #default="{ row }">
            <div class="progress-cell">
              <div class="progress-track">
                <div
                  class="progress-fill"
                  :style="{
                    width: `${calculateProgress(row.totalPackages, row.handledPackages)}%`,
                  }"
                />
              </div>
              <span class="progress-text">
                {{ calculateProgress(row.totalPackages, row.handledPackages) }}%
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" min-width="100">
          <template #default="{ row }">
            <el-tag
              :color="statusMap[row.status].bgColor"
              :style="{
                color: statusMap[row.status].color,
                borderColor: statusMap[row.status].bgColor,
              }"
              size="small"
              effect="light"
              round
            >
              {{ statusMap[row.status].label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          align="center"
          min-width="160"
          fixed="right"
          class-name="operation-cell"
        >
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.ticketId)">
              查看
            </el-button>
            <template v-if="row.status === 'pending_audit'">
              <el-button link type="primary" @click="handleApprove(row)">
                通过
              </el-button>
              <el-button link type="danger" @click="handleReject(row)">
                驳回
              </el-button>
            </template>
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
.resignation-approval-queue {
  padding-bottom: 24px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  height: 48px;
  margin-bottom: 16px;
}

.page-title {
  margin-bottom: 16px;
  font-size: 24px;
  font-weight: 700;
  line-height: 32px;
  color: #1d2129;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 4px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: #ffffff;
  border-radius: 50%;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #86909c;
}

.filter-card {
  padding: 16px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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

.ticket-no {
  font-size: 13px;
  color: #1890ff;
}

.coach-name {
  font-weight: 500;
  color: #1d2129;
  cursor: pointer;

  &:hover {
    color: #1890ff;
  }
}

.reason-text {
  display: inline-block;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenure-text {
  font-size: 14px;
  color: #262626;
}

.progress-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.progress-track {
  width: 100px;
  height: 6px;
  background: #f0f2f5;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #1890ff;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #86909c;
  white-space: nowrap;
}

.text-danger {
  color: #ff4d4f;
}

.text-muted {
  color: #86909c;
}

.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 8px 0;
}

.pagination-info {
  font-size: 14px;
  color: #86909c;

  strong {
    color: #1d2129;
  }
}

:deep(.table-header) {
  th {
    background: #f5f7fa;
  }
}

:deep(.el-table__cell.operation-cell .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  height: 100%;

  .el-button + .el-button {
    margin-left: 0;
  }
}
</style>
