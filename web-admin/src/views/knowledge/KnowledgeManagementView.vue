<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import type { KnowledgeListItem } from '@/types/api';
import {
  getKnowledgeList,
  toggleKnowledgeStatus,
  deleteKnowledge,
} from '@/api/knowledgeManagement';
import { formatDateTime } from '@/utils/format';
import KnowledgeUploadModal from './components/KnowledgeUploadModal.vue';
import KnowledgeDetailModal from './components/KnowledgeDetailModal.vue';

const categoryOptions = [
  { label: '全部', value: '' },
  { label: '安全', value: 'safety' },
  { label: '技巧', value: 'technique' },
  { label: '急救', value: 'emergency' },
  { label: '其他', value: 'other' },
];

const categoryTagMap: Record<
  string,
  { label: string; color: string; bgColor: string }
> = {
  safety: { label: '安全', color: '#1890FF', bgColor: '#E6F7FF' },
  technique: { label: '技巧', color: '#52C41A', bgColor: '#F6FFED' },
  emergency: { label: '急救', color: '#FF4D4F', bgColor: '#FFF1F0' },
  other: { label: '其他', color: '#595959', bgColor: '#F5F5F5' },
};

const statusOptions = [
  { label: '全部', value: null },
  { label: '启用', value: 0 },
  { label: '禁用', value: 1 },
];

const statusTagMap: Record<
  number,
  { label: string; color: string; bgColor: string }
> = {
  0: { label: '启用', color: '#52C41A', bgColor: '#F6FFED' },
  1: { label: '禁用', color: '#FF4D4F', bgColor: '#FFF1F0' },
};

const queryForm = reactive({
  category: '',
  status: null as number | null,
  keyword: '',
});

const tableData = ref<KnowledgeListItem[]>([]);
const loading = ref(false);
const error = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

const uploadVisible = ref(false);
const detailVisible = ref(false);
const detailDocumentId = ref(0);

async function fetchList() {
  loading.value = true;
  error.value = false;
  try {
    const res = await getKnowledgeList({
      page: page.value,
      pageSize: pageSize.value,
      category: (queryForm.category || undefined) as
        | 'safety'
        | 'technique'
        | 'emergency'
        | 'other'
        | undefined,
      status: queryForm.status,
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
  queryForm.category = '';
  queryForm.status = null;
  queryForm.keyword = '';
  page.value = 1;
  fetchList();
}

function handlePageChange(current: number) {
  page.value = current;
  fetchList();
}

function openUpload() {
  uploadVisible.value = true;
}

function openDetail(row: KnowledgeListItem) {
  detailDocumentId.value = row.documentId;
  detailVisible.value = true;
}

function handleUploadSuccess() {
  uploadVisible.value = false;
  ElMessage.success('上传成功');
  fetchList();
}

async function handleToggleStatus(row: KnowledgeListItem) {
  const targetStatus = row.status === 0 ? 1 : 0;
  const actionLabel = targetStatus === 1 ? '禁用' : '启用';
  try {
    await ElMessageBox.confirm(
      `确认${actionLabel}文档「${row.title}」？`,
      `${actionLabel}文档`,
      {
        confirmButtonText: `确认${actionLabel}`,
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    await toggleKnowledgeStatus({
      documentId: row.documentId,
      status: targetStatus,
    });
    ElMessage.success(`文档已${actionLabel}`);
    fetchList();
  } catch (err) {
    if (err instanceof Error && err.message !== 'cancel') {
      ElMessage.error(err.message);
    }
  }
}

async function handleDelete(row: KnowledgeListItem) {
  try {
    await ElMessageBox.confirm(
      '删除后该文档将不再被 AI 助理引用，是否继续？',
      '确认删除',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    await deleteKnowledge({ documentId: row.documentId });
    ElMessage.success('文档已删除');
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
  <div class="knowledge-management">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>系统配置</el-breadcrumb-item>
        <el-breadcrumb-item>知识库管理</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="filter-card">
      <el-form :model="queryForm" inline class="filter-form">
        <el-form-item>
          <el-select
            v-model="queryForm.category"
            placeholder="全部分类"
            style="width: 160px"
            clearable
          >
            <el-option
              v-for="option in categoryOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="queryForm.status"
            placeholder="全部状态"
            style="width: 160px"
            clearable
          >
            <el-option
              v-for="option in statusOptions"
              :key="String(option.value)"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="queryForm.keyword"
            placeholder="请输入文档标题"
            clearable
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-button" @click="handleSearch">
            查询
          </el-button>
          <el-button data-testid="reset-button" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="operation-bar">
        <el-button type="primary" :icon="Plus" data-testid="add-button" @click="openUpload">
          新增文档
        </el-button>
      </div>
    </div>

    <div class="table-card">
      <el-skeleton v-if="loading" :rows="3" animated />
      <template v-else-if="error">
        <el-empty description="加载失败">
          <el-button type="primary" @click="fetchList">重试</el-button>
        </el-empty>
      </template>
      <template v-else-if="!tableData || tableData.length === 0">
        <el-empty description="暂无知识库文档">
          <el-button type="primary" @click="openUpload">新增文档</el-button>
        </el-empty>
      </template>
      <el-table
        v-else
        :data="tableData"
        stripe
        header-row-class-name="table-header"
        style="width: 100%"
      >
        <el-table-column label="标题" min-width="180">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :data-testid="`title-button-${row.documentId}`"
              @click="openDetail(row)"
            >
              {{ row.title }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="分类" align="center" min-width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: categoryTagMap[row.category]?.color,
                backgroundColor: categoryTagMap[row.category]?.bgColor,
              }"
            >
              {{ categoryTagMap[row.category]?.label || row.category }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" min-width="100">
          <template #default="{ row }">
            <span
              class="status-tag"
              :style="{
                color: statusTagMap[row.status]?.color,
                backgroundColor: statusTagMap[row.status]?.bgColor,
              }"
            >
              {{ statusTagMap[row.status]?.label || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="上传人" prop="createdBy" align="center" min-width="120" />
        <el-table-column label="上传时间" align="center" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          align="center"
          min-width="140"
          fixed="right"
          class-name="operation-cell"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :data-testid="`toggle-button-${row.documentId}`"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 0 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              :data-testid="`delete-button-${row.documentId}`"
              @click="handleDelete(row)"
            >
              删除
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

    <KnowledgeUploadModal
      v-model:visible="uploadVisible"
      @success="handleUploadSuccess"
    />
    <KnowledgeDetailModal
      v-model:visible="detailVisible"
      :document-id="detailDocumentId"
    />
  </div>
</template>

<style scoped lang="scss">
.knowledge-management {
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
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.filter-form {
  margin-bottom: 0;
}

.operation-bar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
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
  align-items: center;
  justify-content: center;
  padding: 16px 8px 0;
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

:deep(.el-table__cell.operation-cell .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  height: 100%;

  .el-button + .el-button {
    margin-left: 0;
  }
}
</style>
