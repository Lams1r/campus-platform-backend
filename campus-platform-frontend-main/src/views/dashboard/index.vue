<template>
  <div class="dashboard-container">
    <!-- Hero 区域 — Dark Tile -->
    <section class="hero-tile">
      <div class="hero-orb"></div>
      <div class="hero-content">
        <div class="hero-left">
          <el-avatar :size="56" :src="authStore.avatar" class="hero-avatar">
            <el-icon :size="28"><User /></el-icon>
          </el-avatar>
          <div class="hero-text">
            <h2 class="hero-title">
              {{ greeting }}，{{ authStore.nickname || authStore.username }}
            </h2>
            <p class="hero-desc">今天是 {{ currentDate }}，{{ roleText }}工作台已为你准备好</p>
          </div>
        </div>
        <div class="hero-meta">
          <span>{{ roleText }}</span>
          <strong>Campus OS</strong>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="$router.push('/education/attendance')">
            <el-icon><Checked /></el-icon>考勤签到
          </el-button>
          <el-button @click="$router.push('/campus/repair')">
            <el-icon><Tools /></el-icon>提交报修
          </el-button>
          <el-button @click="$router.push('/campus/notice')">
            <el-icon><Bell /></el-icon>查看公告
          </el-button>
        </div>
      </div>
    </section>

    <!-- 统计卡片 — Light Tile -->
    <section class="stats-tile" v-loading="loading">
      <el-row :gutter="20">
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon"><el-icon><UserFilled /></el-icon></div>
            <div class="stat-value">{{ stats.studentCount }}</div>
            <div class="stat-label">在校学生</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon"><el-icon><User /></el-icon></div>
            <div class="stat-value">{{ stats.teacherCount }}</div>
            <div class="stat-label">在职教师</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon"><el-icon><Reading /></el-icon></div>
            <div class="stat-value">{{ stats.courseCount }}</div>
            <div class="stat-label">开设课程</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon"><el-icon><Checked /></el-icon></div>
            <div class="stat-value">{{ stats.attendanceRate }}%</div>
            <div class="stat-label">今日出勤率</div>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- 图表 + 待办 — Parchment Tile -->
    <section class="content-tile">
      <el-row :gutter="20">
        <el-col :xs="24" :lg="16">
          <div class="tile-card">
            <div class="tile-card-header">
              <span class="tile-card-title">考勤趋势</span>
              <el-radio-group v-model="attendancePeriod" size="small">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
              </el-radio-group>
            </div>
            <div ref="attendanceChart" class="chart-container"></div>
          </div>
        </el-col>
        <el-col :xs="24" :lg="8">
          <div class="tile-card">
            <div class="tile-card-header">
              <span class="tile-card-title">待办事项</span>
            </div>
            <div class="todo-list">
              <div v-for="(item, index) in todoList" :key="index" class="todo-item" @click="$router.push(item.path)">
                <span class="todo-dot" :class="item.type"></span>
                <span class="todo-text">{{ item.content }}</span>
                <span class="todo-time">{{ item.time }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- 公告 + 快捷入口 — Light Tile -->
    <section class="bottom-tile">
      <el-row :gutter="20">
        <el-col :xs="24" :lg="12">
          <div class="tile-card">
            <div class="tile-card-header">
              <span class="tile-card-title">最新公告</span>
              <a class="tile-link" @click="$router.push('/campus/notice')">更多</a>
            </div>
            <div class="notice-list">
              <div v-for="(notice, index) in latestNotices" :key="index" class="notice-item" @click="$router.push('/campus/notice')">
                <span class="notice-dot" :class="{ unread: !notice.read }"></span>
                <div class="notice-content">
                  <div class="notice-title">{{ notice.title }}</div>
                  <div class="notice-meta">
                    <span>{{ notice.publisher }}</span>
                    <span>{{ notice.time }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :lg="12">
          <div class="tile-card">
            <div class="tile-card-header">
              <span class="tile-card-title">快捷入口</span>
            </div>
            <div class="quick-links">
              <div
                v-for="(link, index) in quickLinks"
                :key="index"
                class="quick-link-item"
                @click="$router.push(link.path)"
              >
                <el-icon :size="20" :color="link.color">
                  <component :is="link.icon" />
                </el-icon>
                <span class="link-text">{{ link.name }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import { animate, stagger } from 'animejs'
import { useAuthStore } from '@/stores/auth'
import { getDashboardOverview } from '@/api/dashboard'
import { User, UserFilled, Reading, Checked, Bell, Tools, Calendar, Trophy, Timer, Star } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const attendanceChart = ref(null)
const attendancePeriod = ref('week')
const loading = ref(false)
let chartInstance = null

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const currentDate = computed(() => {
  return new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})

const roleText = computed(() => {
  if (authStore.isAdmin) return '管理员'
  if (authStore.isTeacher) return '教师'
  return '学生'
})

const stats = ref({
  studentCount: 0,
  teacherCount: 0,
  courseCount: 0,
  attendanceRate: 0
})

const todoList = ref([
  { type: 'red', content: '期末成绩审核', time: '今天', path: '/education/score' },
  { type: 'orange', content: '宿舍报修处理', time: '2小时前', path: '/campus/repair' },
  { type: 'gray', content: '教学会议', time: '明天 14:00', path: '/campus/notice' },
  { type: 'green', content: '课程资料上传', time: '已完成', path: '/education/course' }
])

const latestNotices = ref([
  { title: '关于2024年寒假放假安排的通知', publisher: '教务处', time: '2小时前', read: false },
  { title: '期末考试时间安排', publisher: '教务处', time: '昨天', read: false },
  { title: '图书馆开放时间调整通知', publisher: '图书馆', time: '3天前', read: true },
  { title: '校园卡系统维护公告', publisher: '信息中心', time: '1周前', read: true }
])

const quickLinks = ref([
  { name: '我的课表', icon: 'Calendar', path: '/education/timetable', color: '#1d1d1f' },
  { name: '成绩查询', icon: 'Trophy', path: '/education/score', color: '#1d1d1f' },
  { name: '请假申请', icon: 'Timer', path: '/education/leave', color: '#1d1d1f' },
  { name: '课程评价', icon: 'Star', path: '/education/evaluation', color: '#1d1d1f' }
])

const chartData = {
  week: {
    xAxis: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    attendance: [95, 96, 94, 97, 95, 90, 88],
    leave: [3, 2, 4, 2, 3, 5, 6],
    absent: [2, 2, 2, 1, 2, 5, 6]
  },
  month: {
    xAxis: ['第1周', '第2周', '第3周', '第4周', '第5周'],
    attendance: [94, 96, 95, 97, 96],
    leave: [4, 2, 3, 2, 2],
    absent: [2, 2, 2, 1, 2]
  }
}

const initChart = () => {
  if (!attendanceChart.value) return

  chartInstance = echarts.init(attendanceChart.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return
  const data = chartData[attendancePeriod.value]
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.88)',
      borderWidth: 0,
      textStyle: { color: '#ffffff' }
    },
    legend: {
      data: ['出勤率', '请假率', '缺勤率'],
      bottom: 0,
      textStyle: { fontSize: 12, color: '#7a7a7a' }
    },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.xAxis,
      axisLine: { lineStyle: { color: '#e0e0e0' } },
      axisLabel: { color: '#7a7a7a', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: '{value}%', color: '#7a7a7a', fontSize: 12 },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series: [
      {
        name: '出勤率',
        type: 'line',
        smooth: true,
        data: data.attendance,
        itemStyle: { color: '#2563eb' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(37, 99, 235, 0.22)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.02)' }
          ])
        }
      },
      {
        name: '请假率',
        type: 'line',
        smooth: true,
        data: data.leave,
        itemStyle: { color: '#94a3b8' }
      },
      {
        name: '缺勤率',
        type: 'line',
        smooth: true,
        data: data.absent,
        itemStyle: { color: '#ff3b30' },
        lineStyle: { opacity: 0.6 }
      }
    ]
  }
  chartInstance.setOption(option)
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDashboardOverview()
    stats.value = res
  } catch (error) {
    stats.value = {
      studentCount: 3256,
      teacherCount: 186,
      courseCount: 428,
      attendanceRate: 96.5
    }
  }
  loading.value = false
  animateStats()
}

