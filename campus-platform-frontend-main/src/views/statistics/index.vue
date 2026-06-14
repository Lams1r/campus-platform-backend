<template>
  <div class="page-container">
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="24" :sm="12" :lg="6" v-for="(card, index) in statCards" :key="index">
        <div class="stat-card" :style="{ background: card.color }">
          <el-icon class="card-icon"><component :is="card.icon" /></el-icon>
          <div class="card-info">
            <div class="card-title">{{ card.title }}</div>
            <div class="card-value">{{ card.value }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header"><span>学生院系分布</span></div>
          </template>
          <div ref="deptChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header"><span>图书借阅趋势</span></div>
          </template>
          <div ref="bookChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header"><span>考勤统计</span></div>
          </template>
          <div ref="attendanceChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header"><span>报修处理情况</span></div>
          </template>
          <div ref="repairChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getDashboardOverview } from '@/api/dashboard'

const deptChart = ref(null)
const bookChart = ref(null)
const attendanceChart = ref(null)
const repairChart = ref(null)
let charts = []

const statCards = ref([
  { title: '在校学生', value: '3,256', icon: 'User', color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { title: '在职教师', value: '186', icon: 'UserFilled', color: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)' },
  { title: '图书总量', value: '52,380', icon: 'Collection', color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { title: '今日考勤率', value: '96.8%', icon: 'Checked', color: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)' }
])

const initCharts = () => {
  // 院系分布饼图
  const dept = echarts.init(deptChart.value)
  dept.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: [
        { value: 580, name: '计算机学院' },
        { value: 420, name: '电子工程学院' },
        { value: 380, name: '商学院' },
        { value: 320, name: '文学院' },
        { value: 280, name: '理学院' }
      ]
    }]
  })
  charts.push(dept)

  // 图书借阅趋势折线图
  const book = echarts.init(bookChart.value)
  book.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value' },
    series: [{ data: [120, 132, 101, 134, 90, 230, 210], type: 'line', smooth: true, areaStyle: {} }]
  })
  charts.push(book)

  // 考勤统计柱状图
  const attendance = echarts.init(attendanceChart.value)
  attendance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['正常', '迟到', '早退', '缺勤', '请假'] },
    yAxis: { type: 'value' },
    series: [{ data: [3200, 45, 12, 8, 156], type: 'bar', itemStyle: { color: '#409EFF' } }]
  })
  charts.push(attendance)

  // 报修处理情况
  const repair = echarts.init(repairChart.value)
  repair.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value' },
    series: [
      { name: '待处理', type: 'bar', stack: 'total', data: [32, 30, 30, 33, 39, 33], itemStyle: { color: '#F56C6C' } },
      { name: '处理中', type: 'bar', stack: 'total', data: [120, 132, 101, 134, 90, 230], itemStyle: { color: '#E6A23C' } },
      { name: '已完成', type: 'bar', stack: 'total', data: [220, 182, 191, 234, 290, 330], itemStyle: { color: '#67C23A' } }
    ]
  })
  charts.push(repair)
}

const fetchData = async () => {
  try {
    const res = await getDashboardOverview()
    if (res) {
      statCards.value[0].value = res.studentCount?.toLocaleString() || '3,256'
      statCards.value[1].value = res.teacherCount?.toString() || '186'
    }
  } catch (error) { console.error(error) }
}

const handleResize = () => charts.forEach(chart => chart?.resize())

onMounted(() => { fetchData(); initCharts(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); charts.forEach(chart => chart?.dispose()) })
</script>

<style scoped>
.stat-cards { margin-bottom: 16px; }
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  color: #fff;
}
.card-icon { font-size: 48px; margin-right: 16px; }
.card-info { flex: 1; }
.card-title { font-size: 14px; opacity: 0.9; margin-bottom: 4px; }
.card-value { font-size: 28px; font-weight: bold; }
.chart-row { margin-bottom: 16px; }
.card-header { font-weight: 500; }
.chart-container { height: 300px; }
</style>
