<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listResignationTickets,
  approveResignationTicket,
  rejectResignationTicket,
} from '@/api/resignation';
import type { ResignationTicket, ResignationStatus } from '@/types/resignation';
import { maskPhone, formatDateTime } from '@/utils/format';

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

const pendingCount = computed(
  () => tableData.value.filter((item) => item.status === 'pending_audit').length
);
const approvedTodayCount = computed(
  () =>
    tableData.value.filter((item) => {
      if (item.status !== 'approved' || !item.submittedAt) return false;
      const submitDate = new Date(item.submittedAt).toDateString();
      return submitDate === new Date().toDateString();
    }).length
);
const rejectedTodayCount = computed(
  () =>
    tableData.value.filter((item) => {
      if (item.status !== 'rejected' || !item.submittedAt) return false;
      const submitDate = new Date(item.submittedAt).toDateString();
      return submitDate === new Date().toDateString();
    }).length
);
const totalResignedCount = computed(
  () => tableData.value.filter((item) => item.status === 'approved').length
);

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

    <div class="page-title">教练离职审批</div>

    <div class="summary-bar">
      <div class="summary-card summary-card--pending">
        <div class="summary-icon">
          <i class="ri-time-line" />
        </div>
        <div class="summary-info">
          <div class="summary-value">{{ pendingCount }}</div>
          <div class="summary-label">待审批</div>
        </div>
      </div>
      <div class="summary-card summary-card--approved">
        <div class="summary-icon">
          <i class="ri-check-double-line" />
        </div>
        <div class="summary-info">
          <div class="summary-value">{{ approvedTodayCount }}</div>
          <div class="summary-label">今日通过</div>
        </div>
      </div>
      <div class="summary-card summary-card--rejected">
        <div class="summary-icon">
          <i class="ri-close-circle-line" />
        </div>
        <div class="summary-info">
          <div class="summary-value">{{ rejectedTodayCount }}</div>
          <div class="summary-label">今日拒绝</div>
        </div>
      </div>
      <div class="summary-card summary-card--total">
        <div class="summary-icon">
          <i class="ri-user-unfollow-line" />
        </div>
        <div class="summary-info">
          <div class="summary-value">{{ totalResignedCount }}</div>
          <div class="summary-label">累计离职</div>
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
      <el-table v-else :data="tableData" stripe header-row-class-name="table-header" style="width: 100%">
        <el-table-column label="工单号" width="100">
          <template #default="{ row }">
            <span class="ticket-no">{{ row.ticketNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="教练姓名" width="100">
          <template #default="{ row }">
            <span class="coach-name">{{ row.coachName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="120">
          <template #default="{ row }">
            {{ maskPhone(row.coachPhone) }}
          </template>
        </el-table-column>
        <el-table-column label="在职时长" align="center" width="100">
          <template #default>
            <span class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="当前学员数" align="center" width="100">
          <template #default="{ row }">
            <span :class="row.totalPackages > 0 ? 'text-danger' : ''">
              {{ row.totalPackages }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="离职原因" width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="reason-text">{{ row.reason || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.submittedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="处理进度" align="center" width="180">
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
        <el-table-column label="状态" align="center" width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: statusMap[row.status].color,
                backgroundColor: statusMap[row.status].bgColor,
              }"
            >
              {{ statusMap[row.status].label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="160" fixed="right">
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
        <div class="pagination-info">
          共 <strong>{{ total }}</strong> 条待审批
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

.summary-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  font-size: 22px;
}

.summary-card--pending .summary-icon {
  color: #faad14;
  background: rgba(250, 173, 20, 0.1);
}

.summary-card--approved .summary-icon {
  color: #52c41a;
  background: rgba(82, 196, 26, 0.1);
}

.summary-card--rejected .summary-icon {
  color: #ff4d4f;
  background: rgba(255, 77, 79, 0.1);
}

.summary-card--total .summary-icon {
  color: #8c8c8c;
  background: rgba(140, 140, 140, 0.1);
}

.summary-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 29px;
}

.summary-card--pending .summary-value {
  color: #faad14;
}

.summary-card--approved .summary-value {
  color: #52c41a;
}

.summary-card--rejected .summary-value {
  color: #ff4d4f;
}

.summary-card--total .summary-value {
  color: #8c8c8c;
}

.summary-label {
  font-size: 13px;
  line-height: 16px;
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
}

.reason-text {
  display: inline-block;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  padding: 0 10px;
  font-size: 12px;
  border-radius: 12px;
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
  justify-content: space-between;
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
</style>