const animateStats = () => {
  // 数字滚动动画
  const animTarget = { student: 0, teacher: 0, course: 0, rate: 0 }
  animate(animTarget, {
    student: stats.value.studentCount || 0,
    teacher: stats.value.teacherCount || 0,
    course: stats.value.courseCount || 0,
    rate: stats.value.attendanceRate || 0,
    duration: 1800,
    easing: 'easeOutExpo',
    onUpdate: () => {
      stats.value = {
        studentCount: Math.round(animTarget.student),
        teacherCount: Math.round(animTarget.teacher),
        courseCount: Math.round(animTarget.course),
        attendanceRate: Math.round(animTarget.rate * 10) / 10
      }
    }
  })
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(attendancePeriod, updateChart)

onMounted(() => {
  fetchData()
  initChart()
  window.addEventListener('resize', handleResize)

  // 卡片交错入场动画
  animate('.stat-card', {
    translateY: [24, 0],
    opacity: [0, 1],
    delay: stagger(120),
    duration: 600,
    easing: 'easeOutCubic'
  })

  animate('.tile-card', {
    translateY: [32, 0],
    opacity: [0, 1],
    delay: stagger(200, { start: 400 }),
    duration: 700,
    easing: 'easeOutCubic'
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 0;
  margin: -22px;
  border-radius: 0 0 var(--apple-radius-xl, 28px) var(--apple-radius-xl, 28px);
  overflow: hidden;
}

/* ============================================================
   Hero — Dark Tile (Apple product-tile-dark)
   ============================================================ */
.hero-tile {
  background:
    radial-gradient(circle at 76% 18%, rgba(96, 165, 250, 0.28), transparent 28%),
    linear-gradient(135deg, var(--apple-tile-1, #111827), var(--apple-tile-3, #0f172a));
  padding: 48px;
  color: var(--apple-on-dark, #ffffff);
  position: relative;
  overflow: hidden;
}

.hero-orb {
  position: absolute;
  right: 8%;
  top: -80px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.2);
  filter: blur(10px);
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hero-avatar {
  background: rgba(255, 255, 255, 0.14);
  color: var(--apple-body-muted, #cccccc);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.hero-title {
  font-family: var(--apple-font-display, "SF Pro Display", system-ui, -apple-system, sans-serif);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.28px;
  line-height: 1.14;
  margin: 0 0 4px;
  color: var(--apple-on-dark, #ffffff);
}

.hero-desc {
  font-size: 17px;
  color: var(--apple-body-muted, #cccccc);
  margin: 0;
  letter-spacing: -0.374px;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.hero-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 128px;
  padding: 12px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.hero-meta span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
}

.hero-meta strong {
  font-size: 17px;
  color: #ffffff;
}

.hero-actions .el-button {
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.16) !important;
  color: var(--apple-on-dark, #ffffff) !important;
}

.hero-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.18) !important;
}

.hero-actions .el-button--primary {
  background: rgba(255, 255, 255, 0.9) !important;
  border-color: rgba(255, 255, 255, 0.9) !important;
  color: var(--apple-ink, #1d1d1f) !important;
}

/* ============================================================
   Stats — Light Tile
   ============================================================ */
.stats-tile {
  background: var(--apple-surface-strong, #ffffff);
  padding: 32px 48px;
}

.stat-card {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), transparent),
    var(--apple-canvas, #ffffff);
  border: 1px solid var(--apple-hairline, #e0e0e0);
  border-radius: var(--apple-radius-lg, 18px);
  padding: 24px;
  text-align: center;
  box-shadow: var(--apple-shadow-card, none);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--apple-shadow-soft, none);
}

.stat-icon {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--apple-primary, #2563eb);
  background: rgba(37, 99, 235, 0.1);
}

.stat-value {
  font-family: var(--apple-font-display, "SF Pro Display", system-ui, -apple-system, sans-serif);
  font-size: 34px;
  font-weight: 600;
  letter-spacing: -0.374px;
  line-height: 1.1;
  color: var(--apple-ink, #1d1d1f);
}

.stat-label {
  font-size: 14px;
  color: var(--apple-ink-muted-48, #7a7a7a);
  margin-top: 6px;
  letter-spacing: -0.224px;
}

/* ============================================================
   Content — Parchment Tile
   ============================================================ */
.content-tile {
  background: var(--apple-parchment, #f5f5f7);
  padding: 32px 48px;
}

.tile-card {
  background: var(--apple-canvas, #ffffff);
  border: 1px solid var(--apple-hairline, #e0e0e0);
  border-radius: var(--apple-radius-lg, 18px);
  padding: 24px;
  box-shadow: var(--apple-shadow-card, none);
  min-height: 100%;
}

.tile-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.tile-card-title {
  font-family: var(--apple-font-display, "SF Pro Display", system-ui, -apple-system, sans-serif);
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 0.231px;
  color: var(--apple-ink, #1d1d1f);
}

.tile-link {
  color: var(--apple-link, #0066cc);
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
  letter-spacing: -0.224px;
}

.tile-link:hover {
  text-decoration: underline;
}

.chart-container {
  height: 280px;
}

/* ============================================================
   Bottom — Light Tile
   ============================================================ */
.bottom-tile {
  background: var(--apple-canvas, #ffffff);
  padding: 32px 48px;
}

/* ============================================================
   Todo List
   ============================================================ */
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: var(--apple-radius-sm, 8px);
  background: var(--apple-parchment, #f5f5f7);
  cursor: pointer;
  transition: background 0.18s ease, transform 0.18s ease;
}

.todo-item:hover {
  background: var(--apple-surface-strong, #ffffff);
  transform: translateX(2px);
}

.todo-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.todo-dot.red { background: #ff3b30; }
.todo-dot.orange { background: #ff9500; }
.todo-dot.green { background: #34c759; }
.todo-dot.gray { background: var(--apple-chip, #d2d2d7); }

.todo-text {
  flex: 1;
  font-size: 14px;
  color: var(--apple-ink, #1d1d1f);
  letter-spacing: -0.224px;
}

.todo-time {
  font-size: 12px;
  color: var(--apple-ink-muted-48, #7a7a7a);
  letter-spacing: -0.12px;
}

/* ============================================================
   Notice List
   ============================================================ */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: var(--apple-radius-sm, 8px);
  cursor: pointer;
  transition: background 0.2s;
}

.notice-item:hover {
  background: var(--apple-parchment, #f5f5f7);
  transform: translateX(2px);
}

.notice-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--apple-chip, #d2d2d7);
  margin-top: 7px;
  flex-shrink: 0;
}

.notice-dot.unread {
  background: var(--apple-ink, #1d1d1f);
}

.notice-content {
  flex: 1;
}

.notice-title {
  font-size: 14px;
  color: var(--apple-ink, #1d1d1f);
  letter-spacing: -0.224px;
  line-height: 1.43;
}

.notice-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--apple-ink-muted-48, #7a7a7a);
  margin-top: 4px;
  letter-spacing: -0.12px;
}

/* ============================================================
   Quick Links
   ============================================================ */
.quick-links {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick-link-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), transparent),
    var(--apple-parchment, #f5f5f7);
  border: 1px solid transparent;
  border-radius: var(--apple-radius-lg, 18px);
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-link-item:hover {
  background: var(--apple-surface-strong, #ffffff);
  border-color: var(--apple-hairline, #e0e0e0);
  transform: translateY(-1px);
}

.link-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-ink, #1d1d1f);
  letter-spacing: -0.224px;
}

/* ============================================================
   Responsive
   ============================================================ */
@media (max-width: 833px) {
  .dashboard-container {
    margin: -14px;
    border-radius: 0;
  }

  .hero-tile,
  .stats-tile,
  .content-tile,
  .bottom-tile {
    padding: 24px;
  }

  .hero-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-left {
    align-items: flex-start;
  }

  .hero-meta {
    width: 100%;
  }

  .hero-title {
    font-size: 24px;
  }

  .stat-value {
    font-size: 28px;
  }
}

@media (max-width: 640px) {
  .dashboard-container {
    margin: -10px;
  }

  .hero-tile,
  .stats-tile,
  .content-tile,
  .bottom-tile {
    padding: 18px 14px;
  }

  .hero-actions {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr;
  }

  .hero-actions .el-button {
    width: 100%;
    margin-left: 0 !important;
  }

  .hero-avatar {
    width: 46px !important;
    height: 46px !important;
  }

  .tile-card {
    padding: 18px;
    border-radius: 20px;
  }

  .tile-card-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .quick-links {
    grid-template-columns: 1fr;
  }

  .chart-container {
    height: 240px;
  }
}
</style>
