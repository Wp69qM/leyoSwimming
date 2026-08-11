<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import {
  getResignationTicketDetail,
  approveResignationTicket,
  rejectResignationTicket,
} from '@/api/resignation';
import type {
  ResignationTicketDetail,
  ResignationChecklist,
  ResignationStatus,
  StudentHandleResult,
  ResignationPackageItem,
} from '@/types/resignation';
import { maskPhone, formatDateTime } from '@/utils/format';

const route = useRoute();
const router = useRouter();

const ticketId = computed(() => String(route.params.ticketId));

const detail = ref<ResignationTicketDetail | null>(null);
const loading = ref(false);
const error = ref(false);
const approvalComment = ref('');
const checklist = ref<ResignationChecklist>({
  activeStudentsCleared: false,
  allActionsRegistered: false,
  scheduleCleared: false,
  settlementCompleted: false,
});

const statusMap: Record<
  ResignationStatus,
  { label: string; type: 'primary' | 'success' | 'danger' | 'warning'; color: string; bgColor: string }
> = {
  processing: { label: '处理中', type: 'warning', color: '#FAAD14', bgColor: '#FFFBE6' },
  pending_audit: { label: '待审批', type: 'primary', color: '#1890FF', bgColor: '#E6F7FF' },
  approved: { label: '已通过', type: 'success', color: '#52C41A', bgColor: '#F6FFED' },
  rejected: { label: '已驳回', type: 'danger', color: '#FF4D4F', bgColor: '#FFF1F0' },
};

const resultMap: Record<Exclude<StudentHandleResult, null>, { label: string; color: string; bgColor: string }> = {
  transfer: { label: '转新教练', color: '#1890FF', bgColor: '#E6F7FF' },
  refund: { label: '全额退款', color: '#FF4D4F', bgColor: '#FFF1F0' },
  continue: { label: '继续上完', color: '#52C41A', bgColor: '#F6FFED' },
};

const allChecklistPassed = computed(() => {
  return (
    checklist.value.activeStudentsCleared &&
    checklist.value.allActionsRegistered &&
    checklist.value.scheduleCleared &&
    checklist.value.settlementCompleted
  );
});

const activeStudentCount = computed(() => detail.value?.packages.length ?? 0);

const totalHours = computed(
  () =>
    detail.value?.packages.reduce((sum, pkg) => sum + pkg.totalHours, 0) ?? 0
);

const unpassedItems = computed(() => {
  const items: string[] = [];
  if (!checklist.value.activeStudentsCleared) items.push('Active 学员数清零');
  if (!checklist.value.allActionsRegistered) items.push('登记所有学员处理结果');
  if (!checklist.value.settlementCompleted) items.push('完成教练费结算');
  if (!checklist.value.scheduleCleared) items.push('清空未来排班');
  return items;
});

function getResultDisplay(row: ResignationPackageItem) {
  if (!row.action) {
    return { label: '待处理', color: '#C9CDD4', bgColor: 'transparent', isTag: false };
  }
  return { ...resultMap[row.action], isTag: true };
}

function applyAutoChecklist() {
  if (!detail.value) return;
  checklist.value = {
    activeStudentsCleared: detail.value.packages.length === 0,
    allActionsRegistered:
      detail.value.packages.length === 0 ||
      detail.value.packages.every((pkg) => !!pkg.action),
    scheduleCleared: detail.value.scheduleCleared,
    settlementCompleted: detail.value.settlementStatus === 1,
  };
}

interface TimelineNode {
  title: string;
  time: string;
  done: boolean;
  active: boolean;
}

const timelineNodes = computed<TimelineNode[]>(() => {
  if (!detail.value) return [];
  const { status, submittedAt, packages, handledPackages } = detail.value;
  const submitted = formatDateTime(submittedAt);

  const studentProcessingDone = packages.length === 0 || handledPackages === packages.length;
  const studentProcessingActive = packages.length > 0 && handledPackages < packages.length;

  const checklistDone = allChecklistPassed.value;
  const checklistActive = studentProcessingDone && !checklistDone && status === 'pending_audit';

  const approvedDone = status === 'approved';
  const rejectedDone = status === 'rejected';
  const finalDone = approvedDone || rejectedDone;

  return [
    { title: '提交离职', time: submitted, done: true, active: false },
    {
      title: '学员处理登记',
      time: studentProcessingDone ? submitted : '进行中',
      done: studentProcessingDone,
      active: studentProcessingActive,
    },
    {
      title: '检查通过',
      time: checklistDone ? submitted : '-',
      done: checklistDone,
      active: checklistActive,
    },
    {
      title: rejectedDone ? '管理员驳回' : '管理员通过',
      time: finalDone ? submitted : '-',
      done: finalDone,
      active: false,
    },
  ];
});

