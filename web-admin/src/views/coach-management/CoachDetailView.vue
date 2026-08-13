<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { AdminCoachDetail } from '@/types/api';
import { getCoachDetail, cancelCoachEntry } from '@/api/coachManagement';
import { formatDateTime, maskPhone, maskIdCard } from '@/utils/format';
import CoachEditModal from './CoachEditModal.vue';

const route = useRoute();
const router = useRouter();

const coachId = Number(route.params.coachId);
const coach = ref<AdminCoachDetail | null>(null);
const loading = ref(false);
const error = ref(false);
const activeTab = ref('basic');
const editVisible = ref(false);

const coachStatusMap: Record<
  number,
  { label: string; color: string; bgColor: string }
> = {
  0: { label: '待审核', color: '#FAAD14', bgColor: '#FFFBE6' },
  1: { label: '已通过', color: '#52C41A', bgColor: '#F6FFED' },
  2: { label: '已驳回', color: '#FF4D4F', bgColor: '#FFF1F0' },
  3: { label: '已离职', color: '#8C8C8C', bgColor: '#F5F5F5' },
  4: { label: '申请离职中', color: '#1890FF', bgColor: '#E6F7FF' },
};

const realtimeStatusMap: Record<
  string,
  { label: string; color: string; bgColor: string }
> = {
  空闲中: { label: '空闲中', color: '#52C41A', bgColor: '#F6FFED' },
  上课中: { label: '上课中', color: '#1890FF', bgColor: '#E6F7FF' },
  休息中: { label: '休息中', color: '#FAAD14', bgColor: '#FFFBE6' },
  已下班: { label: '已下班', color: '#8C8C8C', bgColor: '#F5F5F5' },
  请假中: { label: '请假中', color: '#722ED1', bgColor: '#F9F0FF' },
};

function formatGender(gender: string): string {
  return gender === 'MALE' ? '男' : gender === 'FEMALE' ? '女' : '-';
}

function findCertUrl(certType: string): string {
  return (
    coach.value?.certificates.find((item) => item.certType === certType)
      ?.imageUrl || ''
  );
}

function formatStrokes(strokes: string | undefined): string {
  return strokes ? strokes.replace(/,/g, '、') : '-';
}

