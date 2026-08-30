<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import {
  getCoachApplicationDetail,
  approveCoachApplication,
  rejectCoachApplication,
} from '@/api/coachAudit';
import type {
  CoachApplicationDetail,
  CoachAuditStatus,
  CoachApplicationCertificate,
} from '@/types/coachAudit';
import { maskPhone, maskIdCard, formatDateTime } from '@/utils/format';

const route = useRoute();
const router = useRouter();

const applicationId = computed(() => Number(route.params.applicationId));

const detail = ref<CoachApplicationDetail | null>(null);
const loading = ref(false);
const error = ref(false);
const previewImageUrl = ref('');
const previewVisible = ref(false);
const approvalComment = ref('');

const checklist = reactive({
  idCardClear: false,
  infoConsistent: false,
  noBadRecord: false,
  priceReasonable: false,
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

const statusLabelMap: Record<string, string> = {
  draft: '草稿',
  pending: '待审核',
  approved: '已通过',
  rejected: '已驳回',
};

const auditActionMap: Record<string, string> = {
  submit: '提交入驻',
  approve: '通过审核',
  reject: '驳回审核',
  update: '更新资料',
};

const genderMap: Record<string, string> = {
  male: '男',
  female: '女',
};

const certTypeLabels: Record<string, string> = {
  ID_CARD_FRONT: '身份证正面',
  ID_CARD_BACK: '身份证反面',
  COACH_CERT: '教练资格证',
  HEALTH_CERT: '健康证',
  PORTRAIT: '个人形象照',
  OTHER: '其他资质',
};

const expectedCertTypes = [
  'ID_CARD_FRONT',
  'ID_CARD_BACK',
  'COACH_CERT',
  'HEALTH_CERT',
  'PORTRAIT',
];

const entryTypeLabel = computed(() => {
  if (!detail.value) return '-';
  return entryTypeMap[detail.value.previousCoachStatus] || '其他';
});

const sortedCertificates = computed<CoachApplicationCertificate[]>(() => {
  if (!detail.value) return [];
  return [...detail.value.certificates].sort(
    (a, b) => a.sortOrder - b.sortOrder
  );
});

const certificateMap = computed(() => {
  const map = new Map<string, CoachApplicationCertificate>();
  sortedCertificates.value.forEach((cert) => {
    map.set(cert.certType, cert);
  });
  return map;
});

function formatGender(gender: string): string {
  return genderMap[gender] || gender || '-';
}

function formatStrokes(strokes: string | string[]): string {
  if (!strokes || (Array.isArray(strokes) && strokes.length === 0)) return '-';
  return Array.isArray(strokes) ? strokes.join('、') : strokes;
}

function openPreview(url: string) {
  if (!url) return;
  previewImageUrl.value = url;
  previewVisible.value = true;
}

async function fetchDetail() {
  if (Number.isNaN(applicationId.value) || applicationId.value <= 0) {
    error.value = true;
    return;
  }

  loading.value = true;
  error.value = false;
  try {
    const res = await getCoachApplicationDetail(applicationId.value);
    if (res.data) {
      detail.value = res.data;
      approvalComment.value = '';
      resetChecklist();
    } else {
      error.value = true;
    }
  } catch (err) {
    error.value = true;
    detail.value = null;
    ElMessage.error(err instanceof Error ? err.message : '加载失败');
  } finally {
    loading.value = false;
  }
}

function resetChecklist() {
  checklist.idCardClear = false;
  checklist.infoConsistent = false;
  checklist.noBadRecord = false;
  checklist.priceReasonable = false;
}

function goBack() {
  router.push('/coach-audit/queue');
}

async function handleApprove() {
  if (!detail.value) return;
  try {
    const message = approvalComment.value.trim()
      ? `确认通过教练 ${detail.value.name} 的入驻申请？\n审核意见：${approvalComment.value.trim()}`
      : `确认通过教练 ${detail.value.name} 的入驻申请？通过后将立即生效且不可撤销`;
    await ElMessageBox.confirm(message, '确认通过', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await approveCoachApplication(detail.value.applicationId);
    ElMessage.success('操作成功');
    fetchDetail();
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
  try {
    const message = `确认驳回教练 ${detail.value.name} 的入驻申请？\n驳回原因：${approvalComment.value.trim()}`;
    await ElMessageBox.confirm(message, '驳回入驻申请', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await rejectCoachApplication(
      detail.value.applicationId,
      approvalComment.value.trim()
    );
    ElMessage.success('已驳回');
    fetchDetail();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

watch(
  () => route.params.applicationId,
  () => {
    fetchDetail();
  },
  { immediate: true }
);
</script>

<template>
  <div v-if="error" class="error-page">
    <el-empty description="入驻申请不存在">
      <el-button type="primary" @click="goBack">返回列表</el-button>
    </el-empty>
  </div>

  <div v-else-if="detail" class="coach-audit-detail">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>用户管理</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/coach-audit/queue' }">
          教练入驻审核
        </el-breadcrumb-item>
        <el-breadcrumb-item>审核详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="page-title-row">
      <div class="back-btn" @click="goBack">
        <el-icon :size="18"><ArrowLeft /></el-icon>
      </div>
      <h1 class="page-title">教练入驻资料详情</h1>
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

    <div class="info-card">
      <div class="coach-header">
        <div class="coach-header-left">
          <div class="coach-avatar">
            {{ detail.name.charAt(0) }}
          </div>
          <div class="coach-meta">
            <div class="coach-name">{{ detail.name }}</div>
            <div class="application-id">
              申请 ID：{{ detail.applicationId }}
            </div>
          </div>
        </div>
        <div class="entry-type">入驻类型：{{ entryTypeLabel }}</div>
      </div>

      <div class="section">
        <div class="section-title">基础信息</div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">姓名/昵称</span>
            <span class="info-value">{{ detail.name }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">手机号</span>
            <span class="info-value">{{ maskPhone(detail.phone) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">性别</span>
            <span class="info-value">{{ formatGender(detail.gender) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">年龄</span>
            <span class="info-value">{{ detail.age ?? '-' }} 岁</span>
          </div>
          <div class="info-item">
            <span class="info-label">邮箱</span>
            <span class="info-value">{{ detail.email || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">微信二维码</span>
            <span
              v-if="detail.wechatQrUrl"
              class="info-value info-value--link"
              @click="openPreview(detail.wechatQrUrl)"
            >
              点击预览
            </span>
            <span v-else class="info-value">-</span>
          </div>
        </div>
      </div>

      <div class="section">
        <div class="section-title">教学履历</div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">任教年限</span>
            <span class="info-value">{{ detail.teachingYears ?? '-' }} 年</span>
          </div>
          <div class="info-item">
            <span class="info-label">总学员数</span>
            <span class="info-value">{{ detail.totalStudents ?? '-' }} 人</span>
          </div>
          <div class="info-item">
            <span class="info-label">总课时数</span>
            <span class="info-value">{{ detail.totalHours ?? '-' }} 节</span>
          </div>
          <div class="info-item">
            <span class="info-label">擅长泳姿</span>
            <span class="info-value">{{
              formatStrokes(detail.teachingStrokes)
            }}</span>
          </div>
          <div class="info-item info-item--full">
            <span class="info-label">个人简介</span>
            <span class="info-value info-value--block">{{
              detail.bio || '-'
            }}</span>
          </div>
        </div>
      </div>

      <div class="section">
        <div class="section-title">服务设置</div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">参考单价</span>
            <span class="info-value">{{
              detail.referencePrice ? `¥${detail.referencePrice}` : '-'
            }}</span>
          </div>
        </div>
      </div>

      <div class="submit-time">
        提交时间：{{ formatDateTime(detail.submittedAt) }}
      </div>
    </div>

    <div class="content-card">
      <div class="card-title">实名与资质照片</div>
      <div class="id-card-no">身份证号：{{ maskIdCard(detail.idCardNo) }}</div>
      <div class="certificate-list">
        <div
          v-for="certType in expectedCertTypes"
          :key="certType"
          class="certificate-item"
        >
          <div class="certificate-label">{{ certTypeLabels[certType] }}</div>
          <div
            v-if="certificateMap.get(certType)?.imageUrl"
            class="certificate-image"
            @click="openPreview(certificateMap.get(certType)!.imageUrl)"
          >
            <el-image
              :src="certificateMap.get(certType)!.imageUrl"
              :preview-src-list="[]"
              fit="cover"
              class="certificate-thumb"
            >
              <template #error>
                <div class="image-error">加载失败</div>
              </template>
            </el-image>
          </div>
          <div v-else class="certificate-placeholder">未上传</div>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="card-title">审核检查清单</div>
      <div class="checklist">
        <div class="checklist-item">
          <el-checkbox
            v-model="checklist.idCardClear"
            :disabled="detail.status !== 'pending'"
          >
            证件清晰可辨
          </el-checkbox>
          <span
            class="checklist-status"
            :class="checklist.idCardClear ? 'text-success' : 'text-primary'"
          >
            {{ checklist.idCardClear ? '已通过' : '待确认' }}
          </span>
        </div>
        <div class="checklist-item">
          <el-checkbox
            v-model="checklist.infoConsistent"
            :disabled="detail.status !== 'pending'"
          >
            信息一致
          </el-checkbox>
          <span
            class="checklist-status"
            :class="checklist.infoConsistent ? 'text-success' : 'text-primary'"
          >
            {{ checklist.infoConsistent ? '已通过' : '待确认' }}
          </span>
        </div>
        <div class="checklist-item">
          <el-checkbox
            v-model="checklist.noBadRecord"
            :disabled="detail.status !== 'pending'"
          >
            无不良记录
          </el-checkbox>
          <span
            class="checklist-status"
            :class="checklist.noBadRecord ? 'text-success' : 'text-primary'"
          >
            {{ checklist.noBadRecord ? '已通过' : '待确认' }}
          </span>
        </div>
        <div class="checklist-item">
          <el-checkbox
            v-model="checklist.priceReasonable"
            :disabled="detail.status !== 'pending'"
          >
            单价在合理区间
          </el-checkbox>
          <span
            class="checklist-status"
            :class="checklist.priceReasonable ? 'text-success' : 'text-primary'"
          >
            {{ checklist.priceReasonable ? '已通过' : '待确认' }}
          </span>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="card-title">
        申请历史
        <span class="history-count"
          >该教练共有 {{ detail.history.length }} 条申请记录</span
        >
      </div>
      <el-table
        v-if="detail.history.length > 0"
        :data="detail.history"
        style="width: 100%"
        header-row-class-name="table-header"
      >
        <el-table-column label="申请 ID" prop="applicationId" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: statusMap[row.status as CoachAuditStatus].color,
                backgroundColor:
                  statusMap[row.status as CoachAuditStatus].bgColor,
              }"
            >
              {{ statusLabelMap[row.status] || row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.submittedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="审核时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.approvedAt ?? undefined) }}
          </template>
        </el-table-column>
        <el-table-column label="审核人" width="140">
          <template #default="{ row }">
            {{ row.approvedBy || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="驳回原因">
          <template #default="{ row }">
            {{ row.rejectionReason || '-' }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无申请历史" />
    </div>

    <div class="content-card">
      <div class="card-title">审核日志</div>
      <el-timeline v-if="detail.auditLogs.length > 0">
        <el-timeline-item
          v-for="log in detail.auditLogs"
          :key="log.logId"
          :timestamp="formatDateTime(log.createdAt)"
        >
          <div class="audit-log-action">
            {{ auditActionMap[log.action] || log.action }}
          </div>
          <div class="audit-log-meta">
            管理员 ID：{{ log.adminId }} · 状态：{{ log.fromStatus }} →
            {{ log.toStatus }}
          </div>
          <div v-if="log.reason" class="audit-log-reason">
            原因：{{ log.reason }}
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审核日志" />
    </div>

    <div class="fixed-action-bar">
      <div class="fixed-action-bar__inner">
        <el-button size="large" @click="goBack">
          <i class="ri-arrow-go-back-line" />
          返回列表
        </el-button>
        <div class="fixed-action-bar__right">
          <template v-if="detail.status === 'pending'">
            <el-input
              v-model="approvalComment"
              type="textarea"
              :rows="2"
              placeholder="请输入审核意见（选填，驳回时建议填写原因）"
              class="approval-comment-input"
            />
            <el-button type="primary" size="large" @click="handleApprove">
              <i class="ri-check-line" />
              审核通过
            </el-button>
            <el-button
              type="danger"
              size="large"
              :disabled="!approvalComment.trim()"
              @click="handleReject"
            >
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
        </div>
      </div>
    </div>
  </div>

  <el-dialog
    v-model="previewVisible"
    title="图片预览"
    width="600px"
    align-center
  >
    <el-image :src="previewImageUrl" fit="contain" style="width: 100%" />
  </el-dialog>
</template>

<style scoped lang="scss">
.coach-audit-detail {
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

.coach-name {
  font-size: 20px;
  font-weight: 500;
  color: #262626;
}

.application-id {
  font-size: 12px;
  color: #8c8c8c;
}

.entry-type {
  font-size: 14px;
  color: #262626;
}

.section {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-height: 20px;

  &--full {
    grid-column: span 3;
  }
}

.info-label {
  flex-shrink: 0;
  font-size: 14px;
  color: #86909c;
  white-space: nowrap;
}

.info-value {
  font-size: 14px;
  color: #1d2129;

  &--block {
    line-height: 1.6;
    white-space: pre-wrap;
  }

  &--link {
    color: #1890ff;
    cursor: pointer;
  }
}

.submit-time {
  margin-top: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

.card-title {
  margin-bottom: 20px;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.id-card-no {
  margin-bottom: 16px;
  font-size: 14px;
  color: #86909c;
}

.certificate-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.certificate-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.certificate-label {
  font-size: 12px;
  color: #86909c;
}

.certificate-image,
.certificate-placeholder {
  width: 160px;
  height: 120px;
  overflow: hidden;
  border-radius: 8px;
}

.certificate-image {
  cursor: pointer;
}

.certificate-thumb {
  width: 100%;
  height: 100%;
}

.certificate-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #86909c;
  background: #f5f7fa;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 14px;
  color: #86909c;
  background: #f5f7fa;
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

.history-count {
  margin-left: 12px;
  font-size: 12px;
  font-weight: 400;
  color: #86909c;
}

.audit-log-action {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.audit-log-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #86909c;
}

.audit-log-reason {
  margin-top: 4px;
  font-size: 13px;
  color: #ff4d4f;
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

.text-success {
  color: #52c41a;
}

.text-primary {
  color: #1890ff;
}

.fixed-action-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 220px;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 72px;
  padding: 0 24px;
  background: #ffffff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
}

.fixed-action-bar__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1440px;
}

.fixed-action-bar__right {
  display: flex;
  align-items: center;
  gap: 16px;

  .el-button {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

.approval-comment-input {
  width: 400px;

  :deep(.el-textarea__inner) {
    resize: none;
  }
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
