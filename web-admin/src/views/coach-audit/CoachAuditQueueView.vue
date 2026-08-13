<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  User,
  CircleCheck,
  CircleClose,
  TrendCharts,
} from '@element-plus/icons-vue';
import type {
  CoachApplicationItem,
  CoachAuditStatus,
} from '@/types/coachAudit';
import {
  listCoachApplications,
  approveCoachApplication,
  rejectCoachApplication,
  getCoachAuditStats,
} from '@/api/coachAudit';
import { formatDateTime } from '@/utils/format';

const router = useRouter();

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
];

const queryForm = reactive({
  status: 'pending' as CoachAuditStatus | '',
  keyword: '',
  submitStartDate: '',
  submitEndDate: '',
});

const tableData = ref<CoachApplicationItem[]>([]);
const loading = ref(false);
const error = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const selectedRows = ref<CoachApplicationItem[]>([]);
const stats = ref({
  pendingCount: 0,
  todayNewCount: 0,
  overdue24hCount: 0,
  todayApprovedCount: 0,
  todayRejectedCount: 0,
});

const statusMap: Record<
  CoachAuditStatus,
  { label: string; color: string; bgColor: string }
> = {
  pending: { label: '待审核', color: '#1890FF', bgColor: '#E6F7FF' },
  approved: { label: '已通过', color: '#52C41A', bgColor: '#F6FFED' },
  rejected: { label: '已驳回', color: '#FF4D4F', bgColor: '#FFF1F0' },
};

const entryTypeMap: Record<number, string> = {
  [-1]: '首次入驻',
  2: '已驳回重新入驻',
  3: '已离职重新入驻',
};

const pendingRows = computed(() =>
  selectedRows.value.filter((row) => row.status === 'pending')
);

const hasSelectedPending = computed(() => pendingRows.value.length > 0);

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

const todayApprovedCount = computed(() => {
  if (stats.value.todayApprovedCount !== undefined) {
    return stats.value.todayApprovedCount;
  }
  return tableData.value.filter(
    (item) => item.status === 'approved' && isToday(item.submittedAt)
  ).length;
});

const todayRejectedCount = computed(() => {
  if (stats.value.todayRejectedCount !== undefined) {
    return stats.value.todayRejectedCount;
  }
  return tableData.value.filter(
    (item) => item.status === 'rejected' && isToday(item.submittedAt)
  ).length;
});

const statCards = computed(() => [
  {
    label: '待审核',
    value: stats.value.pendingCount,
    color: '#1890FF',
    bgColor: '#E6F7FF',
    icon: User,
  },
  {
    label: '今日通过',
    value: todayApprovedCount.value,
    color: '#52C41A',
    bgColor: '#F6FFED',
    icon: CircleCheck,
  },
  {
    label: '今日驳回',
    value: todayRejectedCount.value,
    color: '#FF4D4F',
    bgColor: '#FFF1F0',
    icon: CircleClose,
  },
  {
    label: '累计入驻',
    value: total.value,
    color: '#262626',
    bgColor: '#F5F5F5',
    icon: TrendCharts,
  },
]);

function formatGender(gender: string): string {
  if (gender === 'male') return '男';
  if (gender === 'female') return '女';
  return gender || '-';
}

function formatStrokes(strokes: string | string[] | undefined): string[] {
  if (!strokes) return [];
  if (Array.isArray(strokes)) return strokes;
  return strokes.split(/[,，]/).filter(Boolean);
}

function formatEntryType(previousCoachStatus: number): string {
  return entryTypeMap[previousCoachStatus] || '其他';
}

function selectable(row: CoachApplicationItem): boolean {
  return row.status === 'pending';
}

async function fetchStats() {
  try {
    const res = await getCoachAuditStats();
    if (res.data) {
      stats.value = {
        ...stats.value,
        ...res.data,
      };
    }
  } catch {
    stats.value = {
      pendingCount: tableData.value.filter((item) => item.status === 'pending')
        .length,
      todayNewCount: tableData.value.filter((item) => {
        if (!item.submittedAt) return false;
        return isToday(item.submittedAt);
      }).length,
      overdue24hCount: tableData.value.filter((item) => {
        if (item.status !== 'pending' || !item.submittedAt) return false;
        return (
          Date.now() - new Date(item.submittedAt).getTime() >
          24 * 60 * 60 * 1000
        );
      }).length,
      todayApprovedCount: tableData.value.filter(
        (item) => item.status === 'approved' && isToday(item.submittedAt)
      ).length,
      todayRejectedCount: tableData.value.filter(
        (item) => item.status === 'rejected' && isToday(item.submittedAt)
      ).length,
    };
  }
}

