<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { ElMessage } from 'element-plus';
import type { AdminOrderDetail } from '@/types/api';
import { getOrderDetail } from '@/api/orderManagement';
import { formatDateTime, maskPhone } from '@/utils/format';
import { getTeachingTypeLabel } from '../package-management/constants';
import {
  orderTypeMap,
  orderStatusMap,
  packageStatusMap,
  formatOrderAmount,
  formatPaymentMethod,
} from './constants';
import { useRefundApproval } from './composables/useRefundApproval';

interface Props {
  visible: boolean;
  orderId?: number;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success'): void;
  (e: 'open:order', orderId: number): void;
}>();

const order = ref<AdminOrderDetail | null>(null);
const loading = ref(false);
const error = ref(false);

const isPurchase = computed(() => order.value?.type === 'purchase');
const isRefund = computed(() => order.value?.type === 'refund');
const canCancel = computed(
  () => isPurchase.value && order.value?.status === 'pending_payment'
);
const canApproveRefund = computed(
  () => isRefund.value && order.value?.status === 'refund_pending'
);
const showPackageSection = computed(() => {
  if (!order.value) return false;
  if (isPurchase.value) {
    return order.value.status !== 'cancelled';
  }
  return isRefund.value;
});

const { approve: approveRefund, reject: rejectRefund } =
  useRefundApproval(handleSuccess);