async function fetchDetail() {
  loading.value = true;
  error.value = false;
  const loader = ElLoading.service({ lock: true, text: '加载中...' });
  try {
    const res = await getResignationTicketDetail(ticketId.value);
    if (res.data) {
      detail.value = res.data;
      applyAutoChecklist();
    } else {
      error.value = true;
    }
  } catch (err) {
    error.value = true;
    detail.value = null;
    ElMessage.error(err instanceof Error ? err.message : '加载失败');
  } finally {
    loader.close();
    loading.value = false;
  }
}

function goBack() {
  router.push('/resignation/approval-queue');
}

function goCoachDetail() {
  if (!detail.value) return;
  router.push(`/coach/detail/${detail.value.coach.coachId}`);
}

async function handleApprove() {
  if (!detail.value) return;
  if (!approvalComment.value.trim()) {
    ElMessage.warning('请输入审批意见');
    return;
  }
  const message = `教练：${detail.value.coach.name}\n工单号：${detail.value.ticketNo}\n审批结论：通过\n\n审批意见：${approvalComment.value.trim()}`;
  try {
    await ElMessageBox.confirm(message, '通过审批确认', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await approveResignationTicket(
      detail.value.ticketId,
      approvalComment.value.trim()
    );
    ElMessage.success('审批通过，教练已离职');
    router.push('/resignation/approval-queue');
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

async function handleReject() {
  if (!detail.value) return;
  if (!approvalComment.value.trim()) {
    ElMessage.warning('请输入审批意见');
    return;
  }
  const message = `教练：${detail.value.coach.name}\n工单号：${detail.value.ticketNo}\n审批结论：驳回\n\n驳回原因：${approvalComment.value.trim()}`;
  try {
    await ElMessageBox.confirm(message, '驳回审批确认', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await rejectResignationTicket(
      detail.value.ticketId,
      approvalComment.value.trim()
    );
    ElMessage.success('已驳回');
    router.push('/resignation/approval-queue');
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

function openResultDialog() {
  ElMessage.info('学员处理结果由教练在教练端登记');
}

watch(
  () => route.params.ticketId,
  () => {
    approvalComment.value = '';
    fetchDetail();
  },
  { immediate: true }
);

onMounted(() => {
  if (!detail.value) fetchDetail();
});
</script>

<template>
  <div v-if="error" class="error-page">
    <el-empty description="工单不存在">
      <el-button type="primary" @click="goBack">返回列表</el-button>
    </el-empty>
  </div>

  <div v-else-if="detail" class="resignation-ticket-detail">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>用户管理</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/resignation/approval-queue' }">
          教练离职审批
        </el-breadcrumb-item>
        <el-breadcrumb-item>工单详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="page-title-row">
      <div class="back-btn" @click="goBack">
        <i class="ri-arrow-left-line" />
      </div>
      <h1 class="page-title">工单详情</h1>
    </div>

    <div class="info-card">
      <div class="coach-header">
        <div class="coach-header-left">
          <div class="coach-avatar">
            {{ detail.coach.name.charAt(0) }}
          </div>
          <div class="coach-meta">
            <div class="coach-name-row">
              <span class="coach-name">{{ detail.coach.name }}</span>
              <span
                class="status-tag"
                :style="{
                  color: statusMap[detail.status].color,
                  backgroundColor: statusMap[detail.status].bgColor,
                }"
              >
                {{ statusMap[detail.status].label }}
              </span>
            </div>
            <div class="ticket-no">工单号：{{ detail.ticketNo }}</div>
          </div>
        </div>
        <el-button class="coach-detail-btn" @click="goCoachDetail">
          <i class="ri-file-list-line" />
          查看教练详情
        </el-button>
      </div>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">手机号</span>
          <span class="info-value">{{ maskPhone(detail.coach.phone) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">入职时间</span>
          <span class="info-value">
            {{ detail.coach.joinedAt ? formatDateTime(detail.coach.joinedAt).split(' ')[0] : '-' }}
          </span>
        </div>
        <div class="info-item">
          <span class="info-label">提交离职时间</span>
          <span class="info-value">{{ formatDateTime(detail.coach.submittedAt) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">Active 学员数</span>
          <span class="info-value" :class="activeStudentCount > 0 ? 'text-danger' : ''">
            {{ activeStudentCount }}
          </span>
        </div>
        <div class="info-item">
          <span class="info-label">累计课时</span>
          <span class="info-value">{{ totalHours }}节</span>
        </div>
        <div class="info-item info-item--wide">
          <span class="info-label">离职原因</span>
          <span class="info-value">{{ detail.reason || '-' }}</span>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="card-title">学员处理清单</div>
      <el-table
        v-if="detail.packages.length > 0"
        :data="detail.packages"
        style="width: 100%"
        header-row-class-name="table-header"
      >
        <el-table-column label="学员" width="180">
          <template #default="{ row }">
            <span class="student-name">{{ row.userName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="套餐编号" width="160">
          <template #default="{ row }">
            <span class="package-no">{{ row.packageNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余课时" align="center" width="120">
          <template #default="{ row }">
            {{ row.availableCount + row.reservedCount }}节
          </template>
        </el-table-column>
        <el-table-column label="处理结果" align="center" width="160">
          <template #default="{ row }">
            <span
              v-if="getResultDisplay(row).isTag"
              class="result-tag"
              :style="{
                color: getResultDisplay(row).color,
                backgroundColor: getResultDisplay(row).bgColor,
              }"
            >
              {{ getResultDisplay(row).label }}
            </span>
            <span v-else class="text-muted">{{ getResultDisplay(row).label }}</span>
          </template>
        </el-table-column>
        <el-table-column label="处理人" align="center" width="140">
          <template #default="{ row }">
            <span class="text-muted">{{ row.handlerName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="处理时间" align="center" width="180">
          <template #default="{ row }">
            <span class="text-muted">{{ row.handledAt ? formatDateTime(row.handledAt) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="detail.status === 'pending_audit'"
          label="操作"
          align="center"
          min-width="100"
          fixed="right"
        >
          <template #default>
            <el-button link type="primary" @click="openResultDialog()">登记</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="无 active 学员" />
    </div>

    <div class="content-card">
      <div class="card-title">管理员检查清单（全部通过才可批准）</div>
      <div class="checklist">
        <div class="checklist-item">
          <el-checkbox
            v-model="checklist.activeStudentsCleared"
            :disabled="detail.status !== 'pending_audit'"
          >
            Active 学员数 = 0
          </el-checkbox>
          <span
            class="checklist-status"
            :class="checklist.activeStudentsCleared ? 'text-success' : 'text-danger'"
          >
            {{ checklist.activeStudentsCleared ? '已通过' : `当前仍有 ${activeStudentCount} 名学员未处理` }}
          </span>
        </div>
        <div class="checklist-item">
          <el-checkbox
            v-model="checklist.allActionsRegistered"
            :disabled="detail.status !== 'pending_audit'"
          >
            所有学员处理结果已登记
          </el-checkbox>
          <span
            class="checklist-status"
            :class="checklist.allActionsRegistered ? 'text-success' : 'text-danger'"
          >
            {{ checklist.allActionsRegistered ? '已通过' : `还有 ${activeStudentCount - detail.handledPackages} 名学员待处理` }}
          </span>
        </div>
        <div class="checklist-item">
          <el-checkbox
            v-model="checklist.settlementCompleted"
            :disabled="detail.status !== 'pending_audit'"
          >
            教练费已结算
          </el-checkbox>
          <span
            class="checklist-status"
            :class="checklist.settlementCompleted ? 'text-success' : 'text-danger'"
          >
            {{ checklist.settlementCompleted ? '已结算' : '未结算' }}
          </span>
        </div>
        <div class="checklist-item">
          <el-checkbox
            v-model="checklist.scheduleCleared"
            :disabled="detail.status !== 'pending_audit'"
          >
            排班未来时段已清空
          </el-checkbox>
          <span
            class="checklist-status"
            :class="checklist.scheduleCleared ? 'text-success' : 'text-danger'"
          >
            {{ checklist.scheduleCleared ? '已清空' : '未清空' }}
          </span>
        </div>
      </div>
      <div
        v-if="!allChecklistPassed && detail.status === 'pending_audit'"
        class="checklist-hint"
      >
        请先完成以下检查项：{{ unpassedItems.join('、') }}
      </div>
    </div>

    <div class="content-card">
      <div class="card-title">审批记录</div>
      <div class="timeline">
        <template v-for="(node, index) in timelineNodes" :key="node.title">
          <div class="timeline-node" :class="{ 'timeline-node--done': node.done, 'timeline-node--active': node.active }">
            <div class="timeline-dot" />
            <div class="timeline-title">{{ node.title }}</div>
            <div class="timeline-time">{{ node.time }}</div>
          </div>
          <div
            v-if="index < timelineNodes.length - 1"
            class="timeline-line"
            :class="{ 'timeline-line--done': node.done }"
          />
        </template>
      </div>
    </div>

    <div class="content-card action-card">
      <div class="card-title">审批操作</div>
      <div class="approval-form">
        <div class="approval-form-label">审批意见</div>
        <el-input
          v-model="approvalComment"
          type="textarea"
          :rows="4"
          placeholder="请输入审批意见（必填）"
          :disabled="detail.status !== 'pending_audit'"
        />
      </div>
      <div class="action-bar">
        <template v-if="detail.status === 'pending_audit'">
          <el-button
            type="primary"
            size="large"
            :disabled="!allChecklistPassed"
            @click="handleApprove"
          >
            <i class="ri-check-line" />
            审核通过
          </el-button>
          <el-button type="danger" size="large" @click="handleReject">
            <i class="ri-close-line" />
            驳回申请
          </el-button>
        </template>
        <span v-else class="closed-status">
          <span
            class="status-tag"
            :style="{
              color: statusMap[detail.status].color,
              backgroundColor: statusMap[detail.status].bgColor,
            }"
          >
            {{ statusMap[detail.status].label }}
          </span>
        </span>
        <div class="action-bar-spacer" />
        <el-button size="large" @click="goBack">
          <i class="ri-arrow-go-back-line" />
          返回列表
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.resignation-ticket-detail {
  padding-bottom: 24px;
}

.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  height: 48px;
  margin-bottom: 16px;
}

.page-title-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  font-size: 18px;
  color: #4e5969;
  cursor: pointer;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  &:hover {
    color: #1890ff;
  }
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1d2129;
}

.info-card,
.content-card {
  padding: 24px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.coach-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.coach-header-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.coach-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  font-size: 32px;
  font-weight: 700;
  color: #1890ff;
  background: #e6f7ff;
  border-radius: 50%;
}

.coach-meta {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  padding-top: 4px;
}

.coach-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.coach-name {
  font-size: 20px;
  font-weight: 500;
  color: #262626;
}

.ticket-no {
  font-size: 12px;
  color: #8c8c8c;
}

.coach-detail-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 20px;

  &--wide {
    grid-column: span 2;
  }
}

.info-label {
  font-size: 14px;
  color: #86909c;
  white-space: nowrap;
}

.info-value {
  font-size: 14px;
  color: #1d2129;
}

.card-title {
  margin-bottom: 20px;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.student-name {
  font-weight: 500;
  color: #1890ff;
}

.package-no {
  font-size: 13px;
  color: #1d2129;
}

.result-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  padding: 0 8px;
  font-size: 12px;
  border-radius: 12px;
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

.text-success {
  color: #52c41a;
}

.text-muted {
  color: #c9cdd4;
}

.checklist {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checklist-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.checklist-status {
  font-size: 12px;
  white-space: nowrap;
}

.checklist-hint {
  margin-top: 12px;
  font-size: 14px;
  color: #ff4d4f;
}

.timeline {
  display: flex;
  align-items: flex-start;
  gap: 0;
  padding-top: 8px;
}

.timeline-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 120px;
  flex-shrink: 0;

  &--done {
    .timeline-dot {
      background: #52c41a;
    }

    .timeline-title {
      color: #1d2129;
      font-weight: 500;
    }
  }

  &--active {
    .timeline-dot {
      background: #1890ff;
    }

    .timeline-title {
      color: #1d2129;
      font-weight: 500;
    }
  }
}

.timeline-dot {
  width: 12px;
  height: 12px;
  background: #e5e6eb;
  border-radius: 50%;
}

.timeline-title {
  font-size: 13px;
  color: #c9cdd4;
}

.timeline-time {
  font-size: 12px;
  color: #86909c;
}

.timeline-line {
  width: 40px;
  height: 2px;
  margin-top: 5px;
  background: #e5e6eb;

  &--done {
    background: #52c41a;
  }
}

.action-card {
  margin-bottom: 0;
}

.approval-form {
  margin-bottom: 20px;
}

.approval-form-label {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 16px;

  .el-button {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

.action-bar-spacer {
  flex: 1;
}

.closed-status {
  display: inline-flex;
  align-items: center;
}

:deep(.table-header) {
  th {
    background: #f5f7fa;
  }
}

:deep(.el-button--primary.is-disabled) {
  background-color: #a0cfff;
  border-color: #a0cfff;
}

.checklist-item {
  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: #52c41a;
    border-color: #52c41a;
  }

  :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
    color: #1d2129;
  }
}
</style>
