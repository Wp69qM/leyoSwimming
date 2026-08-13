<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAdminAuthStore } from '@/stores/adminAuth';
import { ElMessage } from 'element-plus';

const router = useRouter();
const authStore = useAdminAuthStore();

interface MetricItem {
  name: string;
  value: number | string;
  ratio: number;
  icon: string;
}

interface QuickEntry {
  name: string;
  path: string;
  icon: string;
  badge?: number;
}

interface TopCoach {
  rank: number;
  name: string;
  rating: number;
  hours: number;
}

interface TopTimeSlot {
  rank: number;
  slot: string;
  bookings: number;
}

const loading = ref(false);
const error = ref(false);

const pendingTodoCount = ref(3);

const metrics = ref<MetricItem[]>([
  { name: '注册用户数', value: 1286, ratio: 12.5, icon: 'User' },
  { name: '活跃学员数', value: 86, ratio: 5.2, icon: 'Star' },
  { name: '教练数', value: 24, ratio: 0, icon: 'Team' },
  { name: '今日预约数', value: 18, ratio: -3.1, icon: 'Calendar' },
  { name: '今日订单金额', value: '¥3,280', ratio: 8.7, icon: 'Money' },
]);

const quickEntries = ref<QuickEntry[]>([
  { name: '用户管理', path: '/users', icon: 'UserFilled' },
  { name: '教练审核', path: '/coach-audit/queue', icon: 'Checked', badge: 5 },
  { name: '订单管理', path: '/orders', icon: 'Document' },
  { name: '退款审批', path: '/orders', icon: 'Refund', badge: 2 },
  { name: '客服工单', path: '/', icon: 'Service', badge: 1 },
]);

const topCoaches = ref<TopCoach[]>([
  { rank: 1, name: '张教练', rating: 4.9, hours: 128 },
  { rank: 2, name: '李教练', rating: 4.8, hours: 96 },
  { rank: 3, name: '王教练', rating: 4.7, hours: 82 },
  { rank: 4, name: '赵教练', rating: 4.6, hours: 74 },
  { rank: 5, name: '陈教练', rating: 4.5, hours: 65 },
]);

const topTimeSlots = ref<TopTimeSlot[]>([
  { rank: 1, slot: '09:00-10:00', bookings: 32 },
  { rank: 2, slot: '10:00-11:00', bookings: 28 },
  { rank: 3, slot: '19:00-20:00', bookings: 26 },
  { rank: 4, slot: '15:00-16:00', bookings: 21 },
  { rank: 5, slot: '20:00-21:00', bookings: 18 },
]);

const trendTimeRange = ref('week');
const trendOptions = [
  { label: '今日', value: 'today' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '本年', value: 'year' },
];

const trendData = computed(() => {
  const map: Record<string, number[]> = {
    today: [2, 4, 3, 6, 5, 8, 4, 7, 6, 5, 4, 6],
    week: [12, 18, 15, 22, 19, 25, 21],
    month: [45, 52, 48, 60, 55, 58, 62, 59, 65, 70],
    year: [320, 350, 380, 410, 390, 430, 460, 480, 510, 530, 560, 590],
  };
  return map[trendTimeRange.value] || map.week;
});

const trendLabels = computed(() => {
  const map: Record<string, string[]> = {
    today: [
      '08',
      '09',
      '10',
      '11',
      '12',
      '13',
      '14',
      '15',
      '16',
      '17',
      '18',
      '19',
    ],
    week: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    month: ['1日', '5日', '10日', '15日', '20日', '25日', '30日'],
    year: [
      '1月',
      '2月',
      '3月',
      '4月',
      '5月',
      '6月',
      '7月',
      '8月',
      '9月',
      '10月',
      '11月',
      '12月',
    ],
  };
  return map[trendTimeRange.value] || map.week;
});

const maxTrendValue = computed(() => {
  return Math.max(...trendData.value, 1);
});

function navigateTo(path: string) {
  router.push(path);
}

async function fetchDashboardData() {
  loading.value = true;
  error.value = false;
  try {
    // TODO: replace with real dashboard API when available
    await new Promise((resolve) => setTimeout(resolve, 300));
  } catch (err) {
    error.value = true;
    ElMessage.error(err instanceof Error ? err.message : '加载失败');
  } finally {
    loading.value = false;
  }
}

function getIconComponent(name: string): string {
  const map: Record<string, string> = {
    User: 'User',
    Star: 'Star',
    Team: 'UserFilled',
    Calendar: 'Calendar',
    Money: 'Money',
  };
  return map[name] || 'User';
}

onMounted(() => {
  fetchDashboardData();
});
</script>