async function fetchDetail() {
  if (Number.isNaN(coachId)) {
    error.value = true;
    return;
  }
  loading.value = true;
  error.value = false;
  try {
    const res = await getCoachDetail({ coachId });
    if (res.data) {
      coach.value = res.data;
    }
  } catch (err) {
    error.value = true;
    ElMessage.error(err instanceof Error ? err.message : '加载失败');
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push('/coach-management');
}

function handleEdit() {
  editVisible.value = true;
}

function handleEditSuccess() {
  editVisible.value = false;
  ElMessage.success('教练资料已更新');
  fetchDetail();
}

function handleSchedule() {
  ElMessage.info('排班管理功能即将上线');
}

async function handleCancelEntry() {
  if (!coach.value) return;
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入取消入驻原因，该操作将标记教练为已离职',
      '取消入驻',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入取消原因',
        type: 'warning',
      }
    );
    await cancelCoachEntry({
      coachId: coach.value.coachId,
      reason: value.trim(),
    });
    ElMessage.success('已取消入驻');
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
  <div class="coach-detail">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>用户管理</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/coach-management' }"
          >教练管理</el-breadcrumb-item
        >
        <el-breadcrumb-item>教练详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else-if="error || !coach">
      <el-empty description="教练不存在或加载失败">
        <el-button type="primary" @click="goBack">返回列表</el-button>
      </el-empty>
    </template>

    <template v-else>
      <div class="info-card">
        <div class="info-main">
          <img
            class="avatar"
            :src="coach.avatarUrl || '/default-avatar.png'"
            alt="头像"
          />
          <div class="info-content">
            <div class="info-title">
              <span class="name">{{ coach.name || '未设置' }}</span>
              <span class="coach-id">ID: {{ coach.coachId }}</span>
              <span
                class="status-tag"
                :style="{
                  color: coachStatusMap[coach.status]?.color,
                  backgroundColor: coachStatusMap[coach.status]?.bgColor,
                }"
              >
                {{ coachStatusMap[coach.status]?.label || '-' }}
              </span>
              <span
                class="status-tag"
                :style="{
                  color: realtimeStatusMap[coach.realtimeStatus]?.color,
                  backgroundColor:
                    realtimeStatusMap[coach.realtimeStatus]?.bgColor,
                }"
              >
                {{
                  realtimeStatusMap[coach.realtimeStatus]?.label ||
                  coach.realtimeStatus ||
                  '-'
                }}
              </span>
            </div>
            <div class="info-meta">
              <span>手机号：{{ maskPhone(coach.phone) }}</span>
              <span>性别：{{ formatGender(coach.gender) }}</span>
              <span>年龄：{{ coach.age }}</span>
              <span>任教年限：{{ coach.teachingYears }} 年</span>
              <span>参考单价：{{ coach.referencePrice }} 元/节</span>
              <span>总学员数：{{ coach.totalStudents }}</span>
              <span>累计课时：{{ coach.totalHours }}</span>
            </div>
          </div>
        </div>
        <div class="info-actions">
          <el-button
            v-if="coach.status === 1"
            type="primary"
            @click="handleEdit"
            >编辑</el-button
          >
          <el-button v-if="coach.status === 1" @click="handleSchedule"
            >排班</el-button
          >
          <el-button
            v-if="coach.status === 1"
            type="danger"
            @click="handleCancelEntry"
            >取消入驻</el-button
          >
        </div>
      </div>

      <div class="timeline-card">
        <div class="card-title">状态变更记录</div>
        <el-timeline>
          <el-timeline-item
            v-for="(log, index) in coach.auditLogs || []"
            :key="index"
            :timestamp="formatDateTime(log.createdAt)"
          >
            {{ log.action }}
            <span v-if="log.reason"> - {{ log.reason }}</span>
          </el-timeline-item>
          <el-timeline-item
            v-if="!coach.auditLogs || coach.auditLogs.length === 0"
          >
            暂无状态变更记录
          </el-timeline-item>
        </el-timeline>
      </div>

      <div class="tabs-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <div class="detail-section">
              <div class="section-title">基础信息</div>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="label">姓名</span
                  ><span class="value">{{ coach.name }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">手机号</span
                  ><span class="value">{{ maskPhone(coach.phone) }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">性别</span
                  ><span class="value">{{ formatGender(coach.gender) }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">年龄</span
                  ><span class="value">{{ coach.age }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">邮箱</span
                  ><span class="value">{{ coach.email || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">参考单价</span
                  ><span class="value">{{ coach.referencePrice }} 元/节</span>
                </div>
                <div class="detail-item">
                  <span class="label">微信二维码</span>
                  <el-image
                    v-if="coach.wechatQrUrl"
                    class="qr-thumb"
                    :src="coach.wechatQrUrl"
                    :preview-src-list="[coach.wechatQrUrl]"
                    fit="cover"
                    preview-teleported
                  />
                  <span v-else class="value">-</span>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-title">实名与资质</div>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="label">身份证号</span
                  ><span class="value">{{ maskIdCard(coach.idCardNo) }}</span>
                </div>
              </div>
              <div class="cert-grid">
                <div class="cert-box">
                  <div class="cert-label">身份证正面照</div>
                  <el-image
                    v-if="findCertUrl('ID_CARD_FRONT')"
                    class="cert-image"
                    :src="findCertUrl('ID_CARD_FRONT')"
                    :preview-src-list="[findCertUrl('ID_CARD_FRONT')]"
                    fit="cover"
                    preview-teleported
                  />
                  <span v-else class="cert-empty">未上传</span>
                </div>
                <div class="cert-box">
                  <div class="cert-label">身份证反面照</div>
                  <el-image
                    v-if="findCertUrl('ID_CARD_BACK')"
                    class="cert-image"
                    :src="findCertUrl('ID_CARD_BACK')"
                    :preview-src-list="[findCertUrl('ID_CARD_BACK')]"
                    fit="cover"
                    preview-teleported
                  />
                  <span v-else class="cert-empty">未上传</span>
                </div>
                <div class="cert-box">
                  <div class="cert-label">健康证</div>
                  <el-image
                    v-if="findCertUrl('HEALTH_CERT')"
                    class="cert-image"
                    :src="findCertUrl('HEALTH_CERT')"
                    :preview-src-list="[findCertUrl('HEALTH_CERT')]"
                    fit="cover"
                    preview-teleported
                  />
                  <span v-else class="cert-empty">未上传</span>
                </div>
                <div class="cert-box">
                  <div class="cert-label">个人形象照</div>
                  <el-image
                    v-if="findCertUrl('PORTRAIT')"
                    class="cert-image"
                    :src="findCertUrl('PORTRAIT')"
                    :preview-src-list="[findCertUrl('PORTRAIT')]"
                    fit="cover"
                    preview-teleported
                  />
                  <span v-else class="cert-empty">未上传</span>
                </div>
                <div
                  v-for="cert in coach.certificates.filter(
                    (item) => item.certType === 'COACH_CERT'
                  )"
                  :key="cert.certificateId"
                  class="cert-box"
                >
                  <div class="cert-label">教练资格证</div>
                  <el-image
                    class="cert-image"
                    :src="cert.imageUrl"
                    :preview-src-list="[cert.imageUrl]"
                    fit="cover"
                    preview-teleported
                  />
                </div>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-title">教学履历</div>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="label">任教年限</span
                  ><span class="value">{{ coach.teachingYears }} 年</span>
                </div>
                <div class="detail-item">
                  <span class="label">总学员数</span
                  ><span class="value">{{ coach.totalStudents }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">总课时数</span
                  ><span class="value">{{ coach.totalHours }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">擅长泳姿</span
                  ><span class="value">{{
                    formatStrokes(coach.teachingStrokes)
                  }}</span>
                </div>
              </div>
              <div class="detail-item block">
                <span class="label">个人简介</span>
                <span class="value">{{ coach.bio || '-' }}</span>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-title">服务设置</div>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="label">参考单价</span
                  ><span class="value">{{ coach.referencePrice }} 元/节</span>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="学员列表" name="students">
            <el-empty description="学员列表功能即将上线" />
          </el-tab-pane>

          <el-tab-pane
            v-if="coach.status === 1 || coach.status === 4"
            label="排班"
            name="schedule"
          >
            <el-empty description="排班功能即将上线" />
          </el-tab-pane>

          <el-tab-pane
            v-if="
              coach.status === 1 || coach.status === 4 || coach.status === 3
            "
            label="上课记录"
            name="records"
          >
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
          v-if="coach.status === 1"
          type="danger"
          @click="handleCancelEntry"
          >取消入驻</el-button
        >
        <el-button v-if="coach.status === 1" type="primary" @click="handleEdit"
          >编辑资料</el-button
        >
      </div>
    </template>

    <CoachEditModal
      v-if="coach"
      v-model:visible="editVisible"
      :coach-id="coach.coachId"
      :is-edit="true"
      @success="handleEditSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
.coach-detail {
  padding-bottom: 80px;
}

.breadcrumb-wrap {
  display: flex;
  align-items: center;
  height: 48px;
  margin-bottom: 16px;
}

.info-card,
.timeline-card,
.tabs-card {
  padding: 24px;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.info-main {
  display: flex;
  gap: 16px;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.info-content {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
}

.info-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.name {
  font-size: 20px;
  font-weight: 500;
  color: #262626;
}

.coach-id {
  font-size: 12px;
  color: #8c8c8c;
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

.info-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.card-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.detail-section {
  margin-bottom: 24px;
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

  &.block {
    flex-direction: column;
    margin-top: 16px;
  }
}

.detail-item .label {
  color: #8c8c8c;
}

.detail-item .value {
  color: #262626;
}

.cert-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.cert-box {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .cert-image {
    width: 100px;
    height: 100px;
    border-radius: 4px;
  }
}

.qr-thumb {
  width: 80px;
  height: 80px;
  border-radius: 4px;
}

.cert-label {
  font-size: 12px;
  color: #8c8c8c;
}

.cert-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  font-size: 12px;
  color: #8c8c8c;
  background: #f5f7fa;
  border-radius: 4px;
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
