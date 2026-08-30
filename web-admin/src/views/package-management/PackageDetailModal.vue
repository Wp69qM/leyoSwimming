<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue';
import { ElMessage } from 'element-plus';
import type { AdminPackageDetail } from '@/types/api';
import { getPackageDetail } from '@/api/packageManagement';
import { formatDateTime } from '@/utils/format';
import PackageFreezeModal from './PackageFreezeModal.vue';
import PackageExtendModal from './PackageExtendModal.vue';
import PackageRefundModal from './PackageRefundModal.vue';
import { getTeachingTypeLabel, getStrokeLabels } from './constants';

interface Props {
  visible: boolean;
  packageId?: number;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success'): void;
}>();

const pkg = ref<AdminPackageDetail | null>(null);
const loading = ref(false);
const error = ref(false);
const activeTab = ref('records');

const statusMap: Record<
  string,
  { label: string; color: string; bgColor: string }
> = {
  active: { label: '活跃', color: '#52C41A', bgColor: '#F6FFED' },
  exhausted: { label: '已耗尽', color: '#8C8C8C', bgColor: '#F5F5F5' },
  expired: { label: '已过期', color: '#FAAD14', bgColor: '#FFFBE6' },
  frozen: { label: '已冻结', color: '#722ED1', bgColor: '#F9F0FF' },
  refunded: { label: '已退款', color: '#FF4D4F', bgColor: '#FFF1F0' },
};

const modeMap: Record<
  string,
  { label: string; color: string; bgColor: string }
> = {
  standard: { label: '正价套餐', color: '#1890FF', bgColor: '#E6F7FF' },
  experience: { label: '体验课', color: '#FAAD14', bgColor: '#FFFBE6' },
  custom: { label: '自定义套餐', color: '#52C41A', bgColor: '#F6FFED' },
};

const subModal = reactive({
  freeze: false,
  extend: false,
  refund: false,
});

const canRefund = computed(() => {
  if (!pkg.value) return false;
  if (pkg.value.status !== 'active' || !pkg.value.refundEnabled) {
    return false;
  }
  if (pkg.value.refundValidDays > 0) {
    const deadline = new Date(pkg.value.createdAt);
    deadline.setDate(deadline.getDate() + pkg.value.refundValidDays);
    if (new Date() > deadline) {
      return false;
    }
  }
  return true;
});

function formatAmount(amount: string | undefined | null): string {
  if (amount === undefined || amount === null) return '-';
  return `¥${Number(amount).toFixed(2)}`;
}

async function fetchDetail() {
  if (!props.packageId) return;
  loading.value = true;
  error.value = false;
  try {
    const res = await getPackageDetail({ packageId: props.packageId });
    pkg.value = res.data ?? null;
  } catch (err) {
    error.value = true;
    pkg.value = null;
    ElMessage.error(err instanceof Error ? err.message : '加载失败');
  } finally {
    loading.value = false;
  }
}

function handleClose() {
  emit('update:visible', false);
}

function handleSuccess() {
  emit('success');
  fetchDetail();
}

function openUserDetail() {
  if (!pkg.value?.userId) return;
  window.open(`/user-management?userId=${pkg.value.userId}`, '_blank');
}

