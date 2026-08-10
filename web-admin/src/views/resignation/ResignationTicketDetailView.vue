<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
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
  allActionsRegistered: false,
  scheduleCleared: false,
  settlementCompleted: false,
});

const statusMap: Record<
  ResignationStatus,
  { label: string; type: 'primary' | 'success' | 'danger' | 'warning' }
> = {
  processing: { label: '处理中', type: 'warning' },
  pending_audit: { label: '待审批', type: 'primary' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已驳回', type: 'danger' },
};

const resultMap: Record<Exclude<StudentHandleResult, null>, string> = {
  transfer: '转新教练',
  refund: '全额退款',
  continue: '继续上完',
};

const allChecklistPassed = computed(() => {
  return (
    checklist.value.allActionsRegistered &&
    checklist.value.scheduleCleared &&
    checklist.value.settlementCompleted
  );
});

const unpassedItems = computed(() => {
  const items: string[] = [];
  if (!checklist.value.allActionsRegistered) {
    items.push('学员处理结果未全部登记');
  }
  if (!checklist.value.scheduleCleared) {
    items.push('未来排班未清空');
  }
  if (!checklist.value.settlementCompleted) {
    items.push('教练费未结算');
  }
  return items;
});

const activeStudentCount = computed(() => detail.value?.packages.length ?? 0);

const totalRemainingHours = computed(
  () =>
    detail.value?.packages.reduce(
      (sum, pkg) => sum + pkg.availableCount + pkg.reservedCount,
      0
    ) ?? 0
);

function applyAutoChecklist() {
  if (!detail.value) return;
  checklist.value = {
    allActionsRegistered:
      detail.value.packages.length === 0 ||
      detail.value.packages.every((pkg) => !!pkg.action),
    scheduleCleared: detail.value.scheduleCleared,
    settlementCompleted: detail.value.settlementStatus === 1,
  };
}

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
  // eslint-disable-next-line no-console
  console.warn('学员处理结果登记需后端支持，当前仅前端展示');
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

    <div class="info-card">
      <div class="coach-header">
        <el-avatar :size="80" class="coach-avatar">
          {{ detail.coach.name.charAt(0) }}
        </el-avatar>
        <div class="coach-meta">
          <div class="coach-name-row">
            <span class="coach-name">{{ detail.coach.name }}</span>
            <el-tag :type="statusMap[detail.status].type" effect="light" round>
              {{ statusMap[detail.status].label }}
            </el-tag>
          </div>
          <div class="ticket-no">工单号：{{ detail.ticketNo }}</div>
        </div>
      </div>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">手机号</span>
          <span class="info-value">{{ maskPhone(detail.coach.phone) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">提交离职时间</span>
          <span class="info-value">{{
            formatDateTime(detail.coach.submittedAt)
          }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">active 学员数</span>
          <span class="info-value">{{ activeStudentCount }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">累计剩余课时</span>
          <span class="info-value">{{ totalRemainingHours }}</span>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="card-title">学员处理清单</div>
      <el-table
        v-if="detail.packages.length > 0"
        :data="detail.packages"
        style="width: 100%"
      >
        <el-table-column prop="userName" label="学员" min-width="120" />
        <el-table-column label="剩余课时" align="center" min-width="100">
          <template #default="{ row }">
            {{ row.availableCount + row.reservedCount }}
          </template>
        </el-table-column>
        <el-table-column label="处理结果" align="center" min-width="120">
          <template #default="{ row }">
            <span>{{ row.action ? resultMap[row.action] : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="detail.status === 'pending_audit'"
          label="操作"
          align="center"
          min-width="120"
        >
          <template #default>
            <el-button link type="primary" @click="openResultDialog()"
              >登记</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="无 active 学员" />
    </div>

    <div class="content-card">
      <div class="card-title">管理员检查清单（全部通过才可批准）</div>
      <div class="checklist">
        <el-checkbox
          v-model="checklist.allActionsRegistered"
          :disabled="detail.status !== 'pending_audit'"
        >
          所有学员处理结果已登记
        </el-checkbox>
        <el-checkbox
          v-model="checklist.scheduleCleared"
          :disabled="detail.status !== 'pending_audit'"
        >
          排班未来时段已清空
        </el-checkbox>
        <el-checkbox
          v-model="checklist.settlementCompleted"
          :disabled="detail.status !== 'pending_audit'"
        >
          教练费已结算
        </el-checkbox>
      </div>
      <div
        v-if="!allChecklistPassed && detail.status === 'pending_audit'"
        class="checklist-hint"
      >
        请先完成以下检查项：{{ unpassedItems.join('、') }}
      </div>
    </div>

    <div class="bottom-bar-spacer" />

    <div class="bottom-bar">
      <el-button @click="goBack">返回</el-button>
      <div v-if="detail.status === 'pending_audit'" class="bottom-actions">
        <el-input
          v-model="approvalComment"
          placeholder="请输入审批意见（必填）"
          clearable
          style="width: 400px"
        />
        <el-button type="danger" @click="handleReject">驳回</el-button>
        <el-tooltip
          :disabled="allChecklistPassed"
          content="请先完成检查清单"
          placement="top"
        >
          <el-button
            type="primary"
            :disabled="!allChecklistPassed"
            @click="handleApprove"
          >
            通过
          </el-button>
        </el-tooltip>
      </div>
      <div v-else class="bottom-status">
        <el-tag :type="statusMap[detail.status].type" effect="light" round>
          {{ statusMap[detail.status].label }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.resignation-ticket-detail {
  padding-bottom: 96px;
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

.info-card,
.content-card {
  padding: 24px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 4px;
}

.coach-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.coach-avatar {
  font-size: 32px;
  font-weight: 500;
}

.coach-meta {
  flex: 1;
}

.coach-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
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

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #8c8c8c;
}

.info-value {
  font-size: 14px;
  color: #262626;
}

.card-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.checklist {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checklist-hint {
  margin-top: 12px;
  font-size: 14px;
  color: #ff4d4f;
}

.bottom-bar-spacer {
  height: 80px;
}

.bottom-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 200px;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  background: #ffffff;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.08);
}

.bottom-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bottom-status {
  display: flex;
  align-items: center;
}
</style>
