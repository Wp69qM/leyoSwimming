<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type {
  AdminPackageTemplateListItem,
  AdminPackageTemplateCustomConfigResponse,
} from '@/types/api';
import {
  getPackageTemplateList,
  togglePackageTemplateStatus,
  getCustomPackageConfig,
  saveCustomPackageConfig,
} from '@/api/packageTemplate';
import PackageTemplateEditModal from './PackageTemplateEditModal.vue';

const activeTab = ref('standard');

const modeOptions = [
  { label: '全部', value: '' },
  { label: '正价套餐', value: 'standard' },
  { label: '体验课', value: 'experience' },
];

const statusOptions = [
  { label: '全部', value: '' },
  { label: '已上架', value: 'active' },
  { label: '未上架', value: 'inactive' },
];

const teachingTypeMap: Record<string, string> = {
  one_on_one: '一对一',
  one_on_two: '一对二',
  one_on_three: '一对三',
};

const statusMap: Record<
  string,
  { label: string; color: string; bgColor: string }
> = {
  active: { label: '已上架', color: '#00B42A', bgColor: '#F0FFF5' },
  inactive: { label: '未上架', color: '#86909C', bgColor: '#F7F8FA' },
};

const modeMap: Record<
  string,
  { label: string; color: string; bgColor: string }
> = {
  standard: { label: '正价套餐', color: '#165DFF', bgColor: '#EFF6FF' },
  experience: { label: '体验课', color: '#FF7D00', bgColor: '#FFF7ED' },
};

const queryForm = reactive({
  packageMode: '',
  status: '',
  keyword: '',
});

const tableData = ref<AdminPackageTemplateListItem[]>([]);
const loading = ref(false);
const error = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

const modalVisible = ref(false);
const modalMode = ref<'add' | 'edit' | 'view'>('add');
const modalTemplateId = ref<number | undefined>(undefined);

const customForm = reactive({
  minHours: undefined as number | undefined,
  maxHours: undefined as number | undefined,
  defaultValidDays: undefined as number | undefined,
});
const customOriginal = reactive({ ...customForm });
const customLoading = ref(false);
const customSaving = ref(false);
const customError = ref(false);

