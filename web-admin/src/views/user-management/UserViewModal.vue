<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import type { AdminUserDetail } from '@/types/api';
import { getUserDetail } from '@/api/userManagement';
import { formatDateTime, maskPhone } from '@/utils/format';

const props = defineProps<{
  visible: boolean;
  userId: number | null;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'closed'): void;
  (e: 'edit'): void;
}>();

const user = ref<AdminUserDetail | null>(null);
const loading = ref(false);
const error = ref('');

const identityTagMap: Record<
  number,
  { label: string; color: string; bgColor: string }
> = {
  0: { label: '游客', color: '#8C8C8C', bgColor: '#F5F5F5' },
  1: { label: '注册用户', color: '#1890FF', bgColor: '#E6F7FF' },
  2: { label: '学员', color: '#52C41A', bgColor: '#F6FFED' },
};

const statusTagMap: Record<
  number,
  { label: string; color: string; bgColor: string }
> = {
  0: { label: '正常', color: '#52C41A', bgColor: '#F6FFED' },
  1: { label: '注销', color: '#8C8C8C', bgColor: '#F5F5F5' },
  2: { label: '封禁', color: '#FF4D4F', bgColor: '#FFF1F0' },
};

const profileTagMap: Record<
  boolean,
  { label: string; color: string; bgColor: string }
> = {
  true: { label: '已完善', color: '#52C41A', bgColor: '#F6FFED' },
  false: { label: '未完善', color: '#FAAD14', bgColor: '#FFFBE6' },
};

function formatGender(gender: number): string {
  return gender === 1 ? '男' : gender === 2 ? '女' : '-';
}

function formatAge(age: number | undefined): string {
  return age == null ? '未知' : String(age);
}

async function fetchDetail() {
  if (!props.userId) return;
  loading.value = true;
  error.value = '';
  try {
    const res = await getUserDetail({ userId: props.userId });
    if (res.data) {
      user.value = res.data;
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载用户资料失败';
  } finally {
    loading.value = false;
  }
}

function handleClose() {
  emit('update:visible', false);
  emit('closed');
}

function handleEdit() {
  emit('edit');
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      user.value = null;
      error.value = '';
      fetchDetail();
    }
  }
);
</script>

<template>
  <el-dialog
    v-model="props.visible"
    title="用户详情"
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

    <template v-else-if="user">
      <div class="info-card">
        <el-avatar :size="64" :src="user.avatarUrl" />
        <div class="info-main">
          <div class="info-title">{{ user.name || '未设置' }}</div>
          <div class="info-subtitle">用户 ID: {{ user.userId }}</div>
        </div>
        <div class="info-tags">
          <span
            class="status-tag"
            :style="{
              color: identityTagMap[user.identity]?.color,
              backgroundColor: identityTagMap[user.identity]?.bgColor,
            }"
          >
            {{ identityTagMap[user.identity]?.label || '-' }}
          </span>
          <span
            class="status-tag"
            :style="{
              color: statusTagMap[user.status]?.color,
              backgroundColor: statusTagMap[user.status]?.bgColor,
            }"
          >
            {{ statusTagMap[user.status]?.label || '-' }}
          </span>
          <span
            class="status-tag"
            :style="{
              color: profileTagMap[user.profileCompleted]?.color,
              backgroundColor: profileTagMap[user.profileCompleted]?.bgColor,
            }"
          >
            {{ profileTagMap[user.profileCompleted]?.label || '-' }}
          </span>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">基础信息</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">用户 ID</span>
            <span class="value">{{ user.userId }}</span>
          </div>
          <div class="detail-item">
            <span class="label">昵称/姓名</span>
            <span class="value">{{ user.name || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">手机号</span>
            <span class="value">{{ maskPhone(user.phone) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">性别</span>
            <span class="value">{{ formatGender(user.gender) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">年龄</span>
            <span class="value">{{ formatAge(user.age) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">来源</span>
            <span class="value">{{ user.source || '-' }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">游泳资料</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">是否有基础</span>
            <span class="value">{{ user.hasSwimBasis ? '有' : '无' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">游泳年限</span>
            <span class="value">{{
              user.swimYears != null ? `${user.swimYears} 年` : '-'
            }}</span>
          </div>
          <div class="detail-item">
            <span class="label">擅长泳姿</span>
            <span class="value">{{
              user.swimStrokes?.length ? user.swimStrokes.join('、') : '-'
            }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="label">个人描述</span>
            <span class="value">{{ user.personalDesc || '-' }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">监护人信息</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">监护人姓名</span>
            <span class="value">{{ user.guardianName || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">监护人手机号</span>
            <span class="value">{{
              user.guardianPhone ? maskPhone(user.guardianPhone) : '-'
            }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">账号信息</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">创建时间</span>
            <span class="value">{{ formatDateTime(user.createdAt) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">更新时间</span>
            <span class="value">{{ formatDateTime(user.updatedAt) }}</span>
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button type="primary" @click="handleEdit">编辑资料</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.form-error {
  margin: 16px 24px 0;
}

.info-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  margin: 0 24px 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.info-main {
  flex: 1;
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
  flex-wrap: wrap;
  gap: 8px;
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

.detail-item.full-width {
  grid-column: span 2;
}

.detail-item .label {
  color: #8c8c8c;
}

.detail-item .value {
  color: #262626;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