<template>
  <div class="home-page">
    <el-alert
      v-if="error"
      title="数据加载失败，请重试"
      type="error"
      show-icon
      :closable="false"
      class="error-alert"
    >
      <template #default>
        <el-button type="primary" size="small" @click="fetchDashboardData"
          >刷新</el-button
        >
      </template>
    </el-alert>

    <div class="welcome-card">
      <h2 class="welcome-title">
        欢迎回来，{{ authStore.admin?.name || '管理员' }}
      </h2>
      <p class="welcome-subtitle">今日待处理事项：{{ pendingTodoCount }} 项</p>
    </div>

    <div class="metrics-row">
      <div v-for="metric in metrics" :key="metric.name" class="metric-card">
        <div class="metric-icon-wrap">
          <el-icon class="metric-icon" :size="24">
            <component :is="getIconComponent(metric.icon)" />
          </el-icon>
        </div>
        <div class="metric-info">
          <div class="metric-name">{{ metric.name }}</div>
          <div class="metric-value">{{ metric.value }}</div>
          <div class="metric-ratio">
            <span :class="metric.ratio >= 0 ? 'up' : 'down'">
              {{ metric.ratio >= 0 ? '↑' : '↓' }} {{ Math.abs(metric.ratio) }}%
            </span>
            <span class="ratio-label">环比</span>
          </div>
        </div>
      </div>
    </div>

    <div class="quick-entry-card">
      <div class="card-title">快捷入口</div>
      <div class="quick-entry-list">
        <div
          v-for="entry in quickEntries"
          :key="entry.name"
          class="quick-entry-item"
          @click="navigateTo(entry.path)"
        >
          <div class="quick-entry-icon">
            <el-icon :size="24">
              <component :is="entry.icon" />
            </el-icon>
            <span
              v-if="entry.badge && entry.badge > 0"
              class="quick-entry-badge"
            />
          </div>
          <span class="quick-entry-name">{{ entry.name }}</span>
        </div>
      </div>
    </div>

    <div class="trend-card">
      <div class="trend-header">
        <div class="card-title">预约趋势 / 订单趋势</div>
        <el-radio-group v-model="trendTimeRange" size="small">
          <el-radio-button
            v-for="option in trendOptions"
            :key="option.value"
            :label="option.value"
          >
            {{ option.label }}
          </el-radio-button>
        </el-radio-group>
      </div>
      <el-skeleton v-if="loading" :rows="6" animated />
      <div v-else class="trend-chart">
        <div class="trend-bars">
          <div
            v-for="(value, index) in trendData"
            :key="index"
            class="trend-bar-wrap"
          >
            <div
              class="trend-bar"
              :style="{ height: `${(value / maxTrendValue) * 100}%` }"
            />
            <div class="trend-label">{{ trendLabels[index] }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="top-rank-row">
      <div class="top-rank-card">
        <div class="card-title">Top 教练</div>
        <el-table
          :data="topCoaches"
          header-row-class-name="table-header"
          row-class-name="table-row"
          style="width: 100%"
        >
          <el-table-column label="排名" prop="rank" width="80" align="center" />
          <el-table-column label="教练名" prop="name" />
          <el-table-column
            label="评分"
            prop="rating"
            width="100"
            align="center"
          />
          <el-table-column
            label="课时数"
            prop="hours"
            width="100"
            align="center"
          />
        </el-table>
      </div>
      <div class="top-rank-card">
        <div class="card-title">Top 课程时段</div>
        <el-table
          :data="topTimeSlots"
          header-row-class-name="table-header"
          row-class-name="table-row"
          style="width: 100%"
        >
          <el-table-column label="排名" prop="rank" width="80" align="center" />
          <el-table-column label="时段" prop="slot" />
          <el-table-column
            label="预约数"
            prop="bookings"
            width="120"
            align="center"
          />
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.home-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 24px;
}

.error-alert {
  margin-bottom: 0;
}

.welcome-card {
  padding: 20px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.welcome-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 500;
  color: #262626;
}

.welcome-subtitle {
  margin: 0;
  font-size: 14px;
  color: #595959;
}

.metrics-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.metric-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.metric-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: #e6f7ff;
  border-radius: 8px;
}

.metric-icon {
  color: #1890ff;
}

.metric-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.metric-name {
  font-size: 14px;
  color: #595959;
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #262626;
  line-height: 1;
}

.metric-ratio {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;

  .up {
    color: #52c41a;
  }

  .down {
    color: #ff4d4f;
  }

  .ratio-label {
    color: #8c8c8c;
  }
}

.quick-entry-card {
  padding: 20px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.card-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.quick-entry-list {
  display: flex;
  gap: 24px;
}

.quick-entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;

  &:hover {
    .quick-entry-icon {
      background: #e6f7ff;
      color: #1890ff;
    }
  }
}

.quick-entry-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  color: #595959;
  background: #f5f7fa;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.quick-entry-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  background: #ff4d4f;
  border-radius: 50%;
}

.quick-entry-name {
  font-size: 14px;
  color: #262626;
}

.trend-card {
  padding: 20px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.trend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  .card-title {
    margin-bottom: 0;
  }
}

.trend-chart {
  height: 320px;
}

.trend-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 280px;
  padding: 20px 0 0;
}

.trend-bar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  flex: 1;
  gap: 12px;
  height: 100%;
}

.trend-bar {
  width: 60%;
  min-height: 4px;
  background: linear-gradient(180deg, #1890ff 0%, #69c0ff 100%);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
}

.trend-label {
  font-size: 12px;
  color: #8c8c8c;
}

.top-rank-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.top-rank-card {
  padding: 20px;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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
    height: 48px;
    border-bottom: 1px solid #f0f2f5;
  }
}
</style>
