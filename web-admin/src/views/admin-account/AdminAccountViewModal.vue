<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useAdminAuthStore } from '@/stores/adminAuth';
import type { AdminAccountDetail, AdminAccountAuditLog } from '@/types/api';
import { getAdminAccountDetail } from '@/api/adminAccountManagement';
import { formatDateTime } from '@/utils/format';

const props = defineProps<{
  visible: boolean;
  adminId: number | null;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'closed'): void;
  (e: 'edit', adminId: number): void;
}>();

const authStore = useAdminAuthStore();
const isSuperAdmin = computed(() => authStore.admin?.role === 'super_admin');

const admin = ref<AdminAccountDetail | null>(null);
const loading = ref(false);
const error = ref('');

const roleTagMap: Record<
  string,
  { label: string; color: string; bgColor: string }
> = {
  super_admin: { label: '超级管理员', color: '#1890FF', bgColor: '#E6F7FF' },
  admin: { label: '普通管理员', color: '#52C41A', bgColor: '#F6FFED' },
};

const statusTagMap: Record<
  number,
  { label: string; color: string; bgColor: string }
> = {
  0: { label: '启用', color: '#52C41A', bgColor: '#F6FFED' },
  1: { label: '禁用', color: '#FF4D4F', bgColor: '#FFF1F0' },
};

const actionLabelMap: Record<string, string> = {
  CREATE: '创建',
  UPDATE: '编辑',
  DISABLE: '禁用',
  ENABLE: '启用',
  DELETE: '删除',
  RESET_PASSWORD: '重置密码',
};

const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
});

function isSelf(adminDetail: AdminAccountDetail): boolean {
  return adminDetail.adminId === authStore.admin?.id;
}

function canEdit(adminDetail: AdminAccountDetail): boolean {
  return isSuperAdmin.value && !isSelf(adminDetail);
}

function formatLastLogin(value: string | undefined): string {
  return value ? formatDateTime(value) : '从未登录';
}

function formatAuditLog(log: AdminAccountAuditLog): string {
  const action = actionLabelMap[log.action] || log.action;
  const reason = log.reason ? ` - ${log.reason}` : '';
  return `${action}${reason}`;
}

async function fetchDetail() {
  if (!props.adminId) return;
  loading.value = true;
  error.value = '';
  try {
    const res = await getAdminAccountDetail({ adminId: props.adminId });
    if (res.data) {
      admin.value = res.data;
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载管理员资料失败';
  } finally {
    loading.value = false;
  }
}

function handleClose() {
  emit('update:visible', false);
  emit('closed');
}

function handleEdit() {
  if (admin.value) {
    emit('edit', admin.value.adminId);
  }
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      admin.value = null;
      error.value = '';
      fetchDetail();
    }
  }
);
</script>

<template>
  <el-dialog
    v-model="localVisible"
    title="管理员详情"
    width="560px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="form-error"
    />

    <el-skeleton v-if="loading" :rows="5" animated />

    <template v-else-if="admin">
      <div class="info-card">
        <div class="info-title">{{ admin.name || '未设置' }}</div>
        <div class="info-subtitle">管理员 ID: {{ admin.adminId }}</div>
        <div class="info-tags">
          <span
            class="status-tag"
            :style="{
              color: roleTagMap[admin.role]?.color,
              backgroundColor: roleTagMap[admin.role]?.bgColor,
            }"
          >
            {{ roleTagMap[admin.role]?.label || admin.role }}
          </span>
          <span
            class="status-tag"
            :style="{
              color: statusTagMap[admin.status]?.color,
              backgroundColor: statusTagMap[admin.status]?.bgColor,
            }"
          >
            {{ statusTagMap[admin.status]?.label || '-' }}
          </span>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">基础信息</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">管理员 ID</span>
            <span class="value">{{ admin.adminId }}</span>
          </div>
          <div class="detail-item">
            <span class="label">姓名</span>
            <span class="value">{{ admin.name || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">登录账号</span>
            <span class="value">{{ admin.username }}</span>
          </div>
          <div class="detail-item">
            <span class="label">角色</span>
            <span class="value">{{
              roleTagMap[admin.role]?.label || admin.role
            }}</span>
          </div>
          <div class="detail-item">
            <span class="label">状态</span>
            <span class="value">{{
              statusTagMap[admin.status]?.label || '-'
            }}</span>
          </div>
          <div class="detail-item">
            <span class="label">最近登录时间</span>
            <span class="value">{{ formatLastLogin(admin.lastLoginAt) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">创建时间</span>
            <span class="value">{{ formatDateTime(admin.createdAt) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">更新时间</span>
            <span class="value">{{ formatDateTime(admin.updatedAt) }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">操作日志（最近 5 条）</div>
        <el-timeline v-if="admin.auditLogs && admin.auditLogs.length > 0">
          <el-timeline-item
            v-for="log in admin.auditLogs"
            :key="log.logId"
            :timestamp="formatDateTime(log.createdAt)"
          >
            <div class="log-content">
              <span class="log-operator">{{ log.operatorName || '系统' }}</span>
              <span class="log-action">{{ formatAuditLog(log) }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无操作日志" />
      </div>
    </template>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button
          v-if="admin && canEdit(admin)"
          type="primary"
          @click="handleEdit"
        >
          编辑资料
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.form-error {
  margin: 16px 24px 0;
}

.info-card {
  padding: 24px;
  margin: 0 24px 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.info-title {
  font-size: 20px;
  font-weight: 500;
  color: #262626;
}

.info-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.info-tags {
  display: flex;
  gap: 8px;
  margin-top: 12px;
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

.detail-section {
  padding: 0 24px 16px;
}

.section-title {
  margin-bottom: 16px;
  font-size: 14px;
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

.log-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.log-operator {
  font-size: 14px;
  color: #262626;
}

.log-action {
  font-size: 12px;
  color: #595959;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
