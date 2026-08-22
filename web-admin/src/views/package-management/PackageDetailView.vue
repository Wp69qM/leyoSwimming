<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type {
  AdminPackageDetail,
  PackageStatus,
  PackageMode,
} from '@/types/api';
import { getPackageDetail } from '@/api/packageManagement';
import { formatDateTime } from '@/utils/format';
import { getTeachingTypeLabel } from './constants';

const route = useRoute();
const router = useRouter();

const packageId = Number(route.params.packageId);
const pkg = ref<AdminPackageDetail | null>(null);
const loading = ref(false);
const error = ref(false);
const activeTab = ref('records');

const statusMap: Record<
  PackageStatus,
  { label: string; color: string; bgColor: string }
> = {
  active: { label: '活跃', color: '#52C41A', bgColor: '#F6FFED' },
  exhausted: { label: '已耗尽', color: '#8C8C8C', bgColor: '#F5F5F5' },
  expired: { label: '已过期', color: '#FAAD14', bgColor: '#FFFBE6' },
  frozen: { label: '已冻结', color: '#722ED1', bgColor: '#F9F0FF' },
  refunded: { label: '已退款', color: '#FF4D4F', bgColor: '#FFF1F0' },
};

const modeMap: Record<
  PackageMode,
  { label: string; color: string; bgColor: string }
> = {
  standard: { label: '正价套餐', color: '#1890FF', bgColor: '#E6F7FF' },
  experience: { label: '体验课', color: '#FAAD14', bgColor: '#FFFBE6' },
};

function formatAmount(amount: string | undefined | null): string {
  if (amount === undefined || amount === null) return '-';
  return `¥${Number(amount).toFixed(2)}`;
}

async function fetchDetail() {
  if (Number.isNaN(packageId)) {
    error.value = true;
    return;
  }
  loading.value = true;
  error.value = false;
  try {
    const res = await getPackageDetail({ packageId });
    if (res.data) {
      pkg.value = res.data;
    }
  } catch (err) {
    error.value = true;
    ElMessage.error(err instanceof Error ? err.message : '加载失败');
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push('/package-management');
}

function openUserDetail() {
  if (!pkg.value?.userId) return;
  router.push(`/user-management?userId=${pkg.value.userId}`);
}

function openCoachDetail() {
  if (!pkg.value?.coachId) return;
  router.push(`/coach-management/detail/${pkg.value.coachId}`);
}

function handleFreeze() {
  ElMessage.info('冻结功能即将上线');
}

function handleUnfreeze() {
  ElMessage.info('解冻功能即将上线');
}

function handleExtend() {
  ElMessage.info('延期功能即将上线');
}

function handleRefund() {
  ElMessage.info('请前往订单管理处理退款订单');
}

onMounted(() => {
  fetchDetail();
});
</script>

<template>
  <div class="package-detail">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>套餐订单</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/package-management' }"
          >套餐管理</el-breadcrumb-item
        >
        <el-breadcrumb-item>套餐详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else-if="error || !pkg">
      <el-empty description="套餐不存在或加载失败">
        <el-button type="primary" @click="goBack">返回列表</el-button>
      </el-empty>
    </template>

    <template v-else>
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
          <span
            >用户：
            <el-button link type="primary" @click="openUserDetail">
              {{ pkg.userName || '-' }}
            </el-button>
          </span>
          <span
            >教练：
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
            <span class="label">套餐模式</span>
            <span class="value">{{
              modeMap[pkg.packageMode]?.label || '-'
            }}</span>
          </div>
          <div class="detail-item">
            <span class="label">课程类型</span>
            <span class="value">{{
              getTeachingTypeLabel(pkg.teachingType || undefined)
            }}</span>
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
            <span class="value">{{ pkg.refundRatio }}</span>
          </div>
          <div class="detail-item">
            <span class="label">退款有效天数</span>
            <span class="value">{{ pkg.refundValidDays }} 天</span>
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

      <div class="bottom-bar">
        <el-button @click="goBack">返回</el-button>
        <el-button
          v-if="pkg.status === 'active'"
          type="danger"
          @click="handleFreeze"
        >
          冻结
        </el-button>
        <el-button
          v-if="pkg.status === 'frozen'"
          type="primary"
          @click="handleUnfreeze"
        >
          解冻
        </el-button>
        <el-button
          v-if="pkg.status === 'active' || pkg.status === 'expired'"
          type="primary"
          @click="handleExtend"
        >
          延期
        </el-button>
        <el-button
          v-if="pkg.status === 'active'"
          type="warning"
          @click="handleRefund"
        >
          退款
        </el-button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.package-detail {
  padding-bottom: 80px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  height: 48px;
  margin-bottom: 16px;
}

.info-card,
.tabs-card {
  padding: 24px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.info-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.package-no {
  font-size: 20px;
  font-weight: 500;
  color: #262626;
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
  color: #595959;
}

.freeze-info {
  font-size: 14px;
  color: #ff4d4f;
}

.card-title {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.snapshot-tip {
  margin-bottom: 16px;
  font-size: 12px;
  color: #8c8c8c;
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
  color: #8c8c8c;
}

.detail-item .value {
  color: #262626;
}

.bottom-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 220px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 24px;
  background: #ffffff;
  border-top: 1px solid #e4e7ed;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
}
</style>
