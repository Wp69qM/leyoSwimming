<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { AdminOrderListItem, OrderType, OrderStatus } from '@/types/api';
import {
  getOrderList,
  approveRefund,
  rejectRefund,
} from '@/api/orderManagement';
import { formatDateTime } from '@/utils/format';

const router = useRouter();

const typeOptions = [
  { label: '全部', value: '' },
  { label: '购买订单', value: 'purchase' },
  { label: '退款订单', value: 'refund' },
];

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'pending_payment' },
  { label: '已支付', value: 'paid' },
  { label: '已取消', value: 'cancelled' },
  { label: '退款审批中', value: 'refund_pending' },
  { label: '退款处理中', value: 'refund_processing' },
  { label: '已退款', value: 'refunded' },
  { label: '退款被拒', value: 'rejected' },
  { label: '争议处理中', value: 'dispute_processing' },
];

const paymentMethodOptions = [
  { label: '全部', value: '' },
  { label: '微信支付', value: 'wechat' },
  { label: '支付宝', value: 'alipay' },
];

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

const queryForm = reactive({
  type: '',
  status: '',
  paymentMethod: '',
  startDate: '',
  endDate: '',
  keyword: '',
});

const tableData = ref<AdminOrderListItem[]>([]);
const loading = ref(false);
const error = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

function formatAmount(amount: string | undefined): string {
  if (amount === undefined || amount === null) return '-';
  return `¥${Number(amount).toFixed(2)}`;
}

function formatPackage(row: AdminOrderListItem): string {
  if (!row.packageId) return '-';
  return `ID:${row.packageId}`;
}

async function fetchList() {
  loading.value = true;
  error.value = false;
  try {
    const res = await getOrderList({
      page: page.value,
      pageSize: pageSize.value,
      type: (queryForm.type as OrderType) || undefined,
      status: (queryForm.status as OrderStatus) || undefined,
      paymentMethod: queryForm.paymentMethod || undefined,
      startDate: queryForm.startDate || undefined,
      endDate: queryForm.endDate || undefined,
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
  queryForm.type = '';
  queryForm.status = '';
  queryForm.paymentMethod = '';
  queryForm.startDate = '';
  queryForm.endDate = '';
  queryForm.keyword = '';
  page.value = 1;
  fetchList();
}

function handlePageChange(current: number) {
  page.value = current;
  fetchList();
}

function openDetail(row: AdminOrderListItem) {
  router.push(`/order-management/detail/${row.orderId}`);
}

function openCancel(row: AdminOrderListItem) {
  ElMessage.info('取消订单功能即将上线');
}

async function openApprove(row: AdminOrderListItem) {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入实际退款金额（可低于系统计算金额）',
      '通过退款申请',
      {
        confirmButtonText: '确认通过',
        cancelButtonText: '取消',
        inputType: 'text',
        inputValue: row.paidAmount,
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
      orderId: row.orderId,
      refundAmount: value.trim(),
      adjustReason: adjustReason.trim(),
    });
    ElMessage.success('退款申请已通过');
    fetchList();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

async function openReject(row: AdminOrderListItem) {
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
      orderId: row.orderId,
      rejectedReason: value.trim(),
    });
    ElMessage.success('退款申请已驳回，关联套餐已恢复为 active');
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
  <div class="order-management">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>套餐订单</el-breadcrumb-item>
        <el-breadcrumb-item>订单管理</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="filter-card">
      <el-form :model="queryForm" inline class="filter-form">
        <div class="filter-row">
          <el-form-item label="订单类型">
            <el-select
              v-model="queryForm.type"
              placeholder="全部"
              style="width: 140px"
              clearable
            >
              <el-option
                v-for="option in typeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="订单状态">
            <el-select
              v-model="queryForm.status"
              placeholder="全部"
              style="width: 180px"
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
          <el-form-item label="支付方式">
            <el-select
              v-model="queryForm.paymentMethod"
              placeholder="全部"
              style="width: 140px"
              clearable
            >
              <el-option
                v-for="option in paymentMethodOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间">
            <el-date-picker
              v-model="queryForm.startDate"
              type="date"
              placeholder="开始日期"
              value-format="YYYY-MM-DD"
              style="width: 160px"
            />
            <span class="date-separator">至</span>
            <el-date-picker
              v-model="queryForm.endDate"
              type="date"
              placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input
              v-model="queryForm.keyword"
              placeholder="订单号 / 用户 / 教练"
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
        <el-empty description="暂无订单数据">
          <el-button type="primary" @click="handleReset">重置筛选</el-button>
        </el-empty>
      </template>
      <el-table
        v-else
        :data="tableData"
        header-row-class-name="table-header"
        row-class-name="table-row"
        style="width: 100%"
      >
        <el-table-column label="订单号" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">
              {{ row.orderNo }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="类型" align="center" width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: orderTypeMap[row.type]?.color,
                backgroundColor: orderTypeMap[row.type]?.bgColor,
              }"
            >
              {{ orderTypeMap[row.type]?.label || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="100">
          <template #default="{ row }">
            {{ row.userName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="教练" width="100">
          <template #default="{ row }">
            {{ row.coachName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="套餐" width="140">
          <template #default="{ row }">
            {{ formatPackage(row) }}
          </template>
        </el-table-column>
        <el-table-column label="金额" align="right" width="120">
          <template #default="{ row }">
            {{ formatAmount(row.paidAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="120">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: orderStatusMap[row.status]?.color,
                backgroundColor: orderStatusMap[row.status]?.bgColor,
              }"
            >
              {{ orderStatusMap[row.status]?.label || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">
              查看
            </el-button>
            <el-button
              v-if="row.type === 'purchase' && row.status === 'pending_payment'"
              link
              type="danger"
              @click="openCancel(row)"
            >
              取消
            </el-button>
            <el-button
              v-if="row.type === 'refund' && row.status === 'refund_pending'"
              link
              type="success"
              @click="openApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.type === 'refund' && row.status === 'refund_pending'"
              link
              type="warning"
              @click="openReject(row)"
            >
              驳回
            </el-button>
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
  </div>
</template>

<style scoped lang="scss">
.order-management {
  padding-bottom: 24px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  height: 48px;
  margin-bottom: 16px;
}

.filter-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.filter-form {
  flex: 1;
  margin-bottom: 0;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}

.filter-actions {
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
  justify-content: center;
  padding-top: 16px;
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

:deep(.el-form--inline) {
  .el-form-item {
    margin-right: 0;
    margin-bottom: 0;
  }

  .el-form-item__label {
    padding-right: 8px;
    font-size: 14px;
    color: #262626;
  }
}

:deep(.el-table__body) {
  .el-table__row:last-child td {
    border-bottom: none;
  }
}
</style>
