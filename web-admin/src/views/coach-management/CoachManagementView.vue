<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { AdminCoachListItem } from '@/types/api';
import { getCoachList, cancelCoachEntry } from '@/api/coachManagement';
import { formatDate } from '@/utils/format';
import { ArrowDown } from '@element-plus/icons-vue';
import CoachEditModal from './CoachEditModal.vue';

const router = useRouter();

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: '0' },
  { label: '已通过', value: '1' },
  { label: '已驳回', value: '2' },
  { label: '已离职', value: '3' },
  { label: '申请离职中', value: '4' },
];

const realtimeStatusOptions = [
  { label: '全部', value: '' },
  { label: '空闲中', value: '空闲中' },
  { label: '上课中', value: '上课中' },
  { label: '休息中', value: '休息中' },
  { label: '已下班', value: '已下班' },
  { label: '请假中', value: '请假中' },
];

const coachStatusMap: Record<
  number,
  { label: string; color: string; bgColor: string }
> = {
  0: { label: '待审核', color: '#FAAD14', bgColor: '#FFFBE6' },
  1: { label: '已通过', color: '#52C41A', bgColor: '#F6FFED' },
  2: { label: '已驳回', color: '#FF4D4F', bgColor: '#FFF1F0' },
  3: { label: '已离职', color: '#8C8C8C', bgColor: '#F5F5F5' },
  4: { label: '申请离职中', color: '#1890FF', bgColor: '#E6F7FF' },
};

const realtimeStatusMap: Record<
  string,
  { label: string; color: string; bgColor: string }
> = {
  空闲中: { label: '空闲中', color: '#52C41A', bgColor: '#F6FFED' },
  上课中: { label: '上课中', color: '#1890FF', bgColor: '#E6F7FF' },
  休息中: { label: '休息中', color: '#FAAD14', bgColor: '#FFFBE6' },
  已下班: { label: '已下班', color: '#8C8C8C', bgColor: '#F5F5F5' },
  请假中: { label: '请假中', color: '#722ED1', bgColor: '#F9F0FF' },
};

const queryForm = reactive({
  status: '',
  realtimeStatus: '',
  keyword: '',
});

const tableData = ref<AdminCoachListItem[]>([]);
const loading = ref(false);
const error = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

const editVisible = ref(false);
const selectedCoachId = ref<number | null>(null);
const isEditMode = ref(false);

function formatGender(gender: string): string {
  return gender === 'MALE' ? '男' : gender === 'FEMALE' ? '女' : '-';
}

function formatTenure(tenure: string | undefined): string {
  return tenure || '-';
}

function formatStrokes(strokes: string | undefined): string[] {
  if (!strokes) return [];
  return strokes.split(/[,，]/).filter(Boolean);
}

async function fetchList() {
  loading.value = true;
  error.value = false;
  try {
    const res = await getCoachList({
      page: page.value,
      pageSize: pageSize.value,
      status: queryForm.status || undefined,
      realtimeStatus: queryForm.realtimeStatus || undefined,
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
  queryForm.realtimeStatus = '';
  queryForm.keyword = '';
  page.value = 1;
  fetchList();
}

function handlePageChange(current: number) {
  page.value = current;
  fetchList();
}

function openCreate() {
  isEditMode.value = false;
  selectedCoachId.value = null;
  editVisible.value = true;
}

function openEdit(row: AdminCoachListItem) {
  isEditMode.value = true;
  selectedCoachId.value = row.coachId;
  editVisible.value = true;
}

function openDetail(row: AdminCoachListItem) {
  router.push(`/coach-management/detail/${row.coachId}`);
}

function handleEditSuccess() {
  editVisible.value = false;
  selectedCoachId.value = null;
  ElMessage.success(isEditMode.value ? '教练资料已更新' : '教练已创建');
  fetchList();
}

function handleSchedule() {
  ElMessage.info('排班管理功能即将上线');
}

function handleViewStudents(row: AdminCoachListItem) {
  router.push({
    path: '/user-management',
    query: { coachId: String(row.coachId) },
  });
}

function handleViewLogs() {
  ElMessage.info('操作日志功能即将上线');
}

async function handleCancelEntry(row: AdminCoachListItem) {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入取消入驻原因，该操作将标记教练为已离职',
      '取消入驻',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入取消原因',
        type: 'warning',
      }
    );
    await cancelCoachEntry({ coachId: row.coachId, reason: value.trim() });
    ElMessage.success('已取消入驻');
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
  <div class="coach-management">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>用户管理</el-breadcrumb-item>
        <el-breadcrumb-item>教练管理</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="filter-card">
      <div class="filter-header">
        <el-form :model="queryForm" inline>
          <el-form-item label="在职状态">
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
          <el-form-item label="实时状态">
            <el-select
              v-model="queryForm.realtimeStatus"
              placeholder="全部状态"
              style="width: 160px"
              clearable
            >
              <el-option
                v-for="option in realtimeStatusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
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
        <el-button type="primary" @click="openCreate">新建教练</el-button>
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
        <el-empty description="暂无教练数据">
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
        <el-table-column label="教练 ID" prop="coachId" width="80" />
        <el-table-column label="姓名" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">
              {{ row.name || '未设置' }}
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
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.approvedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="在职时长" align="center" width="100">
          <template #default="{ row }">
            {{ formatTenure(row.tenure) }}
          </template>
        </el-table-column>
        <el-table-column label="在职状态" align="center" width="110">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: coachStatusMap[row.status]?.color,
                backgroundColor: coachStatusMap[row.status]?.bgColor,
              }"
            >
              {{ coachStatusMap[row.status]?.label || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="当前学员数"
          align="center"
          prop="currentStudentCount"
          width="100"
        />
        <el-table-column label="实时状态" align="center" width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: realtimeStatusMap[row.realtimeStatus]?.color,
                backgroundColor: realtimeStatusMap[row.realtimeStatus]?.bgColor,
              }"
            >
              {{
                realtimeStatusMap[row.realtimeStatus]?.label ||
                row.realtimeStatus ||
                '-'
              }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1 || row.status === 2 || row.status === 3"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 1 || row.status === 4"
              link
              type="primary"
              @click="handleSchedule(row)"
            >
              排班
            </el-button>
            <el-dropdown
              trigger="click"
              @command="
                (cmd: string) => {
                  if (cmd === 'cancel') handleCancelEntry(row);
                  else if (cmd === 'students') handleViewStudents(row);
                  else if (cmd === 'logs') handleViewLogs(row);
                }
              "
            >
              <el-button link type="primary">
                更多
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status === 1" command="cancel">
                    <span style="color: #ff4d4f">取消入驻</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="students"
                    >查看学员</el-dropdown-item
                  >
                  <el-dropdown-item command="logs">查看日志</el-dropdown-item>
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

    <CoachEditModal
      v-model:visible="editVisible"
      :coach-id="selectedCoachId"
      :is-edit="isEditMode"
      @success="handleEditSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
.coach-management {
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
  align-items: flex-start;
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
  justify-content: center;
  padding: 16px 8px 0;
}

:deep(.table-header) {
  th {
    background: #f5f7fa;
  }
}
</style>