async function fetchList() {
  loading.value = true;
  error.value = false;
  try {
    const res = await listCoachApplications({
      page: page.value,
      pageSize: pageSize.value,
      status: queryForm.status || undefined,
      keyword: queryForm.keyword || undefined,
      submitStartDate: queryForm.submitStartDate || undefined,
      submitEndDate: queryForm.submitEndDate || undefined,
    });
    if (res.data) {
      tableData.value = res.data.list;
      total.value = res.data.total;
      page.value = res.data.page;
      pageSize.value = res.data.pageSize;
    }
    selectedRows.value = [];
    await fetchStats();
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
  queryForm.status = 'pending';
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

function goDetail(applicationId: number) {
  router.push(`/coach-audit/detail/${applicationId}`);
}

async function handleApprove(row: CoachApplicationItem) {
  try {
    await ElMessageBox.confirm(
      '确认通过该教练入驻申请？通过后将立即生效且不可撤销',
      '确认通过',
      {
        confirmButtonText: '确认通过',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    await approveCoachApplication(row.applicationId);
    ElMessage.success('操作成功');
    fetchList();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

function openRejectDialog(row: CoachApplicationItem) {
  rejectTargetId.value = row.applicationId;
  rejectReason.value = '';
  rejectVisible.value = true;
}

function handleRejectReasonSelect(reason: string) {
  rejectReason.value = reason;
}

function closeRejectDialog() {
  rejectVisible.value = false;
  rejectTargetId.value = null;
  rejectReason.value = '';
}

async function handleRejectSubmit() {
  if (!rejectTargetId.value || !rejectReason.value.trim()) return;
  rejectLoading.value = true;
  try {
    await rejectCoachApplication(
      rejectTargetId.value,
      rejectReason.value.trim()
    );
    ElMessage.success('已驳回');
    closeRejectDialog();
    fetchList();
  } catch (err) {
    if (err instanceof Error) {
      ElMessage.error(err.message);
    }
  } finally {
    rejectLoading.value = false;
  }
}

const BATCH_APPROVE_SIZE = 5;

const quickRejectReasons = [
  '资料不全',
  '证书不清晰',
  '信息不一致',
  '单价不合理',
];
const rejectVisible = ref(false);
const rejectReason = ref('');
const rejectLoading = ref(false);
const rejectTargetId = ref<number | null>(null);

async function handleBatchApprove() {
  if (!hasSelectedPending.value) return;
  const count = pendingRows.value.length;
  try {
    await ElMessageBox.confirm(
      `确认通过已选 ${count} 条入驻申请？通过后将立即生效且不可撤销`,
      '批量通过确认',
      {
        confirmButtonText: '确认通过',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    const results: PromiseSettledResult<void>[] = [];
    for (let i = 0; i < pendingRows.value.length; i += BATCH_APPROVE_SIZE) {
      const batch = pendingRows.value.slice(i, i + BATCH_APPROVE_SIZE);
      const batchResults = await Promise.all(
        batch.map((row) =>
          approveCoachApplication(row.applicationId).then(
            () => ({ status: 'fulfilled' as const, value: undefined }),
            (reason: unknown) => ({ status: 'rejected' as const, reason })
          )
        )
      );
      results.push(...batchResults);
    }

    const failed = results.filter((r) => r.status === 'rejected').length;
    const success = results.length - failed;
    ElMessage.success(
      `成功通过 ${success} 条${failed > 0 ? `，失败 ${failed} 条` : ''}`
    );
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
  <div class="coach-audit-queue">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>用户管理</el-breadcrumb-item>
        <el-breadcrumb-item>教练入驻审核</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="summary-bar">
      <div
        v-for="card in statCards"
        :key="card.label"
        class="summary-card"
        :style="{ backgroundColor: card.bgColor }"
      >
        <div
          class="summary-icon"
          :style="{ color: card.color, backgroundColor: '#ffffff' }"
        >
          <el-icon :size="22">
            <component :is="card.icon" />
          </el-icon>
        </div>
        <div class="summary-info">
          <div class="summary-value" :style="{ color: card.color }">
            {{ card.value }}
          </div>
          <div class="summary-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <div class="filter-card">
      <el-form :model="queryForm" inline class="filter-form" label-width="0">
        <div class="filter-row">
          <el-form-item>
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
          <el-form-item>
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
          <el-form-item>
            <el-input
              v-model="queryForm.keyword"
              placeholder="姓名 / 手机号"
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
        <el-empty description="暂无入驻申请">
          <el-button type="primary" @click="fetchList">刷新</el-button>
        </el-empty>
      </template>
      <el-table
        v-else
        :data="tableData"
        header-row-class-name="table-header"
        row-class-name="table-row"
        style="width: 100%"
        @selection-change="
          (val: CoachApplicationItem[]) => (selectedRows = val)
        "
      >
        <el-table-column
          type="selection"
          width="48"
          align="center"
          :selectable="selectable"
        />
        <el-table-column label="教练 ID" prop="coachId" width="80" />
        <el-table-column label="姓名" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.applicationId)">
              {{ row.name }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="性别" align="center" width="60">
          <template #default="{ row }">
            {{ formatGender(row.gender) }}
          </template>
        </el-table-column>
        <el-table-column label="年龄" align="center" prop="age" width="60" />
        <el-table-column label="教学年限" align="center" width="100">
          <template #default="{ row }"> {{ row.teachingYears }} 年 </template>
        </el-table-column>
        <el-table-column label="擅长" align="center" width="120">
          <template #default="{ row }">
            <div class="stroke-tags">
              <el-tag
                v-for="stroke in formatStrokes(row.teachingStrokes)"
                :key="stroke"
                size="small"
                class="stroke-tag"
              >
                {{ stroke }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.submittedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="流程状态" align="center" width="100">
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
        <el-table-column label="入驻类型" align="center" width="120">
          <template #default="{ row }">
            {{ formatEntryType(row.previousCoachStatus) }}
          </template>
        </el-table-column>
        <el-table-column label="最新申请 ID" prop="applicationId" width="100" />
        <el-table-column label="操作" align="center" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.applicationId)">
              查看
            </el-button>
            <template v-if="row.status === 'pending'">
              <el-button link type="primary" @click="handleApprove(row)">
                通过
              </el-button>
              <el-button link type="danger" @click="openRejectDialog(row)">
                驳回
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="hasSelectedPending" class="batch-bar">
        <span>已选 {{ pendingRows.length }} 条</span>
        <div class="batch-actions">
          <el-button @click="selectedRows = []">取消选择</el-button>
          <el-button type="primary" @click="handleBatchApprove"
            >批量通过</el-button
          >
        </div>
      </div>

      <div v-if="tableData.length > 0" class="pagination-wrap">
        <div class="pagination-total">
          共
          <span class="total-number">{{ total.toLocaleString() }}</span>
          条待审核
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

    <el-dialog
      v-model="rejectVisible"
      title="驳回入驻申请"
      width="360px"
      :close-on-click-modal="false"
      destroy-on-close
      @close="closeRejectDialog"
    >
      <div class="reject-dialog-body">
        <div class="quick-reasons">
          <span
            v-for="reason in quickRejectReasons"
            :key="reason"
            class="quick-reason-tag"
            :class="{ active: rejectReason === reason }"
            @click="handleRejectReasonSelect(reason)"
          >
            {{ reason }}
          </span>
        </div>
        <el-input
          v-model="rejectReason"
          type="textarea"
          :rows="3"
          placeholder="请输入驳回原因，教练将收到该原因"
          maxlength="200"
          show-word-limit
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeRejectDialog">取消</el-button>
          <el-button
            type="danger"
            :loading="rejectLoading"
            :disabled="!rejectReason.trim()"
            @click="handleRejectSubmit"
          >
            确认驳回
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.coach-audit-queue {
  padding-bottom: 24px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  height: 48px;
  margin-bottom: 16px;
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
  gap: 12px;
  padding: 16px;
  border-radius: 4px;
}

.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
}

.summary-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1;
}

.summary-label {
  font-size: 13px;
  color: #595959;
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

.filter-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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

.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.batch-actions {
  display: flex;
  gap: 12px;
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

.stroke-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
}

.stroke-tag {
  margin: 0;
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

:deep(.el-table__body) {
  .el-table__row {
    td {
      height: 56px;
    }
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

.reject-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quick-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-reason-tag {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  font-size: 13px;
  color: #595959;
  background: #f5f5f5;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;

  &.active,
  &:hover {
    color: #1890ff;
    background: #e6f7ff;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