async function fetchDetail() {
  if (!props.orderId) return;
  loading.value = true;
  error.value = false;
  try {
    const res = await getOrderDetail({ orderId: props.orderId });
    order.value = res.data ?? null;
  } catch (err) {
    error.value = true;
    order.value = null;
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
  if (!order.value?.userId) return;
  window.open(`/user-management?userId=${order.value.userId}`, '_blank');
}

function openCoachDetail() {
  if (!order.value?.coachId) return;
  window.open(`/coach-management/detail/${order.value.coachId}`, '_blank');
}

function openPackageDetail() {
  if (!order.value?.packageSnapshot?.packageId) return;
  window.open(
    `/package-management/detail/${order.value.packageSnapshot.packageId}`,
    '_blank'
  );
}

function openPurchaseOrder() {
  if (!order.value?.purchaseOrderId) return;
  emit('open:order', order.value.purchaseOrderId);
}

function handleCancel() {
  ElMessage.info('取消订单功能即将上线');
}

function handleApprove() {
  if (!order.value) return;
  const defaultAmount =
    order.value.calculatedRefundAmount ?? order.value.paidAmount;
  approveRefund({ orderId: order.value.orderId, defaultAmount });
}

function handleReject() {
  if (!order.value) return;
  rejectRefund({ orderId: order.value.orderId });
}

watch(
  () => [props.visible, props.orderId] as const,
  ([visible, orderId], [prevVisible, prevOrderId]) => {
    if (!visible || orderId === undefined) {
      if (!visible) {
        order.value = null;
        error.value = false;
      }
      return;
    }
    if (orderId === prevOrderId && visible === prevVisible) {
      return;
    }
    fetchDetail();
  }
);
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="订单详情"
    width="720px"
    :close-on-click-modal="false"
    destroy-on-close
    class="order-detail-modal"
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading" class="modal-body">
      <template v-if="error || (!loading && !order)">
        <el-empty description="订单不存在或加载失败">
          <el-button type="primary" @click="fetchDetail">重试</el-button>
        </el-empty>
      </template>

      <template v-else-if="order">
        <div class="info-group">
          <div class="group-title">订单信息</div>
          <div class="info-row header-row">
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
          <div class="detail-grid three-col">
            <div class="detail-item">
              <span class="label">用户</span>
              <span class="value">
                <el-button link type="primary" @click="openUserDetail">
                  {{ order.userName || '-' }}
                </el-button>
              </span>
            </div>
            <div class="detail-item">
              <span class="label">用户手机号</span>
              <span class="value">{{ maskPhone(order.userPhone) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">教练</span>
              <span class="value">
                <el-button link type="primary" @click="openCoachDetail">
                  {{ order.coachName || '-' }}
                </el-button>
              </span>
            </div>
            <div class="detail-item">
              <span class="label">课程类型</span>
              <span class="value">{{
                getTeachingTypeLabel(order.teachingType || undefined)
              }}</span>
            </div>
            <div class="detail-item">
              <span class="label">课时数</span>
              <span class="value">
                {{ order.packageSnapshot?.totalHours ?? '-' }}
              </span>
            </div>
            <div class="detail-item">
              <span class="label">创建时间</span>
              <span class="value">{{ formatDateTime(order.createdAt) }}</span>
            </div>
          </div>
        </div>

        <div class="info-group">
          <div class="group-title">金额信息</div>
          <div class="detail-grid three-col">
            <template v-if="isPurchase">
              <div class="detail-item">
                <span class="label">应付金额</span>
                <span class="value amount">{{
                  formatOrderAmount(order.originalAmount)
                }}</span>
              </div>
              <div class="detail-item">
                <span class="label">实付金额</span>
                <span class="value amount">{{
                  formatOrderAmount(order.paidAmount)
                }}</span>
              </div>
              <div class="detail-item">
                <span class="label">优惠金额</span>
                <span class="value">{{
                  formatOrderAmount(order.discountAmount)
                }}</span>
              </div>
            </template>
            <template v-else>
              <div class="detail-item">
                <span class="label">可退金额</span>
                <span class="value amount">{{
                  formatOrderAmount(order.calculatedRefundAmount)
                }}</span>
              </div>
              <div class="detail-item">
                <span class="label">申请退款金额</span>
                <span class="value">{{
                  formatOrderAmount(order.originalAmount)
                }}</span>
              </div>
              <div class="detail-item">
                <span class="label">调整后退款金额</span>
                <span class="value amount">{{
                  formatOrderAmount(order.paidAmount)
                }}</span>
              </div>
            </template>
          </div>
        </div>

        <div class="info-group">
          <div class="group-title">
            {{ isRefund ? '退款信息' : '支付信息' }}
          </div>
          <div class="detail-grid three-col">
            <div class="detail-item">
              <span class="label">{{
                isRefund ? '退款渠道' : '支付方式'
              }}</span>
              <span class="value">{{
                formatPaymentMethod(order.paymentMethod)
              }}</span>
            </div>
            <div v-if="!isRefund" class="detail-item">
              <span class="label">支付时间</span>
              <span class="value">{{ formatDateTime(order.paidAt) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">{{
                isRefund ? '渠道退款流水号' : '第三方流水号'
              }}</span>
              <span class="value">{{ order.channelTradeNo || '-' }}</span>
            </div>
            <div v-if="isRefund" class="detail-item">
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
              <span class="label">{{ isRefund ? '退款原因' : '原因' }}</span>
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
              <span class="label">{{
                isRefund ? '审批时间' : '支付成功时间'
              }}</span>
              <span class="value">{{ formatDateTime(order.approvedAt) }}</span>
            </div>
            <div v-if="order.refundedAt" class="detail-item">
              <span class="label">退款到账时间</span>
              <span class="value">{{ formatDateTime(order.refundedAt) }}</span>
            </div>
          </div>
        </div>

        <div v-if="showPackageSection" class="info-group">
          <div class="group-title">关联套餐</div>
          <template v-if="order.packageSnapshot">
            <div class="detail-grid three-col">
              <div class="detail-item">
                <span class="label">套餐编号</span>
                <span class="value">{{
                  order.packageSnapshot.packageNo || '-'
                }}</span>
              </div>
              <div class="detail-item">
                <span class="label">状态</span>
                <span
                  class="status-tag"
                  :style="{
                    color:
                      packageStatusMap[order.packageSnapshot.status]?.color,
                    backgroundColor:
                      packageStatusMap[order.packageSnapshot.status]?.bgColor,
                  }"
                >
                  {{
                    packageStatusMap[order.packageSnapshot.status]?.label || '-'
                  }}
                </span>
              </div>
              <div class="detail-item">
                <span class="label">总课时</span>
                <span class="value">{{
                  order.packageSnapshot.totalHours
                }}</span>
              </div>
              <div class="detail-item">
                <span class="label">剩余课时</span>
                <span class="value">{{
                  order.packageSnapshot.availableCount
                }}</span>
              </div>
              <div class="detail-item">
                <span class="label">到期时间</span>
                <span class="value">{{
                  formatDateTime(order.packageSnapshot.expireAt)
                }}</span>
              </div>
              <div class="detail-item">
                <span class="label">操作</span>
                <span class="value">
                  <el-button link type="primary" @click="openPackageDetail">
                    查看套餐
                  </el-button>
                </span>
              </div>
            </div>
          </template>
          <template v-else>
            <el-empty description="暂无关联套餐数据" />
          </template>
        </div>
      </template>
    </div>

    <template #footer>
      <div class="modal-footer">
        <el-button @click="handleClose">返回</el-button>
        <el-button v-if="canCancel" type="danger" @click="handleCancel">
          取消订单
        </el-button>
        <template v-if="canApproveRefund">
          <el-button type="success" @click="handleApprove">通过</el-button>
          <el-button type="warning" @click="handleReject">驳回</el-button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.order-detail-modal {
  :deep(.el-dialog__body) {
    max-height: 60vh;
    padding-top: 12px;
    overflow-y: auto;
  }
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-group {
  padding: 20px;
  background: var(--calicat-surface, #f9fafb);
  border: 1px solid var(--calicat-divider, #f3f4f6);
  border-radius: var(--calicat-radius-card, 12px);
}

.group-title {
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--calicat-text-primary, #1d2129);
}

.header-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.order-no {
  font-size: 16px;
  font-weight: 600;
  color: var(--calicat-text-primary, #1d2129);
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

.detail-grid {
  display: grid;
  gap: 16px 24px;
}

.detail-grid.three-col {
  grid-template-columns: repeat(3, 1fr);
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
}

.detail-item .label {
  font-size: 12px;
  color: var(--calicat-text-secondary, #86939c);
}

.detail-item .value {
  color: var(--calicat-text-primary, #1d2129);
}

.detail-item .value.amount {
  font-size: 16px;
  font-weight: 600;
  color: var(--calicat-text-primary, #1d2129);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .detail-grid.three-col {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