async function fetchList() {
  loading.value = true;
  error.value = false;
  try {
    const res = await getPackageTemplateList({
      page: page.value,
      pageSize: pageSize.value,
      packageMode: queryForm.packageMode || undefined,
      status: queryForm.status || undefined,
      keyword: queryForm.keyword || undefined,
    });
    if (res.data) {
      tableData.value = res.data.items;
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
  queryForm.packageMode = '';
  queryForm.status = '';
  queryForm.keyword = '';
  page.value = 1;
  fetchList();
}

function handlePageChange(current: number) {
  page.value = current;
  fetchList();
}

function handleSizeChange() {
  page.value = 1;
  fetchList();
}

function openAddModal() {
  modalMode.value = 'add';
  modalTemplateId.value = undefined;
  modalVisible.value = true;
}

function openEditModal(row: AdminPackageTemplateListItem) {
  if (row.status === 'active') {
    openViewModal(row);
    return;
  }
  modalMode.value = 'edit';
  modalTemplateId.value = row.packageTemplateId;
  modalVisible.value = true;
}

function openViewModal(row: AdminPackageTemplateListItem) {
  modalMode.value = 'view';
  modalTemplateId.value = row.packageTemplateId;
  modalVisible.value = true;
}

function handleModalSuccess() {
  fetchList();
}

async function handleToggleStatus(row: AdminPackageTemplateListItem) {
  const isActive = row.status === 'active';
  const action = isActive ? '下架' : '上架';
  try {
    await ElMessageBox.confirm(`确定要${action}「${row.name}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await togglePackageTemplateStatus({
      packageTemplateId: row.packageTemplateId,
    });
    ElMessage.success(`${action}成功`);
    fetchList();
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err instanceof Error ? err.message : '操作失败');
    }
  }
}

function formatCoaches(row: AdminPackageTemplateListItem): string {
  if (!row.coachNames || row.coachNames.length === 0) return '-';
  if (row.coachNames.length <= 2) return row.coachNames.join('、');
  return `${row.coachNames.slice(0, 2).join('、')} 等${row.coachNames.length}人`;
}

function formatPrice(value: number | undefined): string {
  if (value === undefined || value === null) return '-';
  return `¥${Number(value).toFixed(2)}`;
}

async function fetchCustomConfig() {
  customLoading.value = true;
  customError.value = false;
  try {
    const res = await getCustomPackageConfig();
    if (res.data) {
      customForm.minHours = res.data.minHours ?? undefined;
      customForm.maxHours = res.data.maxHours ?? undefined;
      customForm.defaultValidDays = res.data.defaultValidDays ?? undefined;
      Object.assign(customOriginal, customForm);
    }
  } catch (err) {
    customError.value = true;
    ElMessage.error(err instanceof Error ? err.message : '加载失败');
  } finally {
    customLoading.value = false;
  }
}

function validateCustomForm(): boolean {
  const { minHours, maxHours, defaultValidDays } = customForm;
  if (
    minHours === undefined ||
    maxHours === undefined ||
    defaultValidDays === undefined
  ) {
    ElMessage.warning('请填写完整配置');
    return false;
  }
  if (minHours <= 0 || minHours > maxHours || maxHours > 100) {
    ElMessage.warning('最小课时需大于 0 且不超过最大课时，最大课时不超过 100');
    return false;
  }
  if (defaultValidDays <= 0) {
    ElMessage.warning('默认有效期需大于 0 天');
    return false;
  }
  return true;
}

async function handleSaveCustomConfig() {
  if (!validateCustomForm()) return;
  customSaving.value = true;
  try {
    const res = await saveCustomPackageConfig({
      minHours: customForm.minHours as number,
      maxHours: customForm.maxHours as number,
      defaultValidDays: customForm.defaultValidDays as number,
    });
    if (res.data) {
      const data: AdminPackageTemplateCustomConfigResponse = res.data;
      customForm.minHours = data.minHours;
      customForm.maxHours = data.maxHours;
      customForm.defaultValidDays = data.defaultValidDays;
      Object.assign(customOriginal, customForm);
    }
    ElMessage.success('保存成功');
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败');
  } finally {
    customSaving.value = false;
  }
}

function handleResetCustomConfig() {
  customForm.minHours = customOriginal.minHours;
  customForm.maxHours = customOriginal.maxHours;
  customForm.defaultValidDays = customOriginal.defaultValidDays;
}

onMounted(() => {
  fetchList();
  fetchCustomConfig();
});
</script>

<template>
  <div class="package-config">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>套餐订单</el-breadcrumb-item>
        <el-breadcrumb-item>套餐配置</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-tabs v-model="activeTab" class="config-tabs">
      <el-tab-pane label="标准套餐" name="standard">
        <div class="filter-card">
          <el-form :model="queryForm" inline class="filter-form">
            <div class="filter-row">
              <el-form-item label="套餐模式">
                <el-select
                  v-model="queryForm.packageMode"
                  placeholder="全部"
                  style="width: 160px"
                  clearable
                >
                  <el-option
                    v-for="option in modeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select
                  v-model="queryForm.status"
                  placeholder="全部"
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
              <el-form-item label="关键词">
                <el-input
                  v-model="queryForm.keyword"
                  placeholder="按套餐名称搜索"
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
          <div class="filter-extra">
            <el-button type="primary" @click="openAddModal"
              >新增标准套餐</el-button
            >
          </div>
        </div>

        <div class="table-card">
          <el-skeleton v-if="loading" :rows="4" animated />
          <template v-else-if="error">
            <el-empty description="加载失败">
              <el-button type="primary" @click="fetchList">重试</el-button>
            </el-empty>
          </template>
          <template v-else-if="tableData.length === 0">
            <el-empty description="暂无标准套餐数据">
              <el-button type="primary" @click="openAddModal"
                >新增标准套餐</el-button
              >
            </el-empty>
          </template>
          <el-table
            v-else
            :data="tableData"
            header-row-class-name="table-header"
            row-class-name="table-row"
            style="width: 100%"
          >
            <el-table-column label="套餐编号" width="120">
              <template #default="{ row }">
                <span class="text-secondary">{{ row.packageTemplateId }}</span>
              </template>
            </el-table-column>
            <el-table-column label="名称" min-width="160">
              <template #default="{ row }">
                <span class="table-name">{{ row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column label="套餐模式" align="center" width="110">
              <template #default="{ row }">
                <span
                  class="mode-tag"
                  :style="{
                    color: modeMap[row.packageMode]?.color,
                    backgroundColor: modeMap[row.packageMode]?.bgColor,
                  }"
                >
                  {{ modeMap[row.packageMode]?.label || '-' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="适用教练" min-width="160">
              <template #default="{ row }">
                {{ formatCoaches(row) }}
              </template>
            </el-table-column>
            <el-table-column label="教学类型" align="center" width="100">
              <template #default="{ row }">
                {{
                  teachingTypeMap[row.teachingType] || row.teachingType || '-'
                }}
              </template>
            </el-table-column>
            <el-table-column label="课时数" align="center" width="90">
              <template #default="{ row }">
                {{ row.totalHours }}
              </template>
            </el-table-column>
            <el-table-column label="有效期" align="center" width="90">
              <template #default="{ row }"> {{ row.validDays }}天 </template>
            </el-table-column>
            <el-table-column label="售价" align="right" width="120">
              <template #default="{ row }">
                <span class="price-text">{{ formatPrice(row.price) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" align="center" width="100">
              <template #default="{ row }">
                <span
                  class="status-tag"
                  :style="{
                    color: statusMap[row.status]?.color,
                    backgroundColor: statusMap[row.status]?.bgColor,
                  }"
                >
                  {{ statusMap[row.status]?.label || '-' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              align="center"
              width="180"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button link type="primary" @click="openViewModal(row)">
                  查看
                </el-button>
                <el-button
                  v-if="row.status === 'inactive'"
                  link
                  type="primary"
                  @click="openEditModal(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="row.status === 'inactive'"
                  link
                  type="success"
                  @click="handleToggleStatus(row)"
                >
                  上架
                </el-button>
                <el-button
                  v-if="row.status === 'active'"
                  link
                  type="warning"
                  @click="handleToggleStatus(row)"
                >
                  下架
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
              @size-change="handleSizeChange"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="自定义套餐规则" name="custom">
        <div class="form-card">
          <el-skeleton v-if="customLoading" :rows="3" animated />
          <template v-else-if="customError">
            <el-empty description="加载失败">
              <el-button type="primary" @click="fetchCustomConfig"
                >重试</el-button
              >
            </el-empty>
          </template>
          <el-form
            v-else
            :model="customForm"
            label-width="140px"
            class="custom-form"
          >
            <el-form-item label="最小课时数">
              <el-input-number
                v-model="customForm.minHours"
                :min="1"
                :max="100"
                placeholder="最小课时数"
              />
            </el-form-item>
            <el-form-item label="最大课时数">
              <el-input-number
                v-model="customForm.maxHours"
                :min="1"
                :max="100"
                placeholder="最大课时数"
              />
            </el-form-item>
            <el-form-item label="默认有效期">
              <el-input-number
                v-model="customForm.defaultValidDays"
                :min="1"
                placeholder="天"
              />
            </el-form-item>
            <el-form-item>
              <el-button @click="handleResetCustomConfig">重置</el-button>
              <el-button
                type="primary"
                :loading="customSaving"
                @click="handleSaveCustomConfig"
              >
                保存
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>

    <PackageTemplateEditModal
      v-model:visible="modalVisible"
      :mode="modalMode"
      :template-id="modalTemplateId"
      @success="handleModalSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
.package-config {
  padding-bottom: 24px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  height: 48px;
  margin-bottom: 16px;
}

.config-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }
}

.filter-card {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
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

.filter-extra {
  display: flex;
  align-items: center;
}

.table-card {
  padding: 16px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.form-card {
  max-width: 560px;
  padding: 24px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.custom-form {
  :deep(.el-form-item__label) {
    color: var(--calicat-text-primary);
  }
}

.table-name {
  font-weight: 500;
  color: var(--calicat-text-primary);
}

.mode-tag,
.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  padding: 0 10px;
  font-size: 12px;
  border-radius: 12px;
}

.price-text {
  font-weight: 600;
  color: var(--calicat-text-primary);
}

.text-secondary {
  color: var(--calicat-text-secondary);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding-top: 16px;
}

:deep(.table-header) {
  th {
    height: 46px;
    font-size: 13px;
    font-weight: 600;
    color: var(--calicat-text-primary);
    background: var(--calicat-table-header-bg);
  }
}

:deep(.table-row) {
  td {
    height: 57px;
    border-bottom: 1px solid var(--calicat-border);
  }
}

:deep(.el-form--inline) {
  .el-form-item {
    margin-right: 0;
    margin-bottom: 0;
  }

  .el-form-item__label {
    padding-right: 8px;
    font-size: 13px;
    color: var(--calicat-text-primary);
  }
}
</style>
