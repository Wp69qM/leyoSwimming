<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Download, Document } from '@element-plus/icons-vue';
import type { KnowledgeDetail } from '@/types/api';
import {
  getKnowledgeDetail,
  downloadKnowledgeFile,
} from '@/api/knowledgeManagement';
import { formatDateTime } from '@/utils/format';

const props = defineProps({
  visible: {
    type: Boolean,
    required: true,
  },
  documentId: {
    type: Number,
    default: 0,
  },
});

const emit = defineEmits(['update:visible']);

const loading = ref(false);
const downloading = ref(false);
const detail = ref<KnowledgeDetail | null>(null);

const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
});

const categoryMap: Record<string, string> = {
  safety: '安全',
  technique: '技巧',
  emergency: '急救',
  other: '其他',
};

const contentTypeMap: Record<string, string> = {
  text: '纯文本',
  markdown: 'Markdown',
};

const sourceTypeMap: Record<string, string> = {
  manual: '手动输入',
  file: '文件上传',
};

const statusMap: Record<number, string> = {
  0: '启用',
  1: '禁用',
};

const isFileSource = computed(() => detail.value?.sourceType === 'file');
const isMarkdown = computed(() => detail.value?.contentType === 'markdown');

async function fetchDetail() {
  if (!props.documentId) {
    detail.value = null;
    return;
  }

  loading.value = true;
  try {
    const res = await getKnowledgeDetail({ documentId: props.documentId });
    detail.value = res.data ?? null;
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载详情失败');
    detail.value = null;
  } finally {
    loading.value = false;
  }
}

async function handleDownload() {
  if (!props.documentId) return;
  downloading.value = true;
  try {
    await downloadKnowledgeFile({ documentId: props.documentId });
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '下载失败');
  } finally {
    downloading.value = false;
  }
}

function handleClose() {
  emit('update:visible', false);
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      fetchDetail();
    } else {
      detail.value = null;
    }
  }
);

onMounted(() => {
  if (props.visible) {
    fetchDetail();
  }
});
</script>

<template>
  <el-dialog
    v-model="localVisible"
    title="文档详情"
    width="640px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else-if="detail">
      <div class="detail-header">
        <el-icon class="detail-icon"><Document /></el-icon>
        <div class="detail-title-wrap">
          <div class="detail-title">{{ detail.title }}</div>
          <div class="detail-meta">
            <span class="meta-item">分类：{{ categoryMap[detail.category] || detail.category }}</span>
            <span class="meta-item">来源：{{ sourceTypeMap[detail.sourceType] || detail.sourceType }}</span>
            <span class="meta-item">状态：{{ statusMap[detail.status] || detail.status }}</span>
          </div>
        </div>
      </div>

      <div class="detail-info">
        <div class="info-row">
          <span class="info-label">内容类型：</span>
          <span>{{ contentTypeMap[detail.contentType] || detail.contentType }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">上传时间：</span>
          <span>{{ formatDateTime(detail.createdAt) }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">更新时间：</span>
          <span>{{ formatDateTime(detail.updatedAt) }}</span>
        </div>
      </div>

      <div class="detail-content">
        <div class="content-label">文档内容</div>
        <el-input
          v-model="detail.content"
          type="textarea"
          :rows="12"
          readonly
          resize="none"
          class="content-textarea"
        />
      </div>
    </template>
    <el-empty v-else description="暂无数据" />

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button
          v-if="isFileSource"
          type="primary"
          :icon="Download"
          :loading="downloading"
          @click="handleDownload"
        >
          下载文件{{ isMarkdown ? '.md' : '.txt' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.detail-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-icon {
  font-size: 40px;
  color: #1890ff;
}

.detail-title-wrap {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
}

.detail-title {
  font-size: 18px;
  font-weight: 500;
  color: #262626;
  word-break: break-all;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: #595959;
}

.detail-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 16px;
  margin-bottom: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.info-row {
  display: flex;
  font-size: 14px;
  color: #262626;
}

.info-label {
  flex-shrink: 0;
  color: #595959;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.content-label {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.content-textarea {
  :deep(.el-textarea__inner) {
    font-family: 'SF Mono', Monaco, Consolas, 'Courier New', monospace;
    font-size: 13px;
    color: #262626;
    background: #fafafa;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
