<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useAdminAuthStore } from '@/stores/adminAuth';
import type { AdminAccountDetail, AdminAccountAuditLog } from '@/types/api';
import { getAdminAccountDetail } from '@/api/adminAccountManagement';
import { formatDateTime } from '@/utils/format';
import { UserFilled, Clock } from '@element-plus/icons-vue';

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
    width="560px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <template #header>
      <div class="dialog-header">
        <el-icon class="dialog-header-icon" :size="18"><UserFilled /></el-icon>
        <div class="dialog-header-text">
          <div class="dialog-title">管理员详情</div>
          <div v-if="admin" class="dialog-subtitle">
            账号：{{ admin.username }}
          </div>
        </div>
      </div>
    </template>

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
        <div class="section-title">
          <el-icon><UserFilled /></el-icon>
          <span>基本信息</span>
        </div>
        <div class="detail-rows">
          <div class="detail-row">
            <span class="label">登录账号</span>
            <span class="value">{{ admin.username }}</span>
          </div>
          <div class="detail-row">
            <span class="label">姓名</span>
            <span class="value">{{ admin.name || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">手机号</span>
            <span class="value">{{ admin.phone || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">角色</span>
            <span class="value status-value">
              <span
                class="status-tag"
                :style="{
                  color: roleTagMap[admin.role]?.color,
                  backgroundColor: roleTagMap[admin.role]?.bgColor,
                }"
              >
                {{ roleTagMap[admin.role]?.label || admin.role }}
              </span>
            </span>
          </div>
          <div class="detail-row">
            <span class="label">账号状态</span>
            <span class="value status-value">
              <span
                class="status-tag"
                :style="{
                  color: statusTagMap[admin.status]?.color,
                  backgroundColor: statusTagMap[admin.status]?.bgColor,
                }"
              >
                {{ statusTagMap[admin.status]?.label || '-' }}
              </span>
            </span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">
          <el-icon><Clock /></el-icon>
          <span>登录信息</span>
        </div>
        <div class="detail-rows">
          <div class="detail-row">
            <span class="label">最后登录</span>
            <span class="value">{{ formatLastLogin(admin.lastLoginAt) }}</span>
          </div>
          <div class="detail-row">
            <span class="label">登录 IP</span>
            <span class="value">{{ admin.lastLoginIp || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">创建时间</span>
            <span class="value">{{ formatDateTime(admin.createdAt) }}</span>
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
.dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dialog-header-icon {
  color: #1890ff;
}

.dialog-header-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dialog-title {
  font-size: 18px;
  font-weight: 500;
  color: #262626;
  line-height: 1.2;
}

.dialog-subtitle {
  font-size: 13px;
  color: #86909c;
  line-height: 1.2;
}

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
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 500;
  color: #262626;
}

.detail-rows {
  display: flex;
  flex-direction: column;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.detail-row {
  display: flex;
  align-items: center;
  min-height: 45px;
  font-size: 14px;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.detail-row .label {
  display: flex;
  align-items: center;
  width: 120px;
  padding: 12px 16px;
  color: #86909c;
  background: #fafafa;
}

.detail-row .value {
  flex: 1;
  padding: 12px 16px;
  color: #262626;
}

.detail-row .status-value {
  padding: 8px 16px;
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
