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
  { label: '处理中', value: 'processing' },
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
const overdueCount = computed(
  () =>
    tableData.value.filter(
      (item) => item.status === 'pending_audit' && isOverdue(item.submittedAt)
    ).length
);

function isOverdue(submittedAt: string): boolean {
  if (!submittedAt) return false;
  const submitTime = new Date(submittedAt).getTime();
  return Date.now() - submitTime > 3 * 24 * 60 * 60 * 1000;
}

function calculateProgress(total: number, handled: number): number {
  if (total <= 0) return 100;
  return Math.round((handled / total) * 100);
}

const statusMap: Record<
  ResignationStatus,
  { label: string; type: 'primary' | 'success' | 'danger' | 'warning' }
> = {
  processing: { label: '处理中', type: 'warning' },
  pending_audit: { label: '待审批', type: 'primary' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已驳回', type: 'danger' },
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

function handleSizeChange(size: number) {
  pageSize.value = size;
  page.value = 1;
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

function getRowClass({ row }: { row: ResignationTicket }) {
  return row.status === 'pending_audit' && isOverdue(row.submittedAt)
    ? 'overdue-row'
    : '';
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

    <div class="summary-bar">
      待审批：{{ pendingCount }} 条 · 超 3 工作日未处理：{{ overdueCount }} 条
    </div>

    <div class="filter-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="审批状态">
          <el-select
            v-model="queryForm.status"
            placeholder="请选择"
            style="width: 160px"
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
        :row-class-name="getRowClass"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="ticketNo" label="工单号" min-width="100" />
        <el-table-column label="教练姓名" min-width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.ticketId)">
              {{ row.coachName }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="手机号" min-width="120">
          <template #default="{ row }">
            {{ maskPhone(row.coachPhone) }}
          </template>
        </el-table-column>
        <el-table-column label="当前学员数" align="center" min-width="100">
          <template #default="{ row }">
            {{ row.totalPackages }}
          </template>
        </el-table-column>
        <el-table-column label="离职原因" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tooltip :content="row.reason" placement="top" :show-after="300">
              <span class="reason-text">{{ row.reason }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.submittedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="处理进度" align="center" min-width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="
                calculateProgress(row.totalPackages, row.handledPackages)
              "
              :stroke-width="8"
              style="width: 120px"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status].type" effect="light" round>
              {{ statusMap[row.status].label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          align="center"
          min-width="160"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.ticketId)"
              >查看</el-button
            >
            <template v-if="row.status === 'pending_audit'">
              <el-button link type="primary" @click="handleApprove(row)"
                >通过</el-button
              >
              <el-button link type="danger" @click="handleReject(row)"
                >驳回</el-button
              >
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
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
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

.summary-bar {
  padding: 16px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #262626;
  background: #e6f7ff;
  border-radius: 4px;
}

.filter-card {
  padding: 16px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 4px;
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
}

.reason-text {
  display: inline-block;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding-top: 16px;
}

:deep(.overdue-row) {
  background-color: #fffbe6;
}
</style>
