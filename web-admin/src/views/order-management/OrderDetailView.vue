<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { AdminOrderDetail, OrderType, OrderStatus } from '@/types/api';
import {
  getOrderDetail,
  approveRefund,
  rejectRefund,
} from '@/api/orderManagement';
import { formatDateTime, maskPhone } from '@/utils/format';

const route = useRoute();
const router = useRouter();

const orderId = Number(route.params.orderId);
const order = ref<AdminOrderDetail | null>(null);
const loading = ref(false);
const error = ref(false);

const orderTypeMap: Record<
  OrderType,
  { label: string; color: string; bgColor: string }
> = {
  purchase: { label: '购买订单', color: '#1890FF', bgColor: '#E6F7FF' },
  refund: { label: '退款订单', color: '#FA541C', bgColor: '#FFF2E8' },
};

const orderStatusMap: Record<
  OrderStatus,
  { label: string; color: string; bgColor: string }
> = {
  pending_payment: { label: '待支付', color: '#FAAD14', bgColor: '#FFFBE6' },
  paid: { label: '已支付', color: '#52C41A', bgColor: '#F6FFED' },
  cancelled: { label: '已取消', color: '#8C8C8C', bgColor: '#F5F5F5' },
  refund_pending: { label: '退款审批中', color: '#FAAD14', bgColor: '#FFFBE6' },
  refund_processing: {
    label: '退款处理中',
    color: '#1890FF',
    bgColor: '#E6F7FF',
  },
  refunded: { label: '已退款', color: '#52C41A', bgColor: '#F6FFED' },
  rejected: { label: '退款被拒', color: '#FF4D4F', bgColor: '#FFF1F0' },
  dispute_processing: {
    label: '争议处理中',
    color: '#722ED1',
    bgColor: '#F9F0FF',
  },
};

function formatAmount(amount: string | undefined | null): string {
  if (amount === undefined || amount === null) return '-';
  return `¥${Number(amount).toFixed(2)}`;
}

function formatPaymentMethod(method: string | null | undefined): string {
  if (!method) return '-';
  if (method === 'wechat') return '微信支付';
  if (method === 'alipay') return '支付宝';
  return method;
}

