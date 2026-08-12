<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
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
const stats = ref({ pendingCount: 0, todayNewCount: 0, overdue24hCount: 0 });

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

function formatGender(gender: string): string {
  if (gender === 'male') return '男';
  if (gender === 'female') return '女';
  return gender || '-';
}

function formatStrokes(strokes: string | string[]): string {
  if (!strokes || (Array.isArray(strokes) && strokes.length === 0)) return '-';
  return Array.isArray(strokes) ? strokes.join('、') : strokes;
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
      stats.value = res.data;
    }
  } catch {
    // 若后端未提供统计接口，则使用当前列表数据兜底
    stats.value = {
      pendingCount: tableData.value.filter((item) => item.status === 'pending')
        .length,
      todayNewCount: tableData.value.filter((item) => {
        if (!item.submittedAt) return false;
        return (
          new Date(item.submittedAt).toDateString() ===
          new Date().toDateString()
        );
      }).length,
      overdue24hCount: tableData.value.filter((item) => {
        if (item.status !== 'pending' || !item.submittedAt) return false;
        return (
          Date.now() - new Date(item.submittedAt).getTime() >
          24 * 60 * 60 * 1000
        );
      }).length,
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

async function handleReject(row: CoachApplicationItem) {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入驳回原因，教练将收到该原因',
      '驳回入驻申请',
      {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入驳回原因',
        type: 'warning',
      }
    );
    await rejectCoachApplication(row.applicationId, value.trim());
    ElMessage.success('已驳回');
    fetchList();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

const BATCH_APPROVE_SIZE = 5;

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
      待审核：{{ stats.pendingCount }} 条 · 今日新增：{{
        stats.todayNewCount
      }}
      条 · 超 24h 未处理：{{ stats.overdue24hCount }} 条
    </div>

    <div class="filter-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="审核状态">
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
            placeholder="姓名 / 手机号"
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
      <div v-if="hasSelectedPending" class="batch-bar">
        <span>已选 {{ pendingRows.length }} 条</span>
        <div class="batch-actions">
          <el-button @click="selectedRows = []">取消选择</el-button>
          <el-button type="primary" @click="handleBatchApprove"
            >批量通过</el-button
          >
        </div>
      </div>

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
        stripe
        header-row-class-name="table-header"
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
        <el-table-column
          label="擅长"
          align="center"
          width="120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ formatStrokes(row.teachingStrokes) }}
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
        <el-table-column label="入驻类型" align="center" width="140">
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
              <el-button link type="danger" @click="handleReject(row)">
                驳回
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="tableData.length > 0" class="pagination-wrap">
        <div class="pagination-info">
          共 <strong>{{ total }}</strong> 条
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