function openCoachDetail() {
  if (!pkg.value?.coachId) return;
  window.open(`/coach-management/detail/${pkg.value.coachId}`, '_blank');
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      activeTab.value = 'records';
      fetchDetail();
    } else {
      pkg.value = null;
      error.value = false;
    }
  }
);
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="套餐详情"
    width="720px"
    :close-on-click-modal="false"
    destroy-on-close
    class="package-detail-modal"
    @update:model-value="emit('update:visible', $event)"
  >
    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else-if="error || !pkg">
      <el-empty description="套餐不存在或加载失败">
        <el-button type="primary" @click="fetchDetail">重试</el-button>
      </el-empty>
    </template>

    <div v-else class="modal-body">
      <div class="info-card">
        <div class="info-title">
          <span class="package-no">{{ pkg.packageNo }}</span>
          <span
            class="status-tag"
            :style="{
              color: modeMap[pkg.packageMode]?.color,
              backgroundColor: modeMap[pkg.packageMode]?.bgColor,
            }"
          >
            {{ modeMap[pkg.packageMode]?.label || '-' }}
          </span>
          <span
            class="status-tag"
            :style="{
              color: statusMap[pkg.status]?.color,
              backgroundColor: statusMap[pkg.status]?.bgColor,
            }"
          >
            {{ statusMap[pkg.status]?.label || '-' }}
          </span>
        </div>
        <div class="info-meta">
          <span>
            用户：
            <el-button link type="primary" @click="openUserDetail">
              {{ pkg.userName || '-' }}
            </el-button>
          </span>
          <span>
            教练：
            <el-button link type="primary" @click="openCoachDetail">
              {{ pkg.coachName || '-' }}
            </el-button>
          </span>
          <span>购买时间：{{ formatDateTime(pkg.createdAt) }}</span>
          <span>到期时间：{{ formatDateTime(pkg.expireAt) }}</span>
        </div>
        <div v-if="pkg.frozenReason" class="freeze-info">
          冻结原因：{{ pkg.frozenReason }}
        </div>
      </div>

      <div class="info-card">
        <div class="card-title">购买时快照</div>
        <div class="snapshot-tip">
          以下信息为购买时快照，不随套餐模板变更而改变
        </div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">套餐名称</span>
            <span class="value">{{ pkg.packageName || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">套餐模式</span>
            <span class="value">{{
              modeMap[pkg.packageMode]?.label || '-'
            }}</span>
          </div>
          <div class="detail-item">
            <span class="label">班级规模</span>
            <span class="value">{{
              getTeachingTypeLabel(pkg.teachingType)
            }}</span>
          </div>
          <div class="detail-item">
            <span class="label">教学泳姿</span>
            <span class="value">{{ getStrokeLabels(pkg.strokeIds) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">课时时长</span>
            <span class="value">{{ pkg.durationMinutes }} 分钟</span>
          </div>
          <div class="detail-item">
            <span class="label">有效天数</span>
            <span class="value">{{ pkg.validDays }} 天</span>
          </div>
          <div class="detail-item">
            <span class="label">总课时</span>
            <span class="value">{{ pkg.totalHours }}</span>
          </div>
          <div class="detail-item">
            <span class="label">每课时价格</span>
            <span class="value">{{ formatAmount(pkg.pricePerHour) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">原价</span>
            <span class="value">{{ formatAmount(pkg.originalPrice) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">实付价</span>
            <span class="value">{{ formatAmount(pkg.paidAmount) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">是否可退款</span>
            <span class="value">{{ pkg.refundEnabled ? '是' : '否' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">退款比例</span>
            <span class="value">{{ Number((pkg.refundRatio ?? 0) * 100).toFixed(0) }}%</span>
          </div>
          <div class="detail-item">
            <span class="label">退款有效天数</span>
            <span class="value">{{ pkg.refundValidDays }} 天</span>
          </div>
          <div class="detail-item">
            <span class="label">可退金额</span>
            <span class="value">{{ formatAmount(pkg.refundAmount) }}</span>
          </div>
        </div>
      </div>

      <div class="info-card">
        <div class="card-title">套餐信息</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">总课时</span>
            <span class="value">{{ pkg.totalHours }}</span>
          </div>
          <div class="detail-item">
            <span class="label">已消耗</span>
            <span class="value">{{ pkg.consumedCount }}</span>
          </div>
          <div class="detail-item">
            <span class="label">已预约</span>
            <span class="value">{{ pkg.reservedCount }}</span>
          </div>
          <div class="detail-item">
            <span class="label">剩余可用</span>
            <span class="value">{{ pkg.availableCount }}</span>
          </div>
        </div>
      </div>

      <div class="tabs-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="上课记录" name="records">
            <el-empty description="上课记录功能即将上线" />
          </el-tab-pane>
          <el-tab-pane label="操作日志" name="logs">
            <el-empty description="操作日志功能即将上线" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <template #footer>
      <div class="modal-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button
          v-if="pkg?.status === 'active'"
          type="danger"
          @click="subModal.freeze = true"
        >
          冻结
        </el-button>
        <el-button
          v-if="pkg?.status === 'frozen'"
          type="primary"
          @click="subModal.freeze = true"
        >
          解冻
        </el-button>
        <el-button
          v-if="pkg?.status === 'active' || pkg?.status === 'expired'"
          type="primary"
          @click="subModal.extend = true"
        >
          延期
        </el-button>
        <el-button
          v-if="canRefund"
          type="warning"
          @click="subModal.refund = true"
        >
          退款
        </el-button>
      </div>
    </template>

    <PackageFreezeModal
      v-if="pkg"
      v-model:visible="subModal.freeze"
      :package-id="pkg.packageId"
      :status="pkg.status"
      :package-no="pkg.packageNo"
      :available-count="pkg.availableCount"
      :expire-at="pkg.expireAt"
      :frozen-reason="pkg.frozenReason"
      :version="pkg.version"
      @success="handleSuccess"
    />

    <PackageExtendModal
      v-if="pkg"
      v-model:visible="subModal.extend"
      :package-id="pkg.packageId"
      :package-no="pkg.packageNo"
      :available-count="pkg.availableCount"
      :current-expire-at="pkg.expireAt"
      :version="pkg.version"
      @success="handleSuccess"
    />

    <PackageRefundModal
      v-if="pkg"
      v-model:visible="subModal.refund"
      :package-id="pkg.packageId"
      :package-no="pkg.packageNo"
      :total-hours="pkg.totalHours"
      :consumed-count="pkg.consumedCount"
      :available-count="pkg.availableCount"
      :expire-at="pkg.expireAt"
      :refund-amount="pkg.refundAmount"
      :refund-ratio="pkg.refundRatio"
      :version="pkg.version"
      @success="handleSuccess"
    />
  </el-dialog>
</template>

<style scoped lang="scss">
.package-detail-modal {
  :deep(.el-dialog__body) {
    max-height: 60vh;
    padding-top: 8px;
    overflow-y: auto;
  }
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-card,
.tabs-card {
  padding: 20px;
  background: var(--calicat-white);
  border-radius: var(--calicat-radius-card);
  box-shadow: var(--calicat-shadow);
}

.info-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.package-no {
  font-size: 18px;
  font-weight: 600;
  color: var(--calicat-text-primary);
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

.info-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--calicat-text-secondary);
}

.freeze-info {
  font-size: 14px;
  color: var(--calicat-danger);
}

.card-title {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--calicat-text-primary);
}

.snapshot-tip {
  margin-bottom: 16px;
  font-size: 12px;
  color: var(--calicat-text-tertiary);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.detail-item {
  display: flex;
  gap: 8px;
  font-size: 14px;
}

.detail-item .label {
  color: var(--calicat-text-tertiary);
}

.detail-item .value {
  color: var(--calicat-text-primary);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