async function fetchDetail() {
  if (Number.isNaN(orderId)) {
    error.value = true;
    return;
  }
  loading.value = true;
  error.value = false;
  try {
    const res = await getOrderDetail({ orderId });
    if (res.data) {
      order.value = res.data;
    }
  } catch (err) {
    error.value = true;
    ElMessage.error(err instanceof Error ? err.message : '加载失败');
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push('/order-management');
}

function openPackageDetail() {
  if (!order.value?.packageId) return;
  router.push(`/package-management/detail/${order.value.packageId}`);
}

function openPurchaseOrder() {
  if (!order.value?.purchaseOrderId) return;
  router.push(`/order-management/detail/${order.value.purchaseOrderId}`);
}

function openUserDetail() {
  if (!order.value?.userId) return;
  router.push(`/user-management?userId=${order.value.userId}`);
}

function openCoachDetail() {
  if (!order.value?.coachId) return;
  router.push(`/coach-management/detail/${order.value.coachId}`);
}

function handleCancel() {
  ElMessage.info('取消订单功能即将上线');
}

async function handleApprove() {
  if (!order.value) return;
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入实际退款金额（可低于系统计算金额）',
      '通过退款申请',
      {
        confirmButtonText: '确认通过',
        cancelButtonText: '取消',
        inputType: 'text',
        inputValue: order.value.paidAmount,
        inputPattern: /^\d+(\.\d{1,2})?$/,
        inputErrorMessage: '请输入有效的金额，最多两位小数',
        type: 'warning',
      }
    );
    const { value: adjustReason } = await ElMessageBox.prompt(
      '请输入调整原因（未调整可填“无”）',
      '退款金额调整原因',
      {
        confirmButtonText: '提交',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入调整原因',
      }
    );
    await approveRefund({
      orderId: order.value.orderId,
      refundAmount: value.trim(),
      adjustReason: adjustReason.trim(),
    });
    ElMessage.success('退款申请已通过');
    fetchDetail();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

async function handleReject() {
  if (!order.value) return;
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入驳回原因，学员将收到该原因',
      '驳回退款申请',
      {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入驳回原因',
        type: 'warning',
      }
    );
    await rejectRefund({
      orderId: order.value.orderId,
      rejectedReason: value.trim(),
    });
    ElMessage.success('退款申请已驳回，关联套餐已恢复为 active');
    fetchDetail();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

onMounted(() => {
  fetchDetail();
});
</script>

<template>
  <div class="order-detail">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>套餐订单</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/order-management' }"
          >订单管理</el-breadcrumb-item
        >
        <el-breadcrumb-item>订单详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else-if="error || !order">
      <el-empty description="订单不存在或加载失败">
        <el-button type="primary" @click="goBack">返回列表</el-button>
      </el-empty>
    </template>

    <template v-else>
      <div class="info-card">
        <div class="info-title">
          <span class="order-no">{{ order.orderNo }}</span>
          <span
            class="status-tag"
            :style="{
              color: orderTypeMap[order.type]?.color,
              backgroundColor: orderTypeMap[order.type]?.bgColor,
            }"
          >
            {{ orderTypeMap[order.type]?.label || '-' }}
          </span>
          <span
            class="status-tag"
            :style="{
              color: orderStatusMap[order.status]?.color,
              backgroundColor: orderStatusMap[order.status]?.bgColor,
            }"
          >
            {{ orderStatusMap[order.status]?.label || '-' }}
          </span>
        </div>
        <div class="info-meta">
          <span
            >用户：
            <el-button link type="primary" @click="openUserDetail">
              {{ order.userName || '-' }}
            </el-button>
          </span>
          <span>用户手机号：{{ maskPhone(order.userPhone) }}</span>
          <span
            >教练：
            <el-button link type="primary" @click="openCoachDetail">
              {{ order.coachName || '-' }}
            </el-button>
          </span>
          <span>创建时间：{{ formatDateTime(order.createdAt) }}</span>
        </div>
      </div>

      <div class="info-card">
        <div class="card-title">金额信息</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">应付金额</span>
            <span class="value">{{ formatAmount(order.originalAmount) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">优惠金额</span>
            <span class="value">{{ formatAmount(order.discountAmount) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">实付/退款金额</span>
            <span class="value">{{ formatAmount(order.paidAmount) }}</span>
          </div>
          <div
            v-if="order.type === 'refund' && order.calculatedRefundAmount"
            class="detail-item"
          >
            <span class="label">系统计算可退金额</span>
            <span class="value">{{
              formatAmount(order.calculatedRefundAmount)
            }}</span>
          </div>
        </div>
      </div>

      <div class="info-card">
        <div class="card-title">
          {{ order.type === 'refund' ? '退款信息' : '支付信息' }}
        </div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">支付方式</span>
            <span class="value">{{
              formatPaymentMethod(order.paymentMethod)
            }}</span>
          </div>
          <div class="detail-item">
            <span class="label">渠道流水号</span>
            <span class="value">{{ order.channelTradeNo || '-' }}</span>
          </div>
          <div v-if="order.type === 'refund'" class="detail-item">
            <span class="label">原购买订单</span>
            <span class="value">
              <el-button
                v-if="order.purchaseOrderId"
                link
                type="primary"
                @click="openPurchaseOrder"
              >
                {{ order.purchaseOrderNo || order.purchaseOrderId }}
              </el-button>
              <span v-else>-</span>
            </span>
          </div>
          <div v-if="order.reason" class="detail-item">
            <span class="label">{{
              order.type === 'refund' ? '退款原因' : '原因'
            }}</span>
            <span class="value">{{ order.reason }}</span>
          </div>
          <div v-if="order.rejectedReason" class="detail-item">
            <span class="label">驳回原因</span>
            <span class="value">{{ order.rejectedReason }}</span>
          </div>
          <div v-if="order.adjustReason" class="detail-item">
            <span class="label">金额调整原因</span>
            <span class="value">{{ order.adjustReason }}</span>
          </div>
          <div v-if="order.approvedAt" class="detail-item">
            <span class="label">处理时间</span>
            <span class="value">{{ formatDateTime(order.approvedAt) }}</span>
          </div>
          <div v-if="order.refundedAt" class="detail-item">
            <span class="label">退款到账时间</span>
            <span class="value">{{ formatDateTime(order.refundedAt) }}</span>
          </div>
        </div>
      </div>

      <div v-if="order.packageId" class="info-card">
        <div class="card-title">关联套餐</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">套餐 ID</span>
            <span class="value">
              <el-button link type="primary" @click="openPackageDetail">
                {{ order.packageId }}
              </el-button>
            </span>
          </div>
        </div>
      </div>

      <div class="info-card">
        <div class="card-title">状态时间轴</div>
        <el-timeline>
          <el-timeline-item
            v-for="(log, index) in order.statusTimeline"
            :key="index"
            :timestamp="formatDateTime(log.time)"
          >
            {{ log.description }}
          </el-timeline-item>
          <el-timeline-item v-if="order.statusTimeline.length === 0">
            暂无状态记录
          </el-timeline-item>
        </el-timeline>
      </div>

      <div class="bottom-bar">
        <el-button @click="goBack">返回</el-button>
        <el-button
          v-if="order.type === 'purchase' && order.status === 'pending_payment'"
          type="danger"
          @click="handleCancel"
        >
          取消订单
        </el-button>
        <el-button
          v-if="order.type === 'refund' && order.status === 'refund_pending'"
          type="success"
          @click="handleApprove"
        >
          通过
        </el-button>
        <el-button
          v-if="order.type === 'refund' && order.status === 'refund_pending'"
          type="warning"
          @click="handleReject"
        >
          驳回
        </el-button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.order-detail {
  padding-bottom: 80px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  height: 48px;
  margin-bottom: 16px;
}

.info-card {
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

.order-no {
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
  font-size: 14px;
  color: #595959;
}

.card-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
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
